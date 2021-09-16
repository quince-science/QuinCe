import logging

import toml

# Local modules
import nrtftp
import quince


# Log a message for a specific instrument
def log_instrument(logger, instrument_id, level, message):
    logger.log(level, str(instrument_id) + ":" + message)


##########################################################

def main():
    # Read in the config
    with open("config.toml", "r") as config_file:
        config = toml.loads(config_file.read())

    # Set up logging
    logging.basicConfig(filename="nrt_uploader.log",
                        format="%(asctime)s:%(levelname)s:%(message)s")
    logger = logging.getLogger('nrt_uploader')
    logger.setLevel(level=config["Logging"]["level"])

    # Connect to the FTP server
    ftp_conn = nrtftp.connect_ftp(config["FTP"])

    ftp_folders = nrtftp.get_instrument_folders(ftp_conn, config["FTP"])
    quince_instruments = quince.get_instruments(config)

    for instrument_id in ftp_folders:
        if not quince.is_quince_instrument(instrument_id, quince_instruments):
            logger.warning("FTP folder " + instrument_id
                           + " does not match a QuinCe instrument")
        else:
            logger.info("Processing instrument " + instrument_id)
            files = nrtftp.get_instrument_files(ftp_conn, config["FTP"], instrument_id)
            for file in files:
                file_content = nrtftp.get_file(ftp_conn, config["FTP"],
                                               instrument_id, file)

                log_instrument(logger, instrument_id, logging.DEBUG, "Uploading file "
                               + file)

                upload_result = quince.upload_file(config, instrument_id, file, file_content)
                status_code = upload_result.status_code
                if status_code == 200:
                    log_instrument(logger, instrument_id, logging.DEBUG,
                                   "Upload succeeded")
                    nrtftp.upload_succeeded(ftp_conn, config["FTP"], instrument_id, file)
                else:
                    log_instrument(logger, instrument_id, logging.ERROR,
                                   "Upload failed (status code " + str(status_code) + ")")
                    log_instrument(logger, instrument_id, logging.ERROR, upload_result.text)

                    nrtftp.upload_failed(ftp_conn, config["FTP"], instrument_id, file)

    # Close down
    if ftp_conn is not None:
        ftp_conn.close()


if __name__ == '__main__':
    main()
