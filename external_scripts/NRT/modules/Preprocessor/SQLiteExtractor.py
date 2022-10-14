import os
import sqlite3
import logging
import pandas as pd
import uuid
import tempfile
import toml
import re

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
                table_dataset = self._get_dataset(db_conn, extractor_config, table['name'])
                if not table_dataset.empty:
                    all_datasets.append(table_dataset)

            # Join and sort datasets
            merged_data = all_datasets[0]

            if len(all_datasets) > 1:
                for i in range(1, len(all_datasets)):
                    merged_data = merged_data.merge(all_datasets[i], how='outer', suffixes=(None, f'___{i}'),
                                                    on=extractor_config['output']['timestamp_column'])\
                        .fillna(value=extractor_config['output']['empty_col_value'])

            # Merge columns from multiple tables with same names.
            # NB Clashing values will be ignored - a correct configuration should not produce any.
            #    Yes, I'm being lazy.
            for col in merged_data.columns:
                repeat_col = re.match('(.*)___[0-9]+$', col)
                if repeat_col is not None:
                    target_col = repeat_col.group(1)
                    merged_data[target_col] = merged_data[target_col].combine_first(merged_data[col])
                    merged_data.drop(col, axis=1, inplace=True)

            # Perform all mappings
            if 'column_mapping' in extractor_config:
                for col_map in extractor_config['column_mapping']['mappings']:

                    mapped_values = []

                    merged_data[col_map['column']] = merged_data[col_map['column']].astype(str)

                    for map_from, map_to in col_map['mapping']:
                        merged_data[col_map['column']].replace(map_from, map_to, inplace=True)
                        mapped_values.append(map_to)

                    column_index = merged_data.columns.get_loc(col_map['column'])

                    for i in range(0, len(merged_data[col_map['column']])):
                        if merged_data.iloc[i, column_index] not in mapped_values:
                            merged_data.iloc[i, column_index] = col_map['other']

            # Replace missing values
            #merged_data.fillna(value=extractor_config['output']['empty_col_value'], inplace=True)

            # Sort by time
            merged_data.sort_values(by=extractor_config['output']['timestamp_column'], inplace=True)

            # Get data as CSV
            date_format = '%Y-%m-%dT%H:%M:%SZ'
            if 'timestamp_format' in extractor_config['output']:
                date_format = extractor_config['output']['timestamp_format']

            result = merged_data.to_csv(None, index=False, date_format=date_format).encode("utf-8")

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

            if extractor_config['output']['timestamp_column'] not in extractor_config['output']['columns']:
                self.logger.log(logging.ERROR, f'Timestamp column not in output columns list')
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

                    selects.append(f"{infield_select} AS '{outfield}'")

                if 'fixed_values' in table:
                    for column, value in table['fixed_values']:
                        selects.append(f"'{value}' AS '{column}'")

                sql += ",".join(selects)
                sql += " FROM " + table_name
                if 'whereclause' in table:
                    sql += " WHERE " + table['whereclause']

                result = pd.read_sql_query(sql, db_conn, dtype=str)

                # Adjust the timestamp format if necessary
                if not result.empty:
                    timestamp_column = extractor_config['output']['timestamp_column']
                    result[timestamp_column] = result.apply(
                        lambda row: SQLiteExtractor.parse_date(row[timestamp_column], table['timestamp_format']),
                        axis=1)

        return result

    @staticmethod
    def parse_date(value, ts_format):
        return pd.to_datetime(value, format=ts_format, utc=True).round(freq='S')
