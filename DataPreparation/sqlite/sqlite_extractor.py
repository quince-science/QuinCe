import sys, os
import toml
import pandas as pd
import numpy as np

from DatabaseExtractor import DatabaseExtractor

def check_output_config(config):
  """ Check that the configuration is valid"""
  ok = True

  if config['output']['sort_column'] not in config['output']['columns']:
    print("Sort column not in output columns list")
    ok = False

  return ok


##########################################################

config = None
sqlite_file = None
out_file = None

try:
  config_file = sys.argv[1]
  with open(config_file, "r") as config_chan:
    config = toml.loads(config_chan.read())

  sqlite_file = sys.argv[2]
  out_file = sys.argv[3]
except IndexError:
  print("Usage: sqlite_extractor.py [config_file] [sqlite_file] [output_file]")
  exit()

# Check configuration. Error messages printed in check function
if not check_output_config(config):
  exit()


extractor = None
try:
  # Initialise the database connection and check database config
  extractor = DatabaseExtractor(sqlite_file, config)

  # Extract all tables
  all_datasets = []
  for table in config['input']['tables']:
    all_datasets.append(extractor.get_dataset(table['name']))

  # Join and sort datasets
  all_data = pd.concat(all_datasets)
  all_data.sort_values(by=config['output']['sort_column'])

  # Replace missing values
  all_data.fillna(value=config['output']['empty_col_value'], inplace=True)

  # Perform all mappings
  if 'mappings' in config['column_mapping']:
    for col_map in config['column_mapping']['mappings']:

      mapped_values = []

      all_data[col_map['column']] = all_data[col_map['column']].astype(str)

      for source, dest in col_map['mapping']:
        all_data[col_map['column']].replace(source, dest, inplace=True)
        mapped_values.append(dest)

      column_index = all_data.columns.get_loc(col_map['column'])

      for i in range(0, len(all_data[col_map['column']])):
        if all_data.iloc[i, column_index] not in mapped_values:
          all_data.iloc[i, column_index] = col_map['other']

  # Sort the final dataframe
  all_data.sort_values(by=['TimeStamp'], inplace=True)

  # Write the final CSV
  all_data.to_csv(out_file, index=False)

finally:
  if extractor is not None:
    del extractor