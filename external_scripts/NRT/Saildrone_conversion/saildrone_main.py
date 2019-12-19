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


# Download requests requires inputs which we extract from the config file
try:
	with open ('./config.json') as file:
		configs = json.load(file)
	drones = configs['drones']
	datasets = configs['datasets']
	start_dates = configs['next_start']
except FileNotFoundError:
	# Create config file with keys, no values.
	# Notify via slack to fill inn config values.
	# Temporary:
	print("Missing config file")


# TODO:
# Add col_order to config file (see pdf)
# Add a check to see if there are new drones in access list. If they are not
# on the ignore list, add them to the next_start list.


# !!! CHANGE to use config file!!!
# Get the start dates to use in request:
#try:
#	with open('./next_start.json') as file:
#		next_start = json.load(file)
#except FileNotFoundError:
#	next_start = {}
#	for drone in access_list:
#		drone_id = str(drone['drone_id'])
#		start_date = drone['start_date']
#		next_start[drone_id] = start_date

end = now.strftime("%Y-%m-%dT%H:%M:%S") + ".000Z"

#-----------------
# Download data

# !!! While testing:
#saildrone.write_json(data_dir, drone_id, dataset, start, end, token)



# !!! Change code below to use new inputs, e.g. from next_request.json
# !!! Get 500 error when try to download all at once)
# !!! Add try except! urllib.error module
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


# !!! WHILE TESTING:
# The loop above will download ocean and biogeo file from one drone. It will
# also create 'json_paths'. While testing, create this manually.
#ocean_path = os.path.join(data_dir,'1021_oceanographic.json')
#biogeo_path = os.path.join(data_dir, '1021_biogeochemical.json')
#json_paths = [ocean_path, biogeo_path]
#drone_id = 1021


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
# !!! even with different times.
# !!! Also need to sort cols so that always the same order.
# Merge if ocean and bio was downloaded (ocean path is then always first).
# Export the merged data to a csv file. Move the ocean and biogeo file to
# archive.
#if len(csv_paths) == 2:
#	merged_df = saildrone.merge_ocean_biogeo(csv_paths[0], csv_paths[1])

#	merged_path = os.path.join(data_dir, str(drone_id) + '_merged.csv')
#	merged_csv = merged_df.to_csv(
#		merged_path, index=None, header=True, sep=',')

#	for path in csv_paths:
#		shutil.move(path, os.path.join(archive_path, os.path.basename(path)))

#   !!! Update next requests json file !!!
# !!! Move to bottom
#with open('./next_start.json', 'w') as file:
#	json.dump(next_start, file,
#		sort_keys=True, indent=4, separators=(',',': '))




###----------------------------------------------------------------------------
### Send to QuinCe
###----------------------------------------------------------------------------

# All files in the data_folder are ready for export to QuinCe. Do quince
# want unmerged?