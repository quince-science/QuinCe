"""
Class to combine data from multiple files into a unified output.

Data can be split into multiple files according to the supplied configuration.
"""
from datetime import datetime
from io import StringIO
import csv
import operator


class Combiner:

    def __init__(self, config):
        """
        Constructor. Store the config and set up the outputs.
        :param config: The combiner configuration.
        """
        self._config = config

        # The date that will be used in output filenames
        self._date = datetime.now()

        # Initialise combined strings
        self._output = {}
        for key in self._config.keys():
            self._output[key] = ''

    def add_data(self, content):
        """
        Add a file's content to the output.
        :param content: The file content.
        :return: Nothing.
        """
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

    def post_process(self):
        """
        Finalise the data ready for output.

        Ensures that the data is sorted by the specified field in ascending order.
        We assume that this will be a date field that's in a format for easy sorting.
        More complex requirements will be dealt with as we find them.
        :return: Nothing
        """
        for key in self._output:
            with StringIO(self._output[key]) as buf:
                reader = csv.reader(buf, delimiter=self._config[key]['separator'])
                sorted_data = sorted(reader, key=operator.itemgetter(self._config[key]['sort_field']))

                with StringIO() as out_buf:
                    writer = csv.writer(out_buf, delimiter=self._config[key]['separator'])
                    writer.writerows(sorted_data)
                    self._output[key] = out_buf.getvalue()

    def write_output(self, folder):
        """
        Write all outputs to the specified folder.
        The filename for each output will be <output_key>_<date>, where the date is that stored in the constructor.
        :param folder: The output folder.
        :return: Nothing.
        """

        date_string = self._date.strftime('%Y%m%d%H%M%S')

        for key in self._output:
            filename = f'{key}_{date_string}'
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
