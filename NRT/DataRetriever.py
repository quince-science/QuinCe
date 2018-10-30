from abc import ABCMeta, abstractmethod
from tabulate import tabulate
import json

class DataRetriever(metaclass=ABCMeta):

  # Empty constructor
  def __init__(self):
    self.configuration = {}

  # Get the configuration type
  @staticmethod
  @abstractmethod
  def get_type():
    raise NotImplementedError("get_type not implemented")

  # Test the configuration to make sure everything works
  @abstractmethod
  def _test_configuration(self):
    raise NotImplementedError("_test_configuration not implemented")


  # Print the current configuration values
  def print_configuration(self):
    table_data = []

    for key, value in self.configuration.items():
      if value is None:
        value = "NOT SET"
      table_data.append([key, value])

    print(tabulate(table_data))

  # Ask the user for all configuration values
  def enter_configuration(self):
    print("Enter configuration values")
    print("--------------------------")

    for key, existing_value in self.configuration.items():
      new_value = None

      while new_value is None:
        input_value = input("%s [%s]: " % (key, existing_value if existing_value is not None else "NOT SET")).strip()
        if input_value == "":
          if existing_value is not None:
            new_value = existing_value
        else:
          new_value = input_value

      self.configuration[key] = new_value

    self._test_configuration()

  # Get the configuration as a JSON object
  def get_configuration_json(self):
    return json.dumps(self.configuration)
