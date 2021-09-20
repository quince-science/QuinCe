import logging
import traceback
from ftplib import FTP
from pathlib import PurePath

from FileListRetriever import FileListRetriever


class FTPRetriever(FileListRetriever):
    def __init__(self, instrument_id, logger, configuration=None):
        super().__init__(instrument_id, logger, configuration)
        self._ftp = None
        self._cached_file = None
        self._cached_content = None

    @staticmethod
    def _get_config_entries():
        return ['Server', 'Port', 'User', 'Password', 'Source Folder']

    @staticmethod
    def get_type():
        return "FTP"

    # noinspection PyBroadException
    def test_configuration(self):
        config_ok = True

        try:
            with FTP() as ftp:
                try:
                    ftp.connect(self.configuration['Server'], port=int(self.configuration['Port']))
                except Exception:
                    config_ok = False
                    self.log(logging.CRITICAL, "Cannot connect to FTP server: "
                             + traceback.format_exc())

                if config_ok:
                    try:
                        ftp.login(user=self.configuration['User'], passwd=self.configuration['Password'])
                    except Exception:
                        config_ok = False
                        self.log(logging.CRITICAL, "Cannot log in to FTP server: "
                                 + traceback.format_exc())

                if config_ok:
                    try:
                        ftp.cwd(self.configuration['Source Folder'])
                    except Exception:
                        config_ok = False
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
            self._ftp.connect(self.configuration['Server'], port=int(self.configuration['Port']))
            self._ftp.login(user=self.configuration['User'], passwd=self.configuration['Password'])
            self._ftp.cwd(self.configuration['Source Folder'])
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
        return filter(lambda name: PurePath(name).match(self.configuration["File Specification"]), all_files)

    def _get_hashsum(self, filename):
        self._download_file(filename)
        return self._hashsum(self._cached_content)

    def _get_file_content(self, filename):
        self._download_file(filename)
        return self._cached_content

    def _download_file(self, filename):
        if self._cached_file is None or self._cached_file != filename:
            self._cached_file = filename
            self._cached_content = bytearray()
            self._ftp.retrbinary(f'RETR {filename}', callback=self._cached_content.extend)
