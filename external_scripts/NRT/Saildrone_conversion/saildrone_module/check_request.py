###############################################################################
### FUNCTIONS WHICH CHECKS THE NEXT REQUEST                                 ###
###############################################################################

### Description:
# Function which returns a list of what to request for download. Drones are
# removed from the request list if they are on the ignore list, OR if any of
# the following are not available: the drone itself, any of the dataset typs,
# or the start date.

#------------------------------------------------------------------------------
import pandas as pd


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

		# Check if datasets we want are available (try-except is used here in
		# order to continue from the main for loop)
		try:
			for dataset in datasets:
				if dataset not in available[0]['data_set']:
				# !!! Send message to slack. Temp solution:
					print(dataset, " dataset not available for drone ", drone)
					del next_request_checked[drone]
					raise Exception()
		except Exception:
			continue

		# Check if the start we want to request is available. First convert to
		# dateformat.
		start_request = pd.to_datetime(start, format='%Y-%m-%dT%H:%M:%S.%fZ')
		start_available = pd.to_datetime(available[0]['start_date'],
			format='%Y-%m-%dT%H:%M:%S.%fZ')
		if start_request < start_available:
			#!!! Send message to slack. Temp solution
			print("Next start to request for ", drone, " is not available.")
			del next_request_checked[drone]
			continue

	return next_request_checked
