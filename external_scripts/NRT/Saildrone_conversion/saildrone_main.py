###############################################################################
### MANAGE SAILDRONE DATA 						                            ###
###############################################################################

### Description
# Script uses Saildrone API to get saildrone data, converts it from json to csv
# format, and shares it with QuinCe.


#------------------------------------------------------------------------------
### Import packages
import os
import saildrone_module
from datetime import datetime
import shutil


#------------------------------------------------------------------------------
### Handling directories

# Store path to the main script directory
script_dir = os.path.dirname(os.path.realpath(__file__))

# Create a data directory if it does not already exist
# (Commented out until API part of script works)
#if not os.path.isdir('./data_files'):
#	os.mkdir(os.path.join(script_dir,'data_files'))

# Store path to the data directory
data_dir = os.path.join(script_dir,'data_files')

# Create new archive directry with current timestamp as name
now = datetime.now()
dt_string = now.strftime("%Y%m%dT%H%M%S")
new_archive_path = data_dir + '\\' + str(dt_string)
os.mkdir(new_archive_path)


#------------------------------------------------------------------------------
### API

# ... Ask for oceanpgraphy and biogeo data file and store in the 'data_files'
# directory.


#------------------------------------------------------------------------------
### Convert to csv format

# Extract name of content in the data directory
filenames = os.listdir(data_dir)

# Run each json file through the function which converts to csv. Then move the
# json file to the archive folder.
for file in filenames:
	if '.json' in file:
		json_file_path = os.path.join(data_dir, file)
		saildrone_module.convert_to_csv(json_file_path)
		shutil.move(json_file_path, os.path.join(new_archive_path, file))


#------------------------------------------------------------------------------
### Merge biogeo and ocean csv file


# Store csv file paths.
# Change this when know how data is received from the API.
ocean_csv_path = os.path.join(data_dir, '1030_oceanographic.csv')
biogeo_csv_path = os.path.join(data_dir, '1030_biogeo.csv')

# Call function in the saildrone module which merges the ocean and biogeo file.
saildrone_module.merge_ocean_bio(ocean_csv_path, biogeo_csv_path)

# Move the original ocean and biogeo csv files to the archive folder.
