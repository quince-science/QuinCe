###############################################################################
### CONVERT JSON TO CSV                                                     ###
###############################################################################

### Description:
# This function converts a json file from saildrone to a csv file.

#------------------------------------------------------------------------------
import pandas as pd


def merge_ocean_bio(ocean_path, biogeo_path):

	# Read the csv files
	ocean_df = pd.read_csv(ocean_path)
	biogeo_df = pd.read_csv(biogeo_path)

	print(ocean_df.head(4))
	print(biogeo_df.head(4))