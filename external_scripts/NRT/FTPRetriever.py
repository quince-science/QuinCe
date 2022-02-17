import logging
import traceback
from ftplib import FTP
from pathlib import PurePath

from FileListRetriever import FileListRetriever


class FTPRetriever(FileListRetriever):
    """
    File retriever for files held on an FTP server.
    """
    def __init__(self, instrument_id, logger, configuration=None):
        super().__init__(instrument_id, logger, configuration)
        self._ftp = None

    @staticmethod
    def _get_config_entries():
        return ['Server', 'Port', 'User', 'Password', 'Source Folder', 'File Specification']

    @staticmethod
    def get_type():
        return "FTP"

    # noinspection PyBroadException
    def test_configuration(self):
        config_ok = True

        try:
            with FTP() as ftp:
                try:
                    ftp.connect(self._configuration['Server'], port=int(self._configuration['Port']))
                except Exception:
                    config_ok = False
                    print("Cannot connect to FTP server: " + traceback.format_exc())
                    self.log(logging.CRITICAL, "Cannot connect to FTP server: "
                             + traceback.format_exc())

                if config_ok:
                    try:
                        ftp.login(user=self._configuration['User'], passwd=self._configuration['Password'])
                    except Exception:
                        config_ok = False
                        print("Cannot log in to FTP server: " + traceback.format_exc())
                        self.log(logging.CRITICAL, "Cannot log in to FTP server: "
                                 + traceback.format_exc())

                if config_ok:
                    try:
                        ftp.cwd(self._configuration['Source Folder'])
                    except Exception:
                        config_ok = False
                        print("Cannot access source folder: " + traceback.format_exc())
                        self.log(logging.CRITICAL, "Cannot access source folder: "
                                 + traceback.format_exc())

                ftp.quit()
        except Exception:
            config_ok = False
            self.log(logging.CRITICAL, "Error checking FTP configuration: "
                     + traceback.format_exc())

        return config_ok

    # noinspection PyBroadException
    def startup(self):
        result = True

        try:
            self._ftp = FTP()
            self._ftp.connect(self._configuration['Server'], port=int(self._configuration['Port']))
            self._ftp.login(user=self._configuration['User'], passwd=self._configuration['Password'])
            self._ftp.cwd(self._configuration['Source Folder'])
        except Exception:
            self.log(logging.CRITICAL, "Cannot log in to FTP server: "
                     + traceback.format_exc())
            result = False

        return result

    def shutdown(self):
        self._ftp.close()
        self._ftp = None

    def _get_all_files(self):
        all_files = self._ftp.nlst()
        return list(filter(lambda name: PurePath(name).match(self._configuration["File Specification"]), all_files))

    def _load_file(self, filename):
        result = bytearray()
        self._ftp.retrbinary(f'RETR {filename}', callback=result.extend)
        return result
