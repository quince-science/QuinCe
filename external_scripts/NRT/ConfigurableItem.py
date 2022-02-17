from abc import abstractmethod
import getpass
import json
from tabulate import tabulate


class ConfigurableItem(object):
    def __init__(self, configuration=None):
        if configuration is None:
            self._configuration = {}
            for entry in self._get_config_entries():
                self._configuration[entry] = None
        else:
            self._configuration = configuration

    # Test the configuration to make sure everything works
    @abstractmethod
    def test_configuration(self):
        raise NotImplementedError("test_configuration not implemented")

    @staticmethod
    def _get_config_entries():
        return []

    # Ask the user for all configuration values
    def enter_configuration(self):
        print("Enter configuration values")
        print("--------------------------")

        for key, existing_value in self._configuration.items():
            new_value = None
            if existing_value is None:
                existing_value = "NOT SET"
            elif key.lower() == "password":
                existing_value = "***"

            if key.lower() == "password":
                input_value = getpass.getpass("%s [%s]: " % (key, existing_value)).strip()
            else:
                input_value = input("%s [%s]: " % (key, existing_value)).strip()

            if input_value == "":
                if existing_value is not None:
                    new_value = self._configuration[key]
            else:
                new_value = input_value

            self._configuration[key] = new_value

        return self.test_configuration()

    # Print the current configuration values
    def print_configuration(self):
        table_data = []

        for key, value in self._configuration.items():
            if value is None:
                value = "NOT SET"
            elif key.lower() == "password":
                value = "***"

            table_data.append([key, value])

        print(tabulate(table_data))

    # Get the configuration as a JSON object
    def get_configuration_json(self):
        return json.dumps(self._configuration)

    def has_config(self):
        return len(self._configuration) > 0
