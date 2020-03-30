###############################################################################
### MERGE CSV FILES                                                         ###
###############################################################################

### Description:
# This function merges csv files. An 'outer full merge' is performed so that
# all values are kept in the merged output file.

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


def merge_datasets(csv_paths):

	count = 1
	for file in csv_paths:

		# Read the file, set the timestamp, and add a header suffix
		df = pd.read_csv(file)
		set_datetime(df)
		file_name = file.split('data_files/',)[1]
		suffix = '_' + file_name.split("_",)[1][0:5] + 'File'
		df = add_header_suffix(df, suffix)

		# For the first file: simply store the data frame and suffix in a new
		# variable so that it can be used in the next iteration
		if count == 1:
			df_previous = df
			suffix_previous = suffix

		# Merge the current data frame with the previous (merged) data frame.
		if count >= 2:
			left = 'time_interval' + suffix_previous
			right = 'time_interval' + suffix
			merged_df = df_previous.merge(df, left_on=left, right_on=right,
				how='outer', sort= True)
			df_previous = merged_df

		count += 1

	return merged_df

def extract_biogeo_observations(all_records):

	biogeo = pd.DataFrame(columns=all_records.columns)

	current_sst = None
	current_salinity = None
	current_air_temp = None

	# Loop through each record of the input.
	# If there's sst/salinity/air temp, keep them
	# If there's CO2 measurements, copy the whole record but add in
	# the last seen sst/sal/airtemp
	for index, row in all_records.iterrows():
		keep_row = False

		# Update cached values if they're present
		if not pd.isnull(row['sbe37_temperature_filtered_oceanFile']):
			current_sst = row['sbe37_temperature_filtered_oceanFile']
			keep_row = True

		if not pd.isnull(row['sbe37_practical_salinity_filtered_oceanFile']):
		  current_salinity = row['sbe37_practical_salinity_filtered_oceanFile']
		  keep_row = True

		if not pd.isnull(row['air_temperature_filtered_atmosFile']):
		  current_air_temp = row['air_temperature_filtered_atmosFile']
		  keep_row = True

    # If we have biogeo data, copy the row and add in the cached values
		if not pd.isnull(row['asvco2_xco2_seawater_dry_biogeFile']):
			row['sbe37_temperature_filtered_oceanFile'] = current_sst
			row['sbe37_practical_salinity_filtered_oceanFile'] = current_salinity
			row['air_temperature_filtered_atmosFile'] = current_air_temp
			keep_row = True

		if keep_row:
			biogeo = biogeo.append(row)

	return biogeo