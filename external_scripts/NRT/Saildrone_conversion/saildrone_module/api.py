###############################################################################
### CREATE DICTIONARY FROM REQUEST OUTPUT AND WRITE JSON FILE               ###
###############################################################################

### Description:
# Function to_dict creates a dictionary from the html request output.
# Function 'write_json' creates a json file. It needs the

#------------------------------------------------------------------------------
import json
import urllib
import saildrone_module as saildrone
import os
import pandas as pd

our_header = {'Content-Type':'application/json', 'Accept':'application/json'}


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

	auth_response_dict = saildrone.to_dict(auth_request)

	token = auth_response_dict['token']
	return token



def get_available(token):
	check_available_url = 'https://developer-mission.saildrone.com/v1/auth/'\
	+ 'access?token=' + token

	check_available_request = urllib.request.Request(
		check_available_url, method='GET')

	available_dict = saildrone.to_dict(check_available_request)

	data = available_dict['data']
	access_list = data['access']
	return access_list



def check_next_request(next_request, access_list, datasets):
	next_request_checked = dict(next_request)
	for drone, start in next_request.items():

		available = [dictionary for dictionary in access_list
			if str(dictionary['drone_id']) == drone]

		# Check if drone itself is available.
		if not available:
			# !!! Send message to slack. Temp solution:
			print("Drone ", drone," no longer available.")
			del next_request_checked[drone]
			continue

		# Check if datasets are available (try-except in order to use continue)
		try:
			for dataset in datasets:
				if dataset not in available[0]['data_set']:
				# !!! Send message to slack. Temp solution:
					print(dataset, " dataset not available for drone ", drone)
					del next_request_checked[drone]
					raise Exception()
		except Exception:
			continue

		# Check if start to request is available. First convert to dateformat
		start_request = pd.to_datetime(start, format='%Y-%m-%dT%H:%M:%S.%fZ')
		start_available = pd.to_datetime(available[0]['start_date'],
			format='%Y-%m-%dT%H:%M:%S.%fZ')
		if start_request < start_available:
			#!!! Send message to slack. Temp solution
			print("Next start to request for ", drone, " is not available.")
			del next_request_checked[drone]
			continue

		return next_request_checked



def write_json(data_dir, drone_id, dataset, start, end, token):

	get_data_url = 'https://developer-mission.saildrone.com/v1/timeseries/'\
	+ f'{drone_id}?data_set={dataset}&interval=1&start_date={start}&end_date='\
	+ f'{end}&order_by=desc&limit=1000&offset=0&token={token}'

	data_request = urllib.request.Request(
		get_data_url, headers=our_header, method='GET')

	data_dict = saildrone.to_dict(data_request)

	output_file_name = str(drone_id) + '_' + dataset + '.json'
	output_file_path = os.path.join(data_dir, output_file_name)

	with open(output_file_path, 'w') as outfile:
		json.dump(data_dict, outfile,
			sort_keys=True, indent=4, separators=(',',': '))

	# !!! Add the first time to the return. This will be used to check if need
	# !!! to continue the request for the given drone and dataset type
	return output_file_path