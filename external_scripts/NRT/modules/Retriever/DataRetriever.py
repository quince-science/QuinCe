from abc import abstractmethod

from ConfigurableItem import ConfigurableItem


class DataRetriever(ConfigurableItem):

    # Base constructor
    def __init__(self, instrument_id, logger, configuration=None):
        super().__init__(configuration)

        self.instrument_id = instrument_id
        self.logger = logger

        # This will be an array of {filename, file_contents}
        self.current_files = []

    # Log a message for a specific instrument
    def log(self, level, message):
        if self.logger is None:
            print(message)
        else:
            self.logger.log(level, str(self.instrument_id) + ":" + message)

    # Get the configuration type
    @staticmethod
    @abstractmethod
    def get_type():
        raise NotImplementedError("get_type not implemented")

    # Initialise the retriever ready to retrieve files
    @abstractmethod
    def startup(self):
        raise NotImplementedError("startup not implemented")

    # Clean up the retriever
    @abstractmethod
    def shutdown(self):
        raise NotImplementedError("shutdown not implemented")

    # Get the next file to be processed
    # and put it in the current_files variable in the form:
    # [{name="xx", content=<bytes>}]
    @abstractmethod
    def _retrieve_next_file_set(self):
        raise NotImplementedError("_retrieve_next_file_set not implemented")

    # The file(s) have been processed successfully;
    # clean them up accordingly
    @abstractmethod
    def _cleanup_success(self):
        raise NotImplementedError("_cleanup_success not implemented")

    # The file(s) were not processed successfully;
    # clean them up accordingly
    @abstractmethod
    def _cleanup_fail(self):
        raise NotImplementedError("_cleanup_fail not implemented")

    # The file(s) were not processed this time;
    # clean them up so they can be reprocessed later
    @abstractmethod
    def _cleanup_not_processed(self):
        raise NotImplementedError("_cleanup_not_processed not implemented")

    # Get the next file to be processed
    def load_next_files(self):
        # Reset the file list
        self.current_files = []
        self._retrieve_next_file_set()
        return len(self.current_files) > 0

    # Add a file to the current files list
    def _add_file(self, filename, contents):
        new_file = {"filename": filename, "contents": contents}
        self.current_files.append(new_file)

    # The file(s) have been processed successfully
    def file_succeeded(self):
        self._cleanup_success()
        self.current_files = []

    # The file(s) failed to process
    def file_failed(self):
        self._cleanup_fail()
        self.current_files = []

    # The file(s) were not processed
    def file_not_processed(self):
        self._cleanup_not_processed()
        self.current_files = []
