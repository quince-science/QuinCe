import sys, os
import toml

# Local modules
from DatabaseExtractor import DatabaseExtractor

def check_output_config(config):
  result = True

  # Output columns
  try:
    out_cols = config["output"]["columns"]
    if (type(out_cols) is not list):
      print("Output columns must be a list/array")
      result = False
    
    if result:
      if (len(out_cols) == 0):
        print("No output columns specified")
        result = False
  except KeyError:
    print("Output columns not specified in config")
    result = False

  # Empty column value
  if result:
    try:
      empty_value = config["output"]["empty_col_value"]
    except KeyError:
      print("Empty column value not specified")
      result = False

  return result

#########################################

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

if not check_output_config(config):
  exit()

out_chan = open(out_file, "w")
for i in range(0, len(config["output"]["columns"])):
  out_chan.write(config["output"]["columns"][i])

  if i < (len(config["output"]["columns"]) - 1):
    out_chan.write(",")

out_chan.write("\n")

extractor = DatabaseExtractor(sqlite_file, config)

table_id = extractor.get_next_row_table()
while table_id is not None:

  row = extractor.get_mapped_row(table_id)
  for i in range(0, len(row)):
    value = row[i]
    if value is None:
      out_chan.write(str(config["output"]["empty_col_value"]))
    else:
      out_chan.write(str(row[i]))
    
    if i < (len(row) - 1):
      out_chan.write(",")
  
  out_chan.write("\n")

  extractor.load_next_row(table_id)
  table_id = extractor.get_next_row_table()

out_chan.close()
extractor.disconnect()
