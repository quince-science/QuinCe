import urllib.request, urllib.error
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
  instruments = conn.read()
  conn.close()

  return json.loads(instruments)
