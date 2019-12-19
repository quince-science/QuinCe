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
import pandas

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
### API
###----------------------------------------------------------------------------


# Create authentication token
token = saildrone.auth()

# See what can be downloaded
access_list = saildrone.check_available(token)


# Get col_order, drones to ignore, and datasets to download from config file
try:
	with open ('./config.json') as file:
		configs = json.load(file)
	drones_ignored = configs['drones_ignored']
	datasets = configs['datasets']
	col_order = configs['col_order']
except FileNotFoundError:
	# Create config file with keys, no values.
	# Notify via slack to fill inn config values.
	# Temporary:
	print("Missing config file")

# Get the next start dates from file
try:
	with open('./next_start.json') as file:
		next_start = json.load(file)
except FileNotFoundError:
	print("Missing 'next_start.json' file")




# TODO:
# Add col_order to config file (see pdf)
# Add a check to see if there are new drones in access list. If they are not
# on the ignore list, add them to the next_start list.


#-----------------
# Download data

# !!! Get 500 error when try to download all at once)

# !!! Add try except! urllib.error module

# !!! Find a way to download so that get the whole period I request, and
# !!! not just the final 1000 measurements


# !!! Change code below to use new inputs, e.g. from next_start.json
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

ocean_path = os.path.join(data_dir,'1053_oceanographic.json')
biogeo_path = os.path.join(data_dir, '1053_biogeochemical.json')
atmos_path = os.path.join(data_dir, '1053_atmospheric.json')
json_paths = [ocean_path, biogeo_path, atmos_path]
drone_id = 1053

###----------------------------------------------------------------------------
### Convert to csv format and merge ocean and bio file
###----------------------------------------------------------------------------

# !!! Add one indent to this section when the download loop is not a comment.

# Convert each json to csv file. Move the json file to the archive
# folder. Store the new csv paths.
csv_paths = []
for path in json_paths :
	csv_path = saildrone.convert_to_csv(path)
	csv_paths.append(csv_path)
#	shutil.move(path, os.path.join(archive_path, os.path.basename(path)))

# !!! Need to update merge script so that co2 and sst are on the same row,
# !!! even with different times. ON HOLD: STEVE WILL DO THIS FIRST

# Merge if ocean, bio and atmos was downloaded (order is always ocean, bio,
# atmos in the csv_path variable.). Sort the columns of the merged dataset
# using the col_order (previously extracted from the config file). Export the
# merged data to a csv file. Move the original csv files to archive.
if len(csv_paths) == 3:
	merged_df = saildrone.merge_ocean_biogeo(
		csv_paths[0], csv_paths[1], csv_paths[2])

	# Add columns that might be missing:
	for param in col_order:
		if param not in merged_df.columns:
			merged_df[param] = None

	# !!! Add check:
	# If there are new headers in the merged_df. Notify slack.

	# Sort by the column order given in config file
	merged_sorted_df = merged_df[col_order]

	merged_path = os.path.join(data_dir, str(drone_id) + '_merged.csv')
	merged_csv = merged_sorted_df.to_csv(merged_path, index=None, header=True, sep=',')

	for path in csv_paths:
		shutil.move(path, os.path.join(archive_path, os.path.basename(path)))


# !!! Update next start json file based on what was succesfully requested
# Example how to write to json:
#next_start = ....
#with open('./next_start.json', 'w') as file:
#	json.dump(next_start, file,
#		sort_keys=True, indent=4, separators=(',',': '))


###----------------------------------------------------------------------------
### Send to QuinCe
###----------------------------------------------------------------------------

# All files in the data_folder are ready for export to QuinCe. Do quince
# want unmerged?