from abc import ABCMeta, abstractmethod
from tabulate import tabulate
import json, getpass

class DataRetriever(metaclass=ABCMeta):

  # Base constructor
  def __init__(self, instrument_id, logger):
    self.instrument_id = instrument_id
    self.logger = logger
    self.configuration = {}
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

  # Get the next file to be processed
  # and put it in the current_files variable in the form:
  # [{name="xx", content=<bytes>}]
  @abstractmethod
  def _retrieve_next_file(self):
    raise NotImplementedError("_retrieve_next_file not implemented")

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

  # Print the current configuration values
  def print_configuration(self):
    table_data = []

    for key, value in self.configuration.items():
      if value is None:
        value = "NOT SET"
      elif key.lower() == "password":
        value = "***"

      table_data.append([key, value])

    print(tabulate(table_data))

  # Ask the user for all configuration values
  def enter_configuration(self):
    print("Enter configuration values")
    print("--------------------------")

    for key, existing_value in self.configuration.items():
      new_value = None
      if existing_value is None:
        existing_value = "NOT SET"
      elif key.lower() == "password":
        existing_value = "***"

      while new_value is None:

        if key.lower() == "password":
          input_value = getpass.getpass("%s [%s]: " % (key, existing_value)).strip()
        else:
          input_value = input("%s [%s]: " % (key, existing_value)).strip()

        if input_value == "":
          if existing_value is not None:
            new_value = self.configuration[key]
        else:
          new_value = input_value

      self.configuration[key] = new_value

    print()
    return self.test_configuration()

  # Get the configuration as a JSON object
  def get_configuration_json(self):
    return json.dumps(self.configuration)

  # Get the next file to be processed
  def load_next_file(self):
    # Reset the file list
    self.current_files = []

    self._retrieve_next_file()
    return len(self.current_files) > 0

  # Add a file to the current files list
  def _add_file(self, filename, contents):
    new_file = {}
    new_file["filename"] = filename
    new_file["contents"] = contents
    self.current_files.append(new_file)

  # The file(s) have been processed successfully
  def file_succeeded(self):
    self._cleanup_success()
    self.current_files = []

  # The file(s) failed to process
  def file_failed(self):
    self._cleanup_fail()
    self.current_files = []
