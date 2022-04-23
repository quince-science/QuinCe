import collections
import os
import sqlite3
import logging
import pandas as pd
import uuid
import tempfile
import toml

from modules.Preprocessor import Preprocessor
from modules.Preprocessor.PreprocessorError import PreprocessorError


class SQLiteExtractor(Preprocessor.Preprocessor):
    def __init__(self, logger, configuration=None):
        super().__init__(logger, configuration)

    @staticmethod
    def get_type():
        return "SQLite Extractor"

    @staticmethod
    def _get_config_entries():
        return ["Extractor Config File"]

    def test_configuration(self):
        # This only tests the existence of the file. The actual config is verified against each passed in database.
        config_ok = True

        if not os.path.exists(self._configuration["Extractor Config File"]):
            config_ok = False
            print("Extractor config file not found")

        return config_ok

    @staticmethod
    def get_processed_filename(filename):
        return f'{filename}.csv'

    # noinspection PyBroadException
    def preprocess(self, data):
        result = None

        # Write the data to a temporary file so SQLite can do its thing
        db_file = os.path.join(tempfile.gettempdir(), str(uuid.uuid4()))

        with open(db_file, 'wb') as f:
            f.write(data.getbuffer())

        # Load configuration
        with open(self._configuration["Extractor Config File"], 'r') as f:
            extractor_config = toml.loads(f.read())

        # Connect to database file
        with sqlite3.connect(db_file) as db_conn:
            # Check configuration against database
            if not self._check_config(db_conn, extractor_config):
                raise PreprocessorError('Extractor configuration invalid')

            # Extract all tables
            all_datasets = []
            for table in extractor_config['input']['tables']:
                all_datasets.append(self._get_dataset(db_conn, extractor_config, table['name']))

            # Join and sort datasets
            all_data = pd.concat(all_datasets)
            all_data.sort_values(by=extractor_config['output']['sort_column'], inplace=True)

            # Replace missing values
            all_data.fillna(value=extractor_config['output']['empty_col_value'], inplace=True)

            # Perform all mappings
            if 'mappings' in extractor_config['column_mapping']:
                for col_map in extractor_config['column_mapping']['mappings']:

                    mapped_values = []

                    all_data[col_map['column']] = all_data[col_map['column']].astype(str)

                    for map_from, map_to in col_map['mapping']:
                        all_data[col_map['column']].replace(map_from, map_to, inplace=True)
                        mapped_values.append(map_to)

                    column_index = all_data.columns.get_loc(col_map['column'])

                    for i in range(0, len(all_data[col_map['column']])):
                        if all_data.iloc[i, column_index] not in mapped_values:
                            all_data.iloc[i, column_index] = col_map['other']

            # Get data as CSV
            result = all_data.to_csv(None, index=False).encode("utf-8")

        # Delete the temporary file
        try:
            os.remove(db_file)
        except Exception:
            # Whatever. It'll get cleaned up eventually
            pass

        # Return data
        return result

    def _check_config(self, db_conn, extractor_config):
        result = True
        output_names = extractor_config['output']['columns']

        for table in extractor_config['input']['tables']:

            # Make sure the mapping input columns are in the table, and the named
            # output columns exist
            c = db_conn.cursor()
            c.execute("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='" + table['name'] + "'")
            if c.fetchone()[0] == 0:
                self.logger.log(logging.ERROR, f'Table {table["name"]} does not exist in database')
                result = False
            c.close()

            # Make sure the specified input columns exist in the table, and the output columns are configured
            c = db_conn.cursor()
            c.execute("select * from " + table['name'] + " limit 1")
            input_names = [description[0] for description in c.description]
            c.close()

            table_outfields = []

            for outfield, infield in table['mapping']:

                table_outfields.append(outfield)
                if outfield not in output_names:
                    self.logger.log(logging.ERROR, f'Output field {outfield} not in configured output columns')
                    result = False

                # Combined input fields are split by "~"
                if infield != "":
                    for sub_infield in infield.split("~"):
                        if sub_infield not in input_names:
                            self.logger.log(logging.ERROR, f'Field {sub_infield} is not in table {table["name"]}')
                            result = False

            # Check that table_outfields and output_names are identical
            if collections.Counter(table_outfields) != collections.Counter(output_names):
                self.logger.log(logging.ERROR, f'Outputs for table {table["name"]} do not match main output config')

            if extractor_config['output']['sort_column'] not in extractor_config['output']['columns']:
                self.logger.log(logging.ERROR, f'Sort column not in output columns list')
                result = False

        return result

    @staticmethod
    def _get_dataset(db_conn, extractor_config, table_name):
        """ Generate a dataset from the specified table """
        result = None

        # Build the SQL
        sql = "SELECT "
        for table in extractor_config['input']['tables']:
            if table['name'] == table_name:
                selects = []
                for outfield, infield in table['mapping']:
                    if infield == "":
                        infield_select = "NULL"
                    else:
                        infield_select = " || '~' || ".join(infield.split("~"))

                    selects.append(infield_select + " AS '" + outfield + "'")

                sql += ",".join(selects)
                sql += " FROM " + table_name
                if 'whereclause' in table:
                    sql += " WHERE " + table['whereclause']

                result = pd.read_sql_query(sql, db_conn)

        return result
