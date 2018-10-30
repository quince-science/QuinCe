import pysftp

# Get a connection to the FTP server
def connect_ftp(ftp_config):
  return pysftp.Connection(host=ftp_config["server"],
      username=ftp_config["user"], password=ftp_config["password"])

# Generate the FTP folder name  for an instrument, and
# create it if necessary
def get_ftp_folder(ftpconn, ftp_config, instrument_id):
  folder = ftp_config["dir"]
  if len(folder) > 0:
    folder = folder + "/"

  folder = folder + str(instrument_id) + "/"

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
    get_ftp_folder(ftpconn, ftp_config, new_id)
