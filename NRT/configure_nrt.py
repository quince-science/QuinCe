import toml
import urllib.error
import base64
from tabulate import tabulate
import quince, nrtdb #Local modules

# Extract the list of IDs from a set of instruments
def get_ids(instruments):
  result = []
  for instrument in instruments:
    result.append(instrument["id"])
  return result

def make_instrument_table(instruments, ids):

  table_data = [["ID", "Name", "Owner"]]

  instrument_index = 0
  id_index = 0

  while (id_index < len(ids)):
    current_id = ids[id_index]
    while (instruments[instrument_index]["id"] < current_id):
      instrument_index = instrument_index + 1

    if (instruments[instrument_index]["id"] == current_id):
      instrument = instruments[instrument_index]
      table_data.append([instrument["id"],
                         instrument["name"],
                         instrument["owner"]])

    id_index = id_index + 1

  print(tabulate(table_data, headers="firstrow"))

#######################################################

dbconn = None

try:
  with open("config.toml", "r") as config_file:
    config = toml.loads(config_file.read())

  dbconn = nrtdb.get_db_conn(config["Database"]["location"])

  print("Getting QuinCe instruments...")
  quince_instruments = quince.get_instruments(config)

  print("Getting NRT instruments...")
  nrt_instruments = nrtdb.get_instruments(dbconn)

  # Check that NRT and QuinCe are in sync
  quince_ids = get_ids(quince_instruments)
  nrt_ids = get_ids(nrt_instruments)

  orphaned_ids = list(set(nrt_ids) - set(quince_ids))
  if (len(orphaned_ids) > 0):
    print("The following instruments are no longer in QuinCe and will be removed:\n")
    make_instrument_table(nrt_instruments, orphaned_ids)
    go = input('Enter Y to proceed, or anything else to quit: ')
    if (not go.lower() == "y"):
      quit()
    else:
      nrtdb.delete_instrument(dbconn, orphaned_ids)

except urllib.error.URLError as e:
  print(e)
except urllib.error.HTTPError as e:
  print("%s %s" % (e.code, e.reason))

if dbconn is not None:
  nrtdb.close(dbconn)
