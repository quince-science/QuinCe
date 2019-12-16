###############################################################################
### MERGE OCEAN AND BIOGEO FILE                                                    ###
###############################################################################

### Description:
# This function merges the ocean and biogeo file. Outer full merge so that all
# values are kept in the merged output file.

#------------------------------------------------------------------------------
import pandas as pd


def add_header_suffix(df, string):
	new_header = ['time_interval']
	for header in list(df.columns):
		if header != 'time_interval':
			header_edited = header + string
			new_header.append(header_edited)
	df.columns = new_header
	return df


def set_datetime(df):
	df.loc[:,'time_interval'] = pd.to_datetime(
		df.loc[:,'time_interval'], format = '%Y-%m-%dT%H:%M:%S.%fZ')


def merge_ocean_biogeo(ocean_path, biogeo_path):

	ocean_df = pd.read_csv(ocean_path)
	biogeo_df = pd.read_csv(biogeo_path)

	set_datetime(ocean_df)
	set_datetime(biogeo_df)

	ocean_df = add_header_suffix(ocean_df, '_oceanFile')
	biogeo_df = add_header_suffix(biogeo_df, '_biogeoFile')

	merged_df = biogeo_df.merge(
		ocean_df, on='time_interval', how='outer', sort= True)

	return merged_df










