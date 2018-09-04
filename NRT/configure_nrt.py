import toml
import urllib.error
import base64
from tabulate import tabulate

# Local modules
import quince, nrtdb
import retriever_factory

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

  while id_index < len(ids):
    current_id = ids[id_index]
    while instruments[instrument_index]["id"] < current_id:
      instrument_index = instrument_index + 1

    if instruments[instrument_index]["id"] == current_id:
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

  # Remove old instruments from NRT
  orphaned_ids = list(set(nrt_ids) - set(quince_ids))
  if len(orphaned_ids) > 0:
    print("The following instruments are no longer in QuinCe and will be removed:\n")
    make_instrument_table(nrt_instruments, orphaned_ids)
    go = input("Enter Y to proceed, or anything else to quit: ")
    if not go.lower() == "y":
      quit()
    else:
      nrtdb.delete_instruments(dbconn, orphaned_ids)

  # Add new instruments from QuinCe
  new_ids = list(set(quince_ids) - set(nrt_ids))
  if len(new_ids) > 0:
    print("The following instruments are new in QuinCe and will be added:\n")
    make_instrument_table(quince_instruments, new_ids)
    go = input("Enter Y to proceed, or anything else to quit: ")
    if not go.lower() == "y":
      quit()
    else:
      nrtdb.add_instruments(dbconn, quince_instruments, new_ids)

#  unconfigured_instruments = nrtdb.get_unconfigured_instruments(dbconn)
#  for instrument in unconfigured_instruments:
#    print("The following instrument has not been configured:")
#    print("  ID: %d, Name: %s, Owner: %s" % (instrument["id"], instrument["name"], instrument["owner"]))
#    go = input("\nEnter Y to configure it now, or anything else to quit: ")
#    if not go.lower() == "y":
#      quit()
#    else:
#      retriever_type = retriever_factory.ask_retriever_type()
#      if retriever_type is None:
#        nrtdb.store_configuration(dbconn, instrument["id"], None)
#      else:
#        retriever = retriever_factory.get_new_instance(retriever_type)
#        print()
#        retriever.enter_configuration()
#        nrtdb.store_configuration(dbconn, instrument["id"], retriever)

except urllib.error.URLError as e:
  print(e)
except urllib.error.HTTPError as e:
  print("%s %s" % (e.code, e.reason))

if dbconn is not None:
  nrtdb.close(dbconn)
