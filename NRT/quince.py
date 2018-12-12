import urllib.request, urllib.error
import requests
import base64
import json

# Retrieve the NRT instruments registered in QuinCe
def get_instruments(config):
  quince_url = config["QuinCe"]["url"]
  user = config["QuinCe"]["user"]
  password = config["QuinCe"]["password"]

  request = urllib.request.Request(quince_url + "/api/nrt/GetInstruments")

  auth_string = "%s:%s" % (user, password)
  base64_auth_string = base64.standard_b64encode(auth_string.encode("utf-8"))
  request.add_header("Authorization", "Basic %s" % base64_auth_string.decode("utf-8"))

  conn = urllib.request.urlopen(request)
  instruments = conn.read().decode("utf-8")
  conn.close()

  return json.loads(instruments)

# See if an instrument ID exists in QuinCe. Compares to a previously
# obtained output from get_instruments
def is_quince_instrument(instrument_id, quince_instruments):
  result = False

  for instrument in quince_instruments:
    if str(instrument["id"]) == str(instrument_id):
      result = True
      break

  return result

# Upload a file to QuinCe
def upload_file(config, instrument_id, filename, contents):
  quince_url = config["QuinCe"]["url"]
  user = config["QuinCe"]["user"]
  password = config["QuinCe"]["password"]

  url = quince_url + "/api/nrt/UploadFile"

  files = {"file" : (filename, contents)}
  params = {"instrument" : instrument_id}
  response = requests.post(url, data=params, files=files, auth=(user, password))
  return response.status_code
