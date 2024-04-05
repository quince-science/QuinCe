"""
Class to combine data from multiple files into a unified output.

Data can be split into multiple files according to the supplied configuration.
"""
from datetime import datetime


class Splitter:

    def __init__(self, config):
        """
        Constructor. Store the config and set up the outputs.
        :param config: The combiner configuration.
        """
        self._config = config

        # The date that will be used in output filenames
        self._date = datetime.now()

        # Records whether data has been added or not. This can only be done once.
        self._data_added = False
        self._filename = None

        # Initialise combined strings
        self._output = {}
        for key in self._config.keys():
            self._output[key] = ''

    def set_data(self, filename, content):
        """
        Add a file's content to the output.
        :param filename: The filename.
        :param content: The file content.
        :return: Nothing.
        """

        if self._data_added:
            raise ValueError('Data has already been added')

        lines = content.split('\n')
        for line in lines:
            stripped_line = line.strip()
            if len(stripped_line) > 0:
                output_key = self._get_key(stripped_line)

                if output_key is None:
                    raise ValueError(f'Cannot find key for line: {line}')

                key_config = self._config[output_key]

                # Remove extra empty columns if required
                if key_config['strip_empty_fields']:
                    stripped_line = stripped_line.strip(key_config['separator'])

                self._output[output_key] = self._output[output_key] + f'{stripped_line}\n'

        self._filename = filename
        self._data_added = True

    def write_output(self, folder):
        """
        Write all outputs to the specified folder.
        The filename for each output will be <output_key>_<date>, where the date is that stored in the constructor.
        :param folder: The output folder.
        :return: Nothing.
        """

        for key in self._output:
            if len(self._output[key].strip()) > 0:
                filename = f'{key}_{self._filename}'
                with open(f'{folder}/{filename}', 'w') as out:
                    out.write(f'{self._output[key]}\n')

    def _get_key(self, line):
        """
        Examine a line to determine which output key it belongs to.
        :param line: The line.
        :return: The matching output key
        """

        matched_key = None

        for key in self._output.keys():
            key_config = self._config[key]

            fields = line.split(key_config['separator'])
            if fields[key_config['id_field']] == key_config['id_value']:
                matched_key = key
                break

        return matched_key
