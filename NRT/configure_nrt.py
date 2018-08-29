import toml
import urllib.request, urllib.error
import base64

#######################################################

with open("config.toml", "r") as config_file:
  config = toml.loads(config_file.read())

print("Retrieving NRT instruments from QuinCe...")

quince_url = config["QuinCe"]["url"]
user = config["QuinCe"]["user"]
password = config["QuinCe"]["password"]

request = urllib.request.Request(quince_url + "/api/nrt/GetInstruments")

auth_string = "%s:%s" % (user, password)
base64_auth_string = base64.standard_b64encode(auth_string.encode("utf-8"))


request.add_header("Authorization", "Basic %s" % base64_auth_string.decode("utf-8"))

try:
  conn = urllib.request.urlopen(request)
  print(conn.read())
except urllib.error.HTTPError as e:
  print("%s %s" % (e.code, e.reason))
  quit()

