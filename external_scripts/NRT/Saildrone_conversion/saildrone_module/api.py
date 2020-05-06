###############################################################################
### FUNCTIONS WHICH SEND REQUESTS TO THE SAILDRONE API											###
###############################################################################

### Description:
# Several requests are sent to the Saildrone API:
# - request a token (function 'auth')
# - request a list of what we can access (function 'get_available')
# - request to download data (function 'write_json')
# This document contains functions executing such requests.

#------------------------------------------------------------------------------
import json
import urllib.request
from urllib.error import HTTPError
import os
import pandas as pd
from datetime import datetime


REQUEST_HEADER = {'Content-Type':'application/json', 'Accept':'application/json'}


# Function which converts html request output to a dictionary
def to_dict(url):

	response = None

	# urlopen throws an error if the HTTP status is not 200.
	# The error is still a response object, so we can grab it -
	# we need to examine it either way
	try:
		response = urllib.request.urlopen(url)
	except HTTPError as e:
		response = e

	# Get the response body as a dictionary
	dictionary = json.loads(response.read().decode('utf-8'))

	error = False

	if response.status == 400:
		if dictionary['message'] != "Request out of time bound":
			error = True
	elif response.status >= 400:
		error = True

	if error:
		# The response is an error, so we can simply raise it
		raise response

	return dictionary


# Function which returns the token needed for authentication
def auth(authentication):

	# Define the authentication request url
	auth_url = 'https://developer-mission.saildrone.com/v1/auth'

	# Define our data
	our_data = json.dumps({'key':authentication['key'],
		'secret':authentication['secret']}).encode()

	# Send the request
	auth_request = urllib.request.Request(
		url=auth_url, headers=REQUEST_HEADER,
		data=our_data, method='POST')

	# Convert the response to a dictionary. Extract and return the token
	auth_response_dict = to_dict(auth_request)
	token = auth_response_dict['token']
	return token


# Function returning a list of what's available from the Saildrone API
def get_available(token):

	# Define the url for requesting what's available
	check_available_url = 'https://developer-mission.saildrone.com/v1/auth/'\
	+ 'access?token=' + token

	# Send the request
	check_available_request = urllib.request.Request(
		check_available_url, method='GET')

	# Convert the output to a dictionary. Extract and return the access list.
	available_dict = to_dict(check_available_request)
	data = available_dict['data']
	access_list = data['access']
	return access_list


# Function which requests data download. I returns the path to the downloaded
# json file.
def write_json(data_dir, drone_id, dataset, start, end, token):

	# Since we can only receive 1000 records per download request we need to
	# keep requesting (while loop) until we do not receive any data
	more_to_request = True
	data_list_concat = []
	offset = 0
	while (more_to_request is True):

		# Define the download request URL
		get_data_url = 'https://developer-mission.saildrone.com/v1/timeseries/'\
		+ f'{drone_id}?data_set={dataset}&interval=1&start_date={start}&end_date='\
		+ f'{end}&order_by=asc&limit=1000&offset={offset}&token={token}'

		print(get_data_url)

		# Send request
		data_request = urllib.request.Request(
			get_data_url, headers=REQUEST_HEADER, method='GET')

		# Store output from request in dictionary
		data_dict = to_dict(data_request)

		# If request output is empty: stop sending requests. If not, add the
		# new data to the concatenated data list. Add one second to the last
		# record recieved (used as start date for the next request).
		print('Received ' + str(len(data_dict['data'])))
		if len(data_dict['data']) == 0:
			more_to_request = False
		else:
			data_list_concat = data_list_concat + data_dict['data']
			offset = offset + len(data_dict['data'])
			print('Total length ' + str(len(data_list_concat)))


	# Replace the data section of the last json file received with the
	# concatenated data list
	data_dict['data'] = data_list_concat

	# Write the dictionary to a json file
	output_file_name = str(drone_id) + '_' + dataset + '.json'
	output_file_path = os.path.join(data_dir, output_file_name)
	with open(output_file_path, 'w') as outfile:
		json.dump(data_dict, outfile,
			sort_keys=True, indent=4, separators=(',',': '))

	return output_file_path
