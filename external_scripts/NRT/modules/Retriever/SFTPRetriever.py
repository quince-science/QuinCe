import logging
import traceback
from paramiko import RSAKey, Ed25519Key, ECDSAKey, PKey
from cryptography.hazmat.primitives import serialization as crypto_serialization
from cryptography.hazmat.primitives.asymmetric import ed25519, dsa, rsa, ec
import paramiko
from io import BytesIO

from modules.Retriever import FileListRetriever

class SFTPRetriever(FileListRetriever.FileListRetriever):
    """
    File retriever for files held on an FTP server.
    """

    def __init__(self, instrument_id, logger, configuration=None):
        super().__init__(instrument_id, logger, configuration)
        self._conn = None

    @staticmethod
    def _get_config_entries():
        return ['Server', 'Port', 'User', 'Private Key File', 'Private Key Password',
                'Source Folder', 'File Specification']

    @staticmethod
    def get_type():
        return "SFTP"

    # noinspection PyBroadException
    def test_configuration(self):
        config_ok = True
        conn = None

        try:
            key = None
            if self._configuration["Private Key File"]:
                key = self._load_ssh_key(
                    self._configuration["Private Key File"],
                    self._configuration["Private Key Password"])
            
            ssh = paramiko.SSHClient()
            ssh.load_system_host_keys()

            ssh.connect(self._configuration["Server"], port=int(self._configuration["Port"]),
                username=self._configuration["User"], pkey=key)

            conn = ssh.open_sftp()

            if config_ok:
                try:
                    conn.chdir(self._configuration['Source Folder'])
                except Exception:
                    config_ok = False
                    print("Cannot access source folder: " + traceback.format_exc())
                    self.log(logging.CRITICAL, "Cannot access source folder: "
                             + traceback.format_exc())
        except Exception as e:
            config_ok = False
            print(f"Cannot connect to FTP server: {e}")
            self.log(logging.CRITICAL, f"Cannot connect to SFTP server: {traceback.format_exc()}")
        finally:
            if conn is not None:
                conn.close()

        return config_ok

    # noinspection PyBroadException
    def startup(self):
        result = True

        try:
            key = None
            if self._configuration["Private Key File"]:
                key = self._load_ssh_key(
                    self._configuration["Private Key File"],
                    self._configuration["Private Key Password"])
            
            ssh = paramiko.SSHClient()
            ssh.load_system_host_keys()

            ssh.connect(self._configuration["Server"], port=int(self._configuration["Port"]),
                username=self._configuration["User"], pkey=key)

            self._conn = ssh.open_sftp()
            self._conn.chdir(self._configuration['Source Folder'])
        except Exception:
            self.log(logging.CRITICAL, "Cannot log in to FTP server: "
                     + traceback.format_exc())
            result = False

        return result

    # noinspection PyBroadException
    def shutdown(self):
        if self._conn is not None:
            try:
                self._conn.close()
            except Exception:
                pass
            finally:
                self._conn = None

    def _get_all_files(self):
        all_files = self._conn.listdir()
        file_spec = None
        if "File Specification" in self._configuration and self._configuration["File Specification"] is not None:
            file_spec = self._configuration["File Specification"].split(";")

        if file_spec is None:
            return self.filter_file_list(all_files, ["*"])
        else:
            return self.filter_file_list(all_files, file_spec)

    def _load_file(self, filename):
        data = BytesIO()
        self._conn.getfo(filename, data)
        data.seek(0)
        return data.read()

    # Load an SSH private key
    # From https://stackoverflow.com/a/72512148/3416897
    def _load_ssh_key(self, key_file, password=None):
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
