###############################################################################
### MERGE OCEAN AND BIOGEO FILE                                                    ###
###############################################################################

### Description:
# This function merges the ocean and biogeo file. Outer full merge so that all
# values are kept in the merged output file.

#------------------------------------------------------------------------------
import pandas as pd


def add_header_suffix(df, string):
	new_header = []
	for header in list(df.columns):
		header_edited = header + string
		new_header.append(header_edited)
	df.columns = new_header
	return df


def set_datetime(df):
	df.loc[:,'time_interval'] = pd.to_datetime(
		df.loc[:,'time_interval'], format = '%Y-%m-%dT%H:%M:%S.%fZ')


# This function need to change!! Will merge lines with different time together.
def merge_ocean_biogeo(ocean_path, biogeo_path, atmos_path):

	ocean_df = pd.read_csv(ocean_path)
	biogeo_df = pd.read_csv(biogeo_path)
	atmos_df = pd.read_csv(atmos_path)

	set_datetime(ocean_df)
	set_datetime(biogeo_df)
	set_datetime(atmos_df)

	ocean_df = add_header_suffix(ocean_df, '_oceanFile')
	biogeo_df = add_header_suffix(biogeo_df, '_biogeoFile')
	atmos_df = add_header_suffix(atmos_df, '_atmosFile')

	initial_merge = biogeo_df.merge(ocean_df,
		left_on='time_interval_biogeoFile',
		right_on='time_interval_oceanFile',
		how='outer', sort= True)

	merged_df = initial_merge.merge(atmos_df,
		left_on = 'time_interval_biogeoFile',
		right_on='time_interval_atmosFile',
		how='outer', sort= True)

	return merged_df










