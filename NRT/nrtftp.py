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
  folder = get_instrument_folder(ftp_config, instrument_id)

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

# Get the list of instrument folders
def get_instrument_folders(ftpconn, ftp_config):
  folder_list = ftpconn.listdir(ftp_config["dir"])
  return folder_list

# Get the list of files waiting to be uploaded for an instrument
def get_instrument_files(ftpconn, ftp_config, instrument_id):
  inbox = get_instrument_folder(ftp_config, instrument_id) + "inbox/"
  return ftpconn.listdir(inbox)

# Get a file from the inbox for an instrument
def get_file(ftpconn, ftp_config, instrument_id, file):
  file_path = get_instrument_folder(ftp_config, instrument_id) + "inbox/" \
    + file

  result = BytesIO()
  ftpconn.getfo(file_path, result)
  return result.getvalue()

# Move a file to the Succeeded folder
def upload_succeeded(ftpconn, ftp_config, instrument_id, file):
  _move_file(ftpconn, ftp_config, instrument_id, file, "succeeded")


# Move a file to the Failed folder
def upload_failed(ftpconn, ftp_config, instrument_id, file):
  _move_file(ftpconn, ftp_config, instrument_id, file, "failed")

# Move a file from the inbox to the specified folder
def _move_file(ftpconn, ftp_config, instrument_id, file, destination):
  source_path = get_instrument_folder(ftp_config, instrument_id) + "inbox/" \
    + file
  dest_path = get_instrument_folder(ftp_config, instrument_id) + destination \
    + "/" + file

  try:
    ftpconn.remove(dest_path)
  except IOError:
    pass

  ftpconn.rename(source_path, dest_path)
