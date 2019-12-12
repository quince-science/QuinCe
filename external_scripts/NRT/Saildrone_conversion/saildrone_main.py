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

# Store path to the data directory
data_dir_path = os.path.join(os.path.dirname(os.path.realpath(__file__)),
	'data_files')

# Create new archive directry with current timestamp as name
now = datetime.now()
dt_string = now.strftime("%Y%m%dT%H%M%S")
new_archive_path = data_dir_path + '\\' + 'archive' + '\\' + str(dt_string)
os.mkdir(new_archive_path)

#------------------------------------------------------------------------------
### API

# ... Ask for oceanpgraphy and biogeo data file and store in the 'data_files'
# directory.


#------------------------------------------------------------------------------
### Convert to csv format

# Extract name of content in the data directory (should be "archive" and some
# json files)
filenames = os.listdir(data_dir_path)

# Run each json file through the function which converts to csv. Then move the
# json file to the archive folder.
for file in filenames:
	if '.json' in file:
		data_file_path = data_dir_path + '\\' + file
		saildrone_module.convert_to_csv(data_file_path)
		shutil.move(data_dir_path + '\\' + file, new_archive_path + '\\' + file)

#------------------------------------------------------------------------------
### Merge biogeo and ocean csv file

# This is temporary. Create a beter way to get these csv paths! (This depends
# on how we receive the data from the API)
ocean_csv_path = data_dir_path + '\\' + "1030_oceanographic.csv"
biogeo_csv_path =  data_dir_path + '\\' + "1030_biogeo.csv"

# Call function in the saildrone module which merges the ocean and biogeo file.
saildrone_module.merge_ocean_bio(ocean_csv_path, biogeo_csv_path)


# Move the original ocean and biogeo csv files to the archive folder.


#------------------------------------------------------------------------------
### Send to QuinCe ????










# Extract name of content in the data directory (should be "archive" and some
# csv files)
#filenames = os.listdir(data_dir_path)

# Create the merge input list, which consist of the path to the csv files
#merge_input = []
#for file in filenames:
#	if '.csv' in file:
#		data_file_path = data_dir_path + '\\' + file
#		merge_input.append(data_file_path)
