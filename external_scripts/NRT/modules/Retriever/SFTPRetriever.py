import logging
import traceback
import pysftp
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
            conn = pysftp.Connection(host=self._configuration["Server"],
                                     port=int(self._configuration["Port"]),
                                     username=self._configuration["User"],
                                     private_key=self._configuration["Private Key File"],
                                     private_key_pass=self._configuration["Private Key Password"])

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
            self._conn = pysftp.Connection(host=self._configuration["Server"],
                                           port=int(self._configuration["Port"]),
                                           username=self._configuration["User"],
                                           private_key=self._configuration["Private Key File"],
                                           private_key_pass=self._configuration["Private Key Password"])
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
