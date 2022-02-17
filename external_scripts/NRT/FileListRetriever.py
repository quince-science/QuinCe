import sqlite3
import time
from abc import abstractmethod
from hashlib import sha256

from DataRetriever import DataRetriever
from NotFoundException import NotFoundException

STATUS_COMPLETE = 1
STATUS_RETRY = 0
STATUS_FAILED = -1


def timestamp():
    return int(time.time())


class FileListRetriever(DataRetriever):
    """
    Base class for a retriever based on list of files.
    The files are not deleted when they've been processed,
    so this class keeps track of which files it's processed in a database.
    Changed files are detected and reprocessed.

    The class tries to reduce file reading activity by keeping a cached copy
    of one downloaded file. Hopefully the NRT retriever will ask for all info
    about that file in one go, so we should only need to download each file once
    per session.
    """
    _DB_FILE = 'FileListRetriever.sqlite'

    def __init__(self, instrument_id, logger, configuration=None):
        super().__init__(instrument_id, logger, configuration)
        self._cached_file = None
        self._cached_content = None

        # Check that the database exists
        if not self._is_db_set_up():
            self._init_db()

        # Tracker for retrieved files
        self._file_list = None
        self._current_file_index = -1

    def _is_db_set_up(self):
        """
        Determines whether or not the database tracking processed files has been initialised.
        :return: True if the database is set up; False otherwise
        """
        with sqlite3.connect(self._DB_FILE) as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='files'")
            result = len(cursor.fetchall()) > 0
        return result

    def _init_db(self):
        """
        Initialise the database that records which files have been processed.
        :return: Nothing
        """
        table_sql = ("CREATE TABLE files("
                     "instrument_id INTEGER, "
                     "filename TEXT, "
                     "hashsum TEXT, "
                     "status INTEGER, "
                     "timestamp INTEGER, "
                     "PRIMARY KEY (instrument_id, filename)"
                     ")"
                     )

        with sqlite3.connect(self._DB_FILE) as conn:
            cursor = conn.cursor()
            cursor.execute(table_sql)
            conn.commit()

    @staticmethod
    @abstractmethod
    def get_type():
        raise NotImplementedError("get_type not implemented")

    # Test the configuration to make sure everything works
    @abstractmethod
    def test_configuration(self):
        raise NotImplementedError("test_configuration not implemented")

    # Initialise the retriever ready to retrieve files
    @abstractmethod
    def startup(self):
        raise NotImplementedError("startup not implemented")

    # Clean up the retriever
    @abstractmethod
    def shutdown(self):
        raise NotImplementedError("shutdown not implemented")

    def _retrieve_next_file_set(self):
        """
        Get the next file to be processed
        and put it in the current_files variable in the form:
        [{name="xx", content=<bytes>}]

        We only download one file at a time
        """
        if self._file_list is None:
            self._file_list = self._get_all_files()

        # Loop through all files until we find one that needs processing
        file_to_process = None

        while file_to_process is None and self._current_file_index < len(self._file_list) - 1:
            self._current_file_index += 1
            filename = self._file_list[self._current_file_index]

            process_file = False

            if self._needs_processing(filename):
                process_file = True
            else:
                if self._file_updated(filename, self._get_hashsum(filename)):
                    process_file = True

            if process_file:
                file_to_process = filename
                break

        if file_to_process is not None:
            self._add_file(file_to_process, self._get_file_content(file_to_process))

    def _record_file(self, status):
        """
        Record the status of the file(s) currently being processed
        :param status: The status
        :return: Nothing
        """
        with sqlite3.connect(self._DB_FILE) as conn:
            cursor = conn.cursor()

            # Update database (add or update)
            for file in self.current_files:
                if not self._file_known(file["filename"]):
                    sql = ("INSERT INTO files "
                           "(instrument_id, filename, hashsum, status, timestamp) "
                           "VALUES (?, ?, ?, ?, ?)"
                           )

                    cursor.execute(sql,
                                   (self.instrument_id, file["filename"], self._hashsum(file["contents"]),
                                    status, timestamp()))

                else:
                    sql = ("UPDATE files SET "
                           "hashsum = ?, status = ?, timestamp = ? "
                           "WHERE instrument_id = ? AND filename = ?"
                           )

                    cursor.execute(sql,
                                   (self._hashsum(file["contents"]), status, timestamp(),
                                    self.instrument_id, file["filename"]))

            conn.commit()

    def _cleanup_success(self):
        self._record_file(STATUS_COMPLETE)

    def _cleanup_fail(self):
        self._record_file(STATUS_FAILED)

    def _cleanup_not_processed(self):
        self._record_file(STATUS_RETRY)

    @abstractmethod
    def _get_all_files(self):
        raise NotImplementedError("_get_all_files not implemented")

    def _get_hashsum(self, filename):
        """
        Get the hashsum for a file
        :param filename: The filename
        :return: The file's hashsum
        """
        return self._hashsum(self._get_file_content(filename))

    def _get_file_content(self, filename):
        """
        Get the contents of a specified file
        :param filename: The filename
        :return: The file contents
        """
        if self._cached_file is None or self._cached_file != filename:
            self._cached_file = filename
            self._cached_content = self._load_file(filename)

        return self._cached_content

    @abstractmethod
    def _load_file(self, filename):
        """
        Retrieve the contents of the specified file
        :param filename: The filename
        :return: The file contents
        """
        raise NotImplementedError("_load_file not implemented")

    def _cleanup_file_action(self, filename):
        """
        Clean up a file after processing.
        :param filename: The file that has been processed
        :return: Nothing
        """
        # By default we don't do any cleanup.
        # Concrete implementations can override if they wish
        pass

    # See if a given file needs processing
    def _needs_processing(self, filename):
        """
        See if a given file needs to be processed.
        Unchanged files that have been processed before require no action.
        :param filename: The file
        :return: True if processing is required; False if not
        """
        result = False

        with sqlite3.connect(self._DB_FILE) as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT filename, status FROM files WHERE instrument_id = ? AND filename = ?",
                           (self.instrument_id, filename))

            record = cursor.fetchone()
            if record is None:
                result = True
            elif record[1] == STATUS_RETRY:
                result = True

        return result

    def _file_known(self, filename):
        """
        See if a file is known in the database
        :param filename: The filename
        :return: True if the file is in the database; False if not
        """
        with sqlite3.connect(self._DB_FILE) as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT filename FROM files WHERE instrument_id = ? AND filename = ?",
                           (self.instrument_id, filename))
            result = len(cursor.fetchall()) > 0
        return result

    def _file_updated(self, filename, new_hashsum):
        """
        See if the hashsum for a given file has changed since it was recorded in the database.
        :param filename: The filename The filename
        :param new_hashsum: The file's current hashsum
        :return: True if the hashsum in the database does not match the hashsum provided
        """
        with sqlite3.connect(self._DB_FILE) as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT hashsum FROM files WHERE instrument_id = ? AND filename = ?",
                           (self.instrument_id, filename))
            row = cursor.fetchone()
            if row is None:
                raise NotFoundException('Database entry', filename)
            else:
                old_hashsum = row[0]
                result = new_hashsum != old_hashsum

        return result

    @staticmethod
    def _hashsum(data):
        """
        Calculate the SHA256 hashsum for the provided data
        :param data: The data
        :return: The data's hashsum
        """
        sha_input = data if type(data) == bytearray else data.encode('utf-8')
        return sha256(sha_input).hexdigest()
