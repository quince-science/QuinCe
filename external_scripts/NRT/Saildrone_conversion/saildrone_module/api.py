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
from datetime import datetime

our_header = {'Content-Type':'application/json', 'Accept':'application/json'}


def to_dict(request_output):
	byte = urllib.request.urlopen(request_output).read()
	string = byte.decode('utf-8')
	dictionary = json.loads(string)
	return dictionary



def auth():
	auth_url = 'https://developer-mission.saildrone.com/v1/auth'
	#our_header = {'Content-Type':'application/json', 'Accept':'application/json'}
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



def check_next_request(next_request, access_list, datasets, drones_ignored):
	next_request_checked = dict(next_request)
	for drone, start in next_request.items():

		# Remove drone from the requst list if it is on the ignore list
		if drone in drones_ignored:
			del next_request_checked[drone]
			continue

		# Find what's available for the drone in question
		available = [dictionary for dictionary in access_list
			if str(dictionary['drone_id']) == drone]

		# Remove drone from next request if it is no longer available
		if not available:
			# !!! Send message to slack. Temp solution:
			print("Drone ", drone," no longer available.")
			del next_request_checked[drone]
			continue

		# Check if datasets we want are available
		# (try-except in order to use continue for the main for loop)
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

	# Since we can only receive 1000 records per download request we need to
	# keep requesting until we do not receive any data.
	more_to_request = True
	count = 1
	data_list_concat = []
	while (more_to_request is True):

		# Define the request URL
		get_data_url = 'https://developer-mission.saildrone.com/v1/timeseries/'\
		+ f'{drone_id}?data_set={dataset}&interval=1&start_date={start}&end_date='\
		+ f'{end}&order_by=asc&limit=1000&offset=0&token={token}'

		# Send request
		data_request = urllib.request.Request(
			get_data_url, headers=our_header, method='GET')

		# Store output from request in dictionary
		data_dict = saildrone.to_dict(data_request)

		# If request is empty: stop sending requests. If not, add the new data
		# to the concatenated data list. And add one second to the last record
		# recieved (used as start date for the next request).
		if len(data_dict['data']) == 0:
			more_to_request = False
		else:
			data_list_concat = data_list_concat + data_dict['data']

			last_dict = data_dict['data'][-1:]
			last_record = datetime.strptime(last_dict[0]['time_interval'],
			 "%Y-%m-%dT%H:%M:%S.000Z")
			start = (last_record +
				pd.Timedelta("1 minute")).strftime("%Y-%m-%dT%H:%M:%S.000Z")

		count += 1


	# Replace the data section of the last json file received with the
	# concatenated data list.
	data_dict['data'] = data_list_concat

	# Write the dictionary to a json file
	output_file_name = str(drone_id) + '_' + dataset + '.json'
	output_file_path = os.path.join(data_dir, output_file_name)
	with open(output_file_path, 'w') as outfile:
		json.dump(data_dict, outfile,
			sort_keys=True, indent=4, separators=(',',': '))

	return output_file_path