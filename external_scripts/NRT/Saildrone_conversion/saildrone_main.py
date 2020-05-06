###############################################################################
### THE MAIN SAILDRONE SCRIPT                                               ###
###############################################################################

### Description
# Script uses Saildrone API to download saildrone data, converts it from json
# to csv format, and shares it with QuinCe.


###----------------------------------------------------------------------------
### Import packages
###----------------------------------------------------------------------------

import os
import saildrone_module as saildrone
from datetime import datetime
import shutil
import json
import pandas as pd


###----------------------------------------------------------------------------
### Handling directories
###----------------------------------------------------------------------------

# Store path to the main script directory
script_dir = os.path.dirname(os.path.realpath(__file__))

# Create a main data directory if it does not already exist
if not os.path.isdir('./data_files'):
	os.mkdir(os.path.join(script_dir,'data_files'))

# Store path to the main data directory
main_data_dir = os.path.join(script_dir,'data_files')

# Create new data directry with current timestamp as name
now = datetime.now()
dt_string = now.strftime("%Y%m%dT%H%M%S")
data_dir = os.path.join(main_data_dir, str(dt_string))
os.mkdir(data_dir)


###----------------------------------------------------------------------------
### Extract information from the config and stored_info files
###----------------------------------------------------------------------------

try:
	with open ('./config.json') as file:
		configs = json.load(file)
	drones_ignored = configs['drones_ignored']
	datasets = configs['datasets']
	col_order = configs['col_order']
	FTP = configs['FTP']
	quince_instrument_ids = configs['instrument_ids']
except FileNotFoundError:
	# !!! Create config file with keys, no values. Notify via slack to fill inn
	# config values. Temporary solution:
	print("Missing config file")

try:
	with open('./stored_info.json') as file:
		stored_info = json.load(file)
	next_request = stored_info['next_request']
	prev_access_list = stored_info['prev_access_list']
except FileNotFoundError:
	# !!! Create stored info template file. Notify via slack to fill inn values.
	# Temporary solution:
	print("Missing 'stored_info.json' file")


###----------------------------------------------------------------------------
### Find out which data to request
###----------------------------------------------------------------------------

# Create authentication token for saildrone API, and see what's available
token = saildrone.auth(configs['saildrone_api'])
access_list = saildrone.get_available(token)

# If the access list has changed since the previous run (and previous
# access list was not empty): print message and replace the prev_access_list
# with the new access_list.
if prev_access_list != access_list and bool(prev_access_list):
	#!!! Send message to slack. Temp soluion:
	print("Access list has changed")
	stored_info['prev_access_list'] = access_list

# If the access list contains new drones which are not on the ignore list, add
# them to the next_request dictionary:
for dictionary in access_list:
	drone = str(dictionary['drone_id'])
	if drone not in drones_ignored and drone not in next_request.keys():
			next_request[drone] = dictionary['start_date']

# Function 'check_next_request' will return what we need to request.
next_request_checked = saildrone.check_next_request(
	next_request, access_list, datasets, drones_ignored)


###----------------------------------------------------------------------------
### Download json, convert to csv, merge datasets and send to Quince
###----------------------------------------------------------------------------

# Create connection to the Quince FTP
ftpconn = saildrone.connect_ftp(FTP)

# Loop that downloads, converts, merges, and sends data files it to the QuinCe
# FTP. Keep track on what to request next time in the next_request_updated.
next_request_updated = dict(next_request_checked)
for drone_id, start_string in next_request_checked.items():

	# Calculate the end date as 7 days after the start date.
	# This prevents any single update being too big.
	# Since this will typically run once per day it's not ususally a problem.
	start_date = datetime.strptime(start_string, "%Y-%m-%dT%H:%M:%S.000Z")
	end_date = start_date + pd.Timedelta("7 days")
	end_string = end_date.strftime("%Y-%m-%dT%H:%M:%S") + ".000Z"

	if not drone_id in quince_instrument_ids:
		raise LookupError('QuinCe instrument ID missing for SailDrone ' + drone_id)

	quince_instrument_id = quince_instrument_ids[drone_id]

	# Download the json files and store their paths
	json_paths =[]
	for dataset in datasets:
		json_path = saildrone.write_json(
			data_dir, drone_id, dataset, start_string, end_string, token)
		json_paths.append(json_path)

	# Convert each json to csv, and store the csv paths
	csv_paths = []
	for path in json_paths :
		csv_path = saildrone.convert_to_csv(path)
		csv_paths.append(csv_path)

	# Create merged dataframe
	merged_df = saildrone.merge_datasets(csv_paths)

	# Add missing columns (with empty data), and sort by the defined column
	# order to ensure consistent data format
	for param in col_order:
		if param not in merged_df.columns:
			merged_df[param] = None
	# !!! Check: If there are new headers in the merged_df.
	# Notify slack and stop script
	merged_sorted_df = merged_df[col_order]

	biogeo_observations = saildrone.extract_biogeo_observations(merged_sorted_df)

	if len(biogeo_observations) > 0:

		# Get the last record we downloaded from the biogeo dataset. This will
		# change once the different SailDrone datasets are no longer merged
		# together. This will be used as the starting point for the next request.
		time_index = biogeo_observations.columns.get_loc('time_interval_biogeFile')
		last_record_date = biogeo_observations.tail(1).iloc[0,time_index]

		# Store the merged data as a csv files in the data directory
		merged_file_name = (str(drone_id) + '_'
			+ start_string[0:4] + start_string[5:7] + start_string[8:10] + 'T'
			+ start_string[11:13] + start_string[14:16] + start_string[17:19] + "-"
			+ last_record_date.strftime('%Y%m%dT%H%M%S') + '.csv')
		merged_path = os.path.join(data_dir, merged_file_name)
		merged_csv = biogeo_observations.to_csv(merged_path,
			index=None, header=True, sep=',')

		# Open the merged csv file in byte format and send to the Quince FTP
		with open(merged_path, 'rb') as file:
			byte = file.read()
			upload_result = saildrone.upload_file(ftpconn=ftpconn,
				ftp_config=FTP, instrument_id=quince_instrument_id,
				filename=merged_file_name, contents=byte)

		#  Set new start date for the next_request:
		next_request_updated[drone_id] = (last_record_date
			+ pd.Timedelta("1 minute")).strftime("%Y-%m-%dT%H:%M:%S.000Z")


	###----------------------------------------------------------------------------
	### Prepare for next download request
	###----------------------------------------------------------------------------

	# Update stored_info file
	stored_info['next_request'] = next_request_updated
	with open('./stored_info.json', 'w') as file:
		json.dump(stored_info, file,
			sort_keys=True, indent=4, separators=(',',': '))

ftpconn.close()
