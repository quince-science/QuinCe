import pysftp
from io import BytesIO

# Upload result codes
UPLOAD_OK = 0
NOT_INITIALISED = 1
FILE_EXISTS = 2


# Get a connection to the FTP server
def connect_ftp(ftp_config):
    return pysftp.Connection(host=ftp_config["server"],
                             port=ftp_config["port"],
                             username=ftp_config["user"],
                             private_key=ftp_config["private_key_file"],
                             private_key_pass=ftp_config["private_key_pass"])


# Generate the FTP folder name for an instrument
def get_instrument_folder(ftp_config, instrument_id):
    folder = ftp_config["dir"]
    if len(folder) > 0:
        folder = folder + "/"

    folder = folder + str(instrument_id) + "/"

    return folder


# Initialise the FTP folder for an instrument
def init_ftp_folder(ftp_conn, ftp_config, instrument_id):
    folder = get_instrument_folder(ftp_config, instrument_id)

    # Create the folder if it doesn't exist
    if not ftp_conn.isdir(folder):
        ftp_conn.mkdir(folder)

    if not ftp_conn.isdir(folder + "/inbox"):
        ftp_conn.mkdir(folder + "/inbox")

    if not ftp_conn.isdir(folder + "/succeeded"):
        ftp_conn.mkdir(folder + "/succeeded")

    if not ftp_conn.isdir(folder + "/failed"):
        ftp_conn.mkdir(folder + "/failed")

    return folder


# Create the FTP folders for the specified IDs
def add_instruments(ftp_conn, ftp_config, ids):
    for new_id in ids:
        init_ftp_folder(ftp_conn, ftp_config, new_id)


# Upload a file to the server
def upload_file(ftp_conn, ftp_config, instrument_id, filename, contents):
    upload_result = UPLOAD_OK

    destination_folder = get_instrument_folder(ftp_config, instrument_id) \
        + "inbox/"
    destination_file = destination_folder + filename

    if not ftp_conn.isdir(destination_folder):
        upload_result = NOT_INITIALISED
    elif ftp_conn.exists(destination_file):
        upload_result = FILE_EXISTS
    else:
        ftp_conn.putfo(BytesIO(contents), destination_file)

    return upload_result


# Get the list of instrument folders
def get_instrument_folders(ftp_conn, ftp_config):
    folder_list = ftp_conn.listdir(ftp_config["dir"])
    return folder_list


# Get the list of files waiting to be uploaded for an instrument
def get_instrument_files(ftp_conn, ftp_config, instrument_id):
    inbox = get_instrument_folder(ftp_config, instrument_id) + "inbox/"
    return ftp_conn.listdir(inbox)


# Get a file from the inbox for an instrument
def get_file(ftp_conn, ftp_config, instrument_id, file):
    file_path = get_instrument_folder(ftp_config, instrument_id) + "inbox/" \
                + file

    result = BytesIO()
    ftp_conn.getfo(file_path, result)
    return result.getvalue()


# Move a file to the Succeeded folder
def upload_succeeded(ftp_conn, ftp_config, instrument_id, file):
    _move_file(ftp_conn, ftp_config, instrument_id, file, "succeeded")


# Move a file to the Failed folder
def upload_failed(ftp_conn, ftp_config, instrument_id, file):
    _move_file(ftp_conn, ftp_config, instrument_id, file, "failed")


# Move a file from the inbox to the specified folder
def _move_file(ftp_conn, ftp_config, instrument_id, file, destination):
    source_path = get_instrument_folder(ftp_config, instrument_id) + "inbox/" \
        + file
    destination_path = get_instrument_folder(ftp_config, instrument_id) + destination \
        + "/" + file

    try:
        ftp_conn.remove(destination_path)
    except IOError:
        pass

    ftp_conn.rename(source_path, destination_path)
