import sys, os
import toml

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

