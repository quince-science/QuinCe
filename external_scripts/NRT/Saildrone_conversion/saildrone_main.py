###############################################################################
### SAILDRONE DATA RETRIEVAL AND CONVERSION		                            ###
###############################################################################

### Description
# Script uses Saildrone API to download saildrone ocean and biogeo data,
# converts it from json to csv format, and shares it with QuinCe.


#------------------------------------------------------------------------------
### Import packages
import os
import saildrone_module as saildrone
from datetime import datetime
import shutil
import urllib
import json
import ast
import pandas as pd


###----------------------------------------------------------------------------
### Handling directories
###----------------------------------------------------------------------------

# Store path to the main script directory
script_dir = os.path.dirname(os.path.realpath(__file__))

# Create a data directory if it does not already exist
if not os.path.isdir('./data_files'):
	os.mkdir(os.path.join(script_dir,'data_files'))

# Store path to the data directory
data_dir = os.path.join(script_dir,'data_files')

# Create new archive directry with current timestamp as name
# !!! Can remove this when everything works
now = datetime.now()
dt_string = now.strftime("%Y%m%dT%H%M%S")
archive_path = os.path.join(data_dir, str(dt_string))
os.mkdir(archive_path)


###----------------------------------------------------------------------------
### Find out which data to request
###----------------------------------------------------------------------------

# Create authentication token for saildrone API, and see what's available
token = saildrone.auth()
access_list = saildrone.get_available(token)

# Import information from config file and from the stored_info file.
try:
	with open ('./config.json') as file:
		configs = json.load(file)
	drones_ignored = configs['drones_ignored']
	datasets = configs['datasets']
	col_order = configs['col_order']
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

# Check if the access list has changed since the previous run (and previous
# access list was not empty): send message and replace the prev_access_list
# with the new access_list.
if prev_access_list != access_list and bool(prev_access_list):
	#!!! Send message to slack. Temp soluion:
	print("Access_list has changed")
	stored_info['prev_access_list'] = access_list

# If the access list contains new drones which are not on ignore list, add
# them to the next_request dictionary:
for dictionary in access_list:
	drone = str(dictionary['drone_id'])
	if drone not in drones_ignored and drone not in next_request.keys():
			next_request[drone] = dictionary['start_date']

# Function 'check_next_request' will return a list of next requests where
# drone items are removed if they are on the ignore list, OR if any of the
# following is not available: the drone itself, any of the dataset typs, or the
# start date.
next_request_checked = saildrone.check_next_request(
	next_request, access_list, datasets, drones_ignored)


###----------------------------------------------------------------------------
### Download json, convert to csv, and merge datasets.
###----------------------------------------------------------------------------

# The end date for download request are always the current time stamp
end = now.strftime("%Y-%m-%dT%H:%M:%S") + ".000Z"


# Loop that downloads, converts, merges and writes datafiles. Keep track on
# next start requests in next_request_updated.
next_request_updated = dict(next_request_checked)
for drone_id, start in next_request_checked.items():

	# Download the json files and store their paths
	json_paths =[]
	for dataset in datasets:
		json_path = saildrone.write_json(
			data_dir, drone_id, dataset, start, end, token)
		json_paths.append(json_path)


	# Convert each json to csv. Move the json file to the archive folder.
	# Store the new csv paths.
	csv_paths = []
	for path in json_paths :
		csv_path = saildrone.convert_to_csv(path)
		csv_paths.append(csv_path)
		shutil.move(path, os.path.join(archive_path, os.path.basename(path)))


	# Create merged dataframe
	if len(csv_paths) > 1:
		merged_df = saildrone.merge_datasets(csv_paths)

		# Add missing columns to ensure consistent data format:
		for param in col_order:
			if param not in merged_df.columns:
				merged_df[param] = None

		# !!! Check: If there are new headers in the merged_df.
		# Notify slack and stop script

		# Sort by the defined column order from the config file
		merged_sorted_df = merged_df[col_order]

		# Get the last record we downloaded from the biogeo dataset. This will
		# change once the different SailDrone datasets are no longer merged
		# together. This will be used as the starting point for the next
		# request.
		col_index = merged_sorted_df.columns.get_loc('time_interval_biogeFile')
		last_record_date = merged_sorted_df.tail(1).iloc[0,col_index]

		# Export the merged data to a csv file
		merged_path = os.path.join(data_dir, str(drone_id) + '_'
			+ start[0:4] + start[5:7] + start[8:10] + 'T' + start[11:13]
			+ start[14:16] + start[17:19] + "-"
			+ last_record_date.strftime('%Y%m%dT%H%M%S') + '.csv')
		merged_csv = merged_sorted_df.to_csv(merged_path,
			index=None, header=True, sep=',')

		# Move the individual csv files to archive.
		for path in csv_paths:
			shutil.move(path, os.path.join(archive_path,
			os.path.basename(path)))

		#  Set new start date for the next_request:
		next_request_updated[drone_id] = (last_record_date
			+ pd.Timedelta("1 minute")).strftime("%Y-%m-%dT%H:%M:%S.000Z")


# Update stored_info file
stored_info['next_request'] = next_request_updated
with open('./stored_info.json', 'w') as file:
	json.dump(stored_info, file,
		sort_keys=True, indent=4, separators=(',',': '))


###----------------------------------------------------------------------------
### Send to QuinCe
###----------------------------------------------------------------------------

# All files in the data_folder are ready for export to QuinCe. Do quince
# want unmerged?