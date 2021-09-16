import logging
import ntpath
import re
import traceback
from io import BytesIO
from zipfile import ZipFile

import json
import toml

import PreprocessorFactory
import RetrieverFactory
# Local modules
import nrtdb
import nrtftp

IGNORE_REGEXPS = [".*err.txt"]


# See if a file should be ignored based on its filename
def ignore_file(filename):
    result = False

    for expr in IGNORE_REGEXPS:
        if re.match(expr, filename):
            result = True
            break

    return result


# Upload a file to the FTP server. If it's a ZIP file,
# extract it and upload the contents as individual files
def upload_file(logger, ftp_conn, ftp_config, instrument_id, preprocessor, filename, contents):
    upload_result = nrtftp.UPLOAD_OK

    if not str.endswith(filename, ".zip"):
        if not ignore_file(filename):
            upload_result = nrtftp.upload_file(ftp_conn, ftp_config,
                                               instrument_id, filename, preprocessor.preprocess(BytesIO(contents)))
    else:
        with ZipFile(BytesIO(contents), 'r') as unzip:
            for name in unzip.namelist():
                if not ignore_file(name):
                    log_instrument(logger, instrument_id, logging.DEBUG, "Uploading "
                                   + "ZIP entry " + name)

                    preprocessed_file = None
                    try:
                        preprocessed_file = preprocessor.preprocess(BytesIO(unzip.read(name)))
                    except Exception:
                        log_instrument(logger, instrument_id, logging.ERROR, "Preprocessing "
                                       + "failed: " + traceback.format_exc())

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
        log_instrument(logger, instrument_id, logging.INFO,
                       "Processing instrument")
        instrument = nrtdb.get_instrument(db_conn, instrument_id)

        if instrument["type"] is None:
            log_instrument(logger, instrument_id, logging.ERROR,
                           "Configuration type not set")
        else:
            # Build the retriever
            if instrument["type"] == "None":
                log_instrument(logger, instrument_id, logging.ERROR,
                               "Instrument is not configured")
            else:
                retriever = RetrieverFactory.get_instance(instrument["type"],
                                                          instrument_id, logger, json.loads(instrument["config"]))

                # Make sure configuration is still valid
                if not retriever.test_configuration():
                    log_instrument(logger, instrument_id, logging.ERROR,
                                   "Configuration invalid")
                # Initialise the retriever
                elif not retriever.startup():
                    log_instrument(logger, instrument_id, logging.ERROR,
                                   "Could not initialise retriever")
                else:
                    preprocessor = PreprocessorFactory.get_new_instance(instrument["preprocessor"])

                    # Loop through all files returned by the retriever one by one
                    while retriever.load_next_file():
                        for file in retriever.current_files:

                            log_instrument(logger, instrument_id, logging.DEBUG,
                                           "Uploading " + file["filename"] + " to FTP server")

                            upload_result = upload_file(logger, ftp_conn, config["FTP"],
                                                        instrument_id, preprocessor, file["filename"], file["contents"])

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
                            else:
                                log_instrument(logger, instrument_id, logging.CRITICAL,
                                               "Unrecognised upload result " + str(upload_result))
                                exit()

                    retriever.shutdown()

    if ftp_conn is not None:
        ftp_conn.close()

    if db_conn is not None:
        db_conn.close()


if __name__ == '__main__':
    main()
