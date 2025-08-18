from io import StringIO
from paramiko import RSAKey, Ed25519Key, ECDSAKey, PKey
from cryptography.hazmat.primitives import serialization as crypto_serialization
from cryptography.hazmat.primitives.asymmetric import ed25519, dsa, rsa, ec
import paramiko
from io import BytesIO
import stat

# Upload result codes
UPLOAD_OK = 0
NOT_INITIALISED = 1
FILE_EXISTS = 2


# Get a connection to the FTP server
def connect_ftp(ftp_config):
    key = load_ssh_key(
        ftp_config["private_key_file"],
        ftp_config["private_key_pass"])
    
    ssh = paramiko.SSHClient()
    ssh.load_system_host_keys()

    ssh.connect(ftp_config["server"], port=int(ftp_config["port"]),
        username=ftp_config["user"], pkey=key)

    return ssh.open_sftp()


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
    if not isdir(ftp_conn, folder):
        ftp_conn.mkdir(folder)

    if not isdir(ftp_conn, folder + "/inbox"):
        ftp_conn.mkdir(folder + "/inbox")

    if not isdir(ftp_conn, folder + "/succeeded"):
        ftp_conn.mkdir(folder + "/succeeded")

    if not isdir(ftp_conn, folder + "/failed"):
        ftp_conn.mkdir(folder + "/failed")

    return folder


# Create the FTP folders for the specified IDs
def add_instruments(ftp_conn, ftp_config, ids):
    for new_id in ids:
        init_ftp_folder(ftp_conn, ftp_config, new_id)


# Upload a file to the server
def upload_file(ftp_conn, ftp_config, instrument_id, filename, contents):
    upload_result = UPLOAD_OK

    # Silently ignore empty files
    if contents is not None:
        destination_folder = get_instrument_folder(ftp_config, instrument_id) \
            + "inbox/"
        destination_file = destination_folder + filename

        if not isdir(ftp_conn, destination_folder):
            init_ftp_folder(ftp_conn, ftp_config, instrument_id)

        if exists(ftp_conn, destination_file):
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

# Load an SSH private key
# From https://stackoverflow.com/a/72512148/3416897
def load_ssh_key(key_file, password=None):
    private_key = None

    with open(key_file) as file_obj:
        file_bytes = bytes(file_obj.read(), "utf-8")
        try:
            key = crypto_serialization.load_ssh_private_key(
                file_bytes,
                password=None if password == '' else password
            )
            file_obj.seek(0)
        except ValueError:
            key = crypto_serialization.load_pem_private_key(
                file_bytes,
                password=password,
            )
            if password:
                encryption_algorithm = crypto_serialization.BestAvailableEncryption(
                    password
                )
            else:
                encryption_algorithm = crypto_serialization.NoEncryption()
            file_obj = StringIO(
                key.private_bytes(
                    crypto_serialization.Encoding.PEM,
                    crypto_serialization.PrivateFormat.OpenSSH,
                    encryption_algorithm,
                ).decode("utf-8")
            )
        if isinstance(key, rsa.RSAPrivateKey):
            private_key = RSAKey.from_private_key(file_obj, password)
        elif isinstance(key, ed25519.Ed25519PrivateKey):
            private_key = Ed25519Key.from_private_key(file_obj, password)
        elif isinstance(key, ec.EllipticCurvePrivateKey):
            private_key = ECDSAKey.from_private_key(file_obj, password)
        else:
            raise TypeError
    return private_key

def isdir(conn, path):
    try:
        return stat.S_ISDIR(conn.stat(path).st_mode)
    except FileNotFoundError:
        return False

def exists(conn, path):
    exists = True

    try:
        conn.stat(path)
    except FileNotFoundError:
        exists = False

    return exists