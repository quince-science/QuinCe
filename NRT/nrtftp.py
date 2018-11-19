import pysftp
from io import BytesIO

# Upload result codes
UPLOAD_OK = 0
NOT_INITIALISED = 1
FILE_EXISTS = 2

# Get a connection to the FTP server
def connect_ftp(ftp_config):
  return pysftp.Connection(host=ftp_config["server"],
      username=ftp_config["user"], password=ftp_config["password"])

# Generate the FTP folder name for an instrument
def get_instrument_folder(ftp_config, instrument_id):
  folder = ftp_config["dir"]
  if len(folder) > 0:
    folder = folder + "/"

  folder = folder + str(instrument_id) + "/"

  return folder

# Initialise the FTP folder for an instrument
def init_ftp_folder(ftpconn, ftp_config, instrument_id):
  folder = get_instrment_folder(ftp_config, instrument_id)

  # Create the folder if it doesn't exist
  if not ftpconn.isdir(folder):
    ftpconn.mkdir(folder)

  if not ftpconn.isdir(folder + "/inbox"):
    ftpconn.mkdir(folder + "/inbox")

  if not ftpconn.isdir(folder + "/succeeded"):
    ftpconn.mkdir(folder + "/succeeded")

  if not ftpconn.isdir(folder + "/failed"):
    ftpconn.mkdir(folder + "/failed")

  return folder

# Create the FTP folders for the specified IDs
def add_instruments(ftpconn, ftp_config, ids):
  for new_id in ids:
    init_ftp_folder(ftpconn, ftp_config, new_id)

# Upload a file to the server
def upload_file(ftpconn, ftp_config, instrument_id, filename, contents):
  upload_result = UPLOAD_OK

  destination_folder = get_instrument_folder(ftp_config, instrument_id) \
    + "inbox/"
  destination_file = destination_folder + filename

  if not ftpconn.isdir(destination_folder):
    upload_result = NOT_INITIALISED
  elif ftpconn.exists(destination_file):
    upload_result = FILE_EXISTS
  else:
    ftpconn.putfo(BytesIO(contents), destination_file)

  return upload_result
