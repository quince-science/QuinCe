import os
from modules.Preprocessor import Preprocessor
from modules.Preprocessor.PreprocessorError import PreprocessorError

class FixNewlinesPreprocessor(Preprocessor.Preprocessor):
    """
    Detect and fix lines that have been split into two.
    """
    
    __VALID_SEPARATORS__ = ['SPACE', 'TAB', ';', ',']
    
    def __init__(self, logger, configuration):
        super().__init__(logger, configuration)

    @staticmethod
    def get_type():
        return 'Newline fixer'

    @staticmethod
    def _get_config_entries():
        return ['Separator', 'Column count']

    def test_configuration(self):
        config_ok = True

        separator = self._configuration['Separator']

        if separator is None:
            config_ok = False
        else:
            if separator not in self.__VALID_SEPARATORS__:
                print('Invalid separator')
                config_ok = False

        column_count = self._configuration['Column count']
        if column_count is not None and len(column_count) > 0:
            try:
                column_count = int(column_count)
                if column_count < 1:
                    print('Column count must be positive')
                    config_ok = False
            except ValueError:
                print('Column count must be an integer')
                config_ok = False

        return config_ok

    def preprocess(self, data):
        separator = self._get_separator()
        file_column_count = self._configuration['Column count']

        output = ''
        self._lines = data.read().decode('utf-8').split('\n')
        self._pos = -1

        if len(self._lines) > 0:

            if file_column_count is None:
                header_line = self._get_line()
                file_column_count = len(header_line.split(separator))
                output += header_line


            line = self._get_line()
            while line is not None:
                fields = line.split(separator)

                if len(fields) < file_column_count:
                    next_line = self._get_line()

                    if next_line is None:
                        output += line
                        line = next_line
                    else:
                        next_fields = next_line.split(separator)
                        next_col_count = len(next_line)

                        if len(fields) + len(next_fields) == file_column_count:
                            fields.extend(next_line)
                            output += separator.join(fields)
                            line = self._get_line()
                        else:
                            output += line
                            line = next_line
                else:
                    output += line
                    line = self._get_line()

        return output.encode('utf-8')


    def _get_separator(self):
        separator = self._configuration['Separator']
        if separator == 'SPACE':
            separator = ' '
        elif separator == 'TAB':
            separator = '\t'
        
        return separator

    def _get_line(self):
        self._pos += 1
        return None if self._pos >= len(self._lines) else self._lines[self._pos]



    @staticmethod
    def _get_config_instructions():
        return """
Separator must be one of 'SPACE' 'TAB' ';' ',' (literal strings).
Column count is optional. If blank, will use the first line of the file to determine the column count.
        """
