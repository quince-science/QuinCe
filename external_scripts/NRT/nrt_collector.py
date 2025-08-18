import json
import logging
import ntpath
import re
import traceback
from io import BytesIO
from zipfile import ZipFile
from datetime import datetime, timedelta
from bisect import bisect
from slack_sdk import WebClient
import toml
import requests

from modules.Preprocessor import PreprocessorFactory
from modules.Retriever import RetrieverFactory

# Local modules
import nrtdb
import nrtftp
from modules.Preprocessor.PreprocessorError import PreprocessorError

IGNORE_REGEXPS = [".*err.txt"]
PREPROCESSOR_FAILED = -10


def post_msg(config, message):
    message_destination = config['messages']['destination']
    if message_destination == 'slack':
        post_slack_msg(config['slack'], message)
    elif message_destination == 'telegram':
        post_telegram_msg(config['telegram'], message)
    else:
        print('UNRECOGNISED MESSAGE DESTINATION')
        print(message)


def post_slack_msg(config, message):
    client = WebClient(token=config['api_token'])
    client.chat_postMessage(channel='#' + config['workspace'], text=f'{message}')


def post_telegram_msg(config, message):
    url = f"https://api.telegram.org/bot{config['token']}/sendMessage?chat_id={config['chat_id']}&text=EXCEPTION MONITOR: {message}"
    requests.get(url)


# See if a file should be ignored based on its filename
def ignore_file(filename):
    result = False

    for expr in IGNORE_REGEXPS:
        if re.match(expr, filename):
            result = True
            break

    return result


def time_for_check(instrument):
    result = instrument["last_check"] is None

    if not result:
        last_check = datetime.fromtimestamp(instrument["last_check"])

        check_hour_index = bisect(instrument["check_hours"], last_check.hour)
        if check_hour_index == 0:
            check_hour = instrument["check_hours"][-1]
        elif check_hour_index == len(instrument["check_hours"]):
            check_hour = instrument["check_hours"][0]
        else:
            check_hour = instrument["check_hours"][check_hour_index]

        next_check = last_check.replace(hour=check_hour, minute=0, second=0)
        if next_check < last_check:
            next_check += timedelta(days=1)

        if datetime.now() >= next_check:
            result = True

    return result


# Upload a file to the FTP server. If it's a ZIP file,
# extract it and upload the contents as individual files
def upload_file(logger, ftp_conn, ftp_config, instrument_id, preprocessor, filename, contents):
    upload_result = nrtftp.UPLOAD_OK

    if not str.endswith(filename, ".zip"):
        if not ignore_file(filename):
            try:
                upload_result = nrtftp.upload_file(ftp_conn, ftp_config,
                                                   instrument_id, filename, preprocessor.preprocess(BytesIO(contents)))
            except PreprocessorError:
                logger.log(logging.ERROR, "Failed to preprocess file " + filename + ":" + traceback.format_exc())
                upload_result = PREPROCESSOR_FAILED
    else:
        with ZipFile(BytesIO(contents), 'r') as unzip:
            for name in unzip.namelist():
                if not ignore_file(name):
                    log_instrument(logger, instrument_id, logging.INFO, "Uploading "
                                   + "ZIP entry " + name)

                    preprocessed_file = None
                    try:
                        preprocessed_file = preprocessor.preprocess(BytesIO(unzip.read(name)))
                    except PreprocessorError:
                        log_instrument(logger, instrument_id, logging.ERROR, "Preprocessing "
                                       + "failed: " + traceback.format_exc())
                        upload_result = PREPROCESSOR_FAILED

                    if preprocessed_file is not None:
                        upload_result = nrtftp.upload_file(ftp_conn, ftp_config,
                                                           instrument_id, ntpath.basename(name), preprocessed_file)

    return upload_result


# Log a message for a specific instrument
def log_instrument(logger, instrument_id, level, message):
    logger.log(level, str(instrument_id) + ":" + message)


#######################################################

def main():
    # Read in the config
    with open("config.toml", "r") as config_file:
        config = toml.loads(config_file.read())

    # Set up logging
    logging.basicConfig(filename="nrt_collector.log",
                        format="%(asctime)s:%(levelname)s:%(message)s")
    logger = logging.getLogger('nrt_collector')
    logger.setLevel(level=config["Logging"]["level"])

    # Connect to NRT database and get instrument list
    db_conn = nrtdb.get_db_conn(config["Database"]["location"])
    instruments = nrtdb.get_instrument_ids(db_conn)

    # Connect to FTP server
    ftp_conn = nrtftp.connect_ftp(config["FTP"])

    # Loop through each instrument
    for instrument_id in instruments:
        try:
            log_instrument(logger, instrument_id, logging.DEBUG,
                           "Checking instrument")
            instrument = nrtdb.get_instrument(db_conn, instrument_id)

            if instrument["type"] is None:
                log_instrument(logger, instrument_id, logging.ERROR,
                               "Configuration type not set")
            elif instrument["paused"]:
                log_instrument(logger, instrument_id, logging.DEBUG,
                               "Instrument is paused")
            elif not time_for_check(instrument):
                log_instrument(logger, instrument_id, logging.DEBUG,
                               "Not time for check yet")
            else:
                log_instrument(logger, instrument_id, logging.DEBUG,
                               "Time for check")

                # Build the retriever
                if instrument["config"] is not None:
                    retriever = RetrieverFactory.get_instance(instrument["type"],
                                                              instrument_id, logger, json.loads(instrument["config"]))

                    # Make sure configuration is still valid
                    if not retriever.test_configuration():
                        log_instrument(logger, instrument_id, logging.ERROR,
                                       "Configuration invalid")
                        post_msg(config, f"Error checking configuration for instrument {instrument_id} ({instrument['name']})")
                    # Initialise the retriever
                    elif not retriever.startup():
                        log_instrument(logger, instrument_id, logging.ERROR,
                                       "Could not initialise retriever")
                        post_msg(config, f"Error initialising retriever for instrument {instrument_id} ({instrument['name']})")
                    else:
                        preprocessor = None if instrument["preprocessor"] is None else \
                            PreprocessorFactory.get_instance(instrument["preprocessor"],
                                                             logger, json.loads(instrument["preprocessor_config"]))

                        # Loop through all files returned by the retriever one by one
                        while retriever.load_next_files():
                            for file in retriever.current_files:

                                log_instrument(logger, instrument_id, logging.INFO,
                                               "Uploading " + file["filename"] + " to FTP server")

                                try:
                                    upload_result = upload_file(logger, ftp_conn, config["FTP"],
                                                                instrument_id, preprocessor,
                                                                preprocessor.get_processed_filename(file["filename"]),
                                                                file["contents"])

                                    if upload_result == nrtftp.NOT_INITIALISED:
                                        log_instrument(logger, instrument_id, logging.ERROR,
                                                       "FTP not initialised")
                                        retriever.file_failed()
                                    elif upload_result == nrtftp.FILE_EXISTS:
                                        log_instrument(logger, instrument_id, logging.DEBUG,
                                                       "File exists on FTP "
                                                       + "server - will retry later")
                                        retriever.file_not_processed()
                                    elif upload_result == nrtftp.UPLOAD_OK:
                                        log_instrument(logger, instrument_id, logging.DEBUG,
                                                       "File uploaded OK (look for individual failures in ZIPs)")
                                        retriever.file_succeeded()
                                    elif upload_result == PREPROCESSOR_FAILED:
                                        log_instrument(logger, instrument_id, logging.DEBUG,
                                                       "File preprocessor failed")
                                        retriever.file_failed()
                                    else:
                                        log_instrument(logger, instrument_id, logging.CRITICAL,
                                                       "Unrecognised upload result " + str(upload_result))
                                        post_msg(config, f"Unrecognised result while uploading to FTP: {str(upload_result)}")
                                        exit()
                                except Exception as e:
                                    log_instrument(logger, instrument_id, logging.ERROR,
                                                   f"Error processing file {file['filename']}  for instrument {instrument_id}:\n{traceback.format_exc()}")
                                    post_msg(config,
                                             f"Error processing NRT for instrument {instrument_id} ({instrument['name']})")
                                    retriever.file_failed()

                        retriever.shutdown()

                        nrtdb.set_last_check(db_conn, instrument)
        except Exception as e:
            log_instrument(logger, instrument_id, logging.ERROR,
                           f"Error processing instrument {instrument_id}: {traceback.format_exc()}")
            post_msg(config, f"Error processing NRT for instrument {instrument_id} ({instrument['name']})")

    if ftp_conn is not None:
        ftp_conn.close()

    if db_conn is not None:
        db_conn.close()


if __name__ == '__main__':
    main()
