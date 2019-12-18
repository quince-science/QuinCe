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


def auth():
	auth_url = 'https://developer-mission.saildrone.com/v1/auth'
	our_header = {'Content-Type':'application/json', 'Accept':'application/json'}
	our_data = json.dumps({'key':'XbXS9f7TfZepb7nD',
		'secret':'dGL99eMuBcsJYm5guq29AtKeCGHCT2kP'}).encode()

	auth_request = urllib.request.Request(
		url=auth_url, headers=our_header,
		data=our_data, method='POST')

	auth_response_dict = saildrone_module.to_dict(auth_request)

	token = auth_response_dict['token']
	return token


def check_available(token):
	check_available_url = 'https://developer-mission.saildrone.com/v1/auth/'\
	+ 'access?token=' + token

	check_available_request = urllib.request.Request(
		check_available_url, method='GET')

	available_dict = saildrone_module.to_dict(check_available_request)

	data = available_dict['data']
	access_list = data['access']
	return access_list


def write_json(data_dir, header, drone_id, dataset, start, end, token):

	get_data_url = 'https://developer-mission.saildrone.com/v1/timeseries/'\
	+ f'{drone_id}?data_set={dataset}&interval=1&start_date={start}&end_date='\
	+ f'{end}&order_by=desc&limit=1000&offset=0&token={token}'

	data_request = urllib.request.Request(
		get_data_url, headers=header, method='GET')

	data_dict = saildrone_module.to_dict(data_request)

	output_file_name = str(drone_id) + '_' + dataset + '.json'
	output_file_path = os.path.join(data_dir, output_file_name)

	with open(output_file_path, 'w') as outfile:
		json.dump(data_dict, outfile,
			sort_keys=True, indent=4, separators=(',',': '))

	return output_file_path