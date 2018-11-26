import logging
import toml, json
from io import BytesIO

# Local modules
import nrtftp, quince

ftpconn = None

# Log a message for a specific instrument
def log_instrument(logger, instrument_id, level, message):
  logger.log(level, str(instrument_id) + ":" + message)

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
  ftpconn = nrtftp.connect_ftp(config["FTP"])

  ftp_folders = nrtftp.get_instrument_folders(ftpconn, config["FTP"])
  quince_instruments = quince.get_instruments(config)

  for instrument_id in ftp_folders:
    if not quince.is_quince_instrument(instrument_id, quince_instruments):
      logger.warning("FTP folder " + instrument_id
        + " does not match a QuinCe instrument")
    else:
      logger.info("Processing instrument " + instrument_id)
      files = nrtftp.get_instrument_files(ftpconn, config["FTP"], instrument_id)
      for file in files:
        file_content = nrtftp.get_file(ftpconn, config["FTP"],
          instrument_id, file)

        log_instrument(logger, instrument_id, logging.DEBUG, "Uploading file " \
          + file)

        upload_result = quince.upload_file(config, instrument_id, file, file_content)
        if upload_result == 200:
          log_instrument(logger, instrument_id, logging.DEBUG, \
          	"Upload succeeded")
          nrtftp.upload_succeeded(ftpconn, config["FTP"], instrument_id, file)
        else:
          log_instrument(logger, instrument_id, logging.ERROR, \
          	"Upload failed (status code " + str(upload_result) + ")")
          nrtftp.upload_failed(ftpconn, config["FTP"], instrument_id, file)

  # Close down
  ftpconn.close()


if __name__ == '__main__':
   main()
