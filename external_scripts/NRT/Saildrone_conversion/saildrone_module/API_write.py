###############################################################################
### CREATE DICTIONARY FROM REQUEST OUTPUT AND WRITE JSON FILE               ###
###############################################################################

### Description:
# Function to_dict creates a dictionary from the html request output.
# Function 'write_json' creates a json file. It needs the

#------------------------------------------------------------------------------
import json
import urllib
import saildrone_module
import os


def to_dict(request_output):
	byte = urllib.request.urlopen(request_output).read()
	string = byte.decode('utf-8')
	dictionary = json.loads(string)
	return dictionary


def write_json(data_dir, header, drone_id, dataset, start, end, token):

	get_data_url = 'https://developer-mission.saildrone.com/v1/timeseries/'\
	+ f'{drone_id}?data_set={dataset}&interval=1&start_date={start}&end_date='\
	+ f'{end}&order_by=desc&limit=1000&offset=0&token={token}'

	data_request = urllib.request.Request(
		get_data_url, headers=header, method='GET')

	data_dict = saildrone_module.to_dict(data_request)  # Does this work?

	output_file_name = str(drone_id) + '_' + dataset + '.json'
	output_file_path = os.path.join(data_dir, output_file_name)

	with open(output_file_path, 'w') as outfile:
		json.dump(data_dict, outfile,
			sort_keys=True, indent=4, separators=(',',': '))

	return output_file_path