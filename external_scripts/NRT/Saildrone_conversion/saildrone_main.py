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

# Create a data directory if it does not already exist  # Change to try except!
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
# drone items are removed if any of the following is not available: the drone
# itself, any of the dataset typs, or the start date.
next_request_checked = saildrone.check_next_request(
	next_request, access_list, datasets)

#-----
## Required inputs for the download request function will be:
# - 'data_dir' and 'token', already defined with the same name
# - 'end' is always current timestamp:
end = now.strftime("%Y-%m-%dT%H:%M:%S") + ".000Z"
# - 'dataset' is defined in the 'datasets' list
# - 'drone_id' and 'start' are defined in the 'next_request_checked' dictionary
#-----


###----------------------------------------------------------------------------
### Download data
###----------------------------------------------------------------------------


# !!! Get 500 error when try to download all at once)
# !!! Add try except! urllib.error module
# !!! Find a way to download so that get the whole period I request, and
# !!! not just the final 1000 measurements




# !!! Change code
#for drone in access_list:
#	drone_id = drone['drone_id']
#	start = drone['start_date']
#	end = drone['end_date']

#	datasets = ['oceanographic', 'biogeochemical']
#	json_paths = []
#	for dataset in datasets:
#		json_path = saildrone.write_json(
#			data_dir, drone_id, dataset, start, end, token)
#		json_paths.append(json_path)



# !!! WHILTE TESTING!!! Download individual datasets
#drone_id = 1053
#dataset = 'atmospheric'
#dataset = 'oceanographic'
#dataset = 'biogeochemical'
#start = '2019-12-18T19:00:00.000Z'
#end = now.strftime("%Y-%m-%dT%H:%M:%S") + ".000Z"

#saildrone.write_json(data_dir, drone_id, dataset, start, end, token)


# !!! WHILE TESTING:
# The loop above will download ocean and biogeo file from one drone. It will
# also create 'json_paths'. While testing, create this manually.
#ocean_path = os.path.join(data_dir,'1053_oceanographic.json')
#biogeo_path = os.path.join(data_dir, '1053_biogeochemical.json')
#atmos_path = os.path.join(data_dir, '1053_atmospheric.json')
#json_paths = [ocean_path, biogeo_path, atmos_path]
#drone_id = 1053

###----------------------------------------------------------------------------
### Convert to csv format and merge ocean and bio file
###----------------------------------------------------------------------------

# !!! Add one indent to this section when the download loop is not a comment.

# Convert each json to csv file. Move the json file to the archive
# folder. Store the new csv paths.
#csv_paths = []
#for path in json_paths :
#	csv_path = saildrone.convert_to_csv(path)
#	csv_paths.append(csv_path)
#	shutil.move(path, os.path.join(archive_path, os.path.basename(path)))

# !!! Need to update merge script so that co2 and sst are on the same row,
# !!! even with different times. ON HOLD: STEVE WILL DO THIS FIRST

# Merge if ocean, bio and atmos was downloaded (order is always ocean, bio,
# atmos in the csv_path variable.). Sort the columns of the merged dataset
# using the col_order (previously extracted from the config file). Export the
# merged data to a csv file. Move the original csv files to archive.
#if len(csv_paths) == 3:
#	merged_df = saildrone.merge_ocean_biogeo(
#		csv_paths[0], csv_paths[1], csv_paths[2])

	# Add columns that might be missing:
#	for param in col_order:
#		if param not in merged_df.columns:
#			merged_df[param] = None

	# !!! Add check:
	# If there are new headers in the merged_df. Notify slack.

	# Sort by the column order given in config file
#	merged_sorted_df = merged_df[col_order]

#	merged_path = os.path.join(data_dir, str(drone_id) + '_merged.csv')
#	merged_csv = merged_sorted_df.to_csv(merged_path, index=None, header=True, sep=',')

#	for path in csv_paths:
#		shutil.move(path, os.path.join(archive_path, os.path.basename(path)))


# !!! Update stored_info.json 'next_request' parameter based on what was
# succesfully requested.
# Example how to write to json:
#next_request = ....
#with open('./stored_info.json', 'w') as file:
#	json.dump(stored_info, file,
#		sort_keys=True, indent=4, separators=(',',': '))


###----------------------------------------------------------------------------
### Send to QuinCe
###----------------------------------------------------------------------------

# All files in the data_folder are ready for export to QuinCe. Do quince
# want unmerged?