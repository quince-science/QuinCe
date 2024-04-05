"""
Abstract class for file retrievers
"""
from abc import abstractmethod


class FileRetriever:
    def __init__(self, station_name, config):
        self.station_name = station_name
        self.config = config

    @abstractmethod
    def get_files(self, last_file, last_file_date):
        """
        Get a list of files modified since the specified file name and date.

        The method implementation can decide which of the two parameters it uses
        to obtain the file list.

        The returned list must contain entries that allow the retriever to get the
        contents of a file. It may be the filename or some other identifier.

        The returned list must be presented in order of ascending modification date, so the last file in the
        list is the most recently modified.

        :param last_file: The filename of the last processed file.
        :param last_file_date: The modification date of the last processed file.
        :return: The list of new files to be combined.
        """
        raise NotImplementedError()

    @abstractmethod
    def get_file(self, file_id):
        """
        Get the contents of a specified file as a string.

        The file_id may be a filename or another identifier that the retriever can use to access the file.

        :param file_id: The file id.
        :return: The file contents.
        """
        raise NotImplementedError()

    @abstractmethod
    def get_file_details(self, file_id):
        """
        Get the name and modification date of the specified file
        :param file_id: The file id.
        :return: The file's name and modification date.
        """
        raise NotImplementedError()