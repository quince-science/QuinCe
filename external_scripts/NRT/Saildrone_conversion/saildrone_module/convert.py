###############################################################################
### CONVERT JSON TO CSV                                                     ###
###############################################################################

### Description:
# This function converts a json file from saildrone to a csv file.

#------------------------------------------------------------------------------
import csv
import json

def convert_to_csv(file_path):

	# Read the json file and create the 'data_list' variable. This variable
	# contains a list of dictionaries. Each dictionary contains several
	# parameter-value pairs, always including one timestamp parameter-value.
	with open(file_path) as json_file:
		json_loaded = json.load(json_file)
	data_list = json_loaded['data']

	# The parameters can only contain one value per timestamp/dictionary.
	# Must therefore remove parameters containing a list of values.
	data_list_edited = []
	for dictionary in data_list:

		dict_to_keep = {}
		for parameter, value in dictionary.items():
			if type(value) is not list:
				dict_to_keep[parameter] = value

		# Do not keep the dictionary if it only contains one parameter-value
		# pair (which means only a timestamp)
		if len(dict_to_keep) > 1:
			data_list_edited.append(dict_to_keep)

	# Find distinct parameters.
	distinct_parameters = []
	for dictionary in data_list_edited:
		for parameter in dictionary.keys():
			if parameter not in distinct_parameters:
				distinct_parameters.append(parameter)

	# Create output file name and write to the file
	csv_file_path = file_path.replace('.json','') + ".csv"
	with open(csv_file_path, 'w', newline = '') as output_csv:
		output_writer = csv.DictWriter(
			output_csv, fieldnames = distinct_parameters)
		output_writer.writeheader()
		for dictionary in data_list_edited:
			output_writer.writerow(dictionary)

	return csv_file_path
