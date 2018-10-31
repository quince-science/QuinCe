
import logging
import toml, json

# Local modules
import retriever_factory, nrtdb

dbconn = None
ftpconn = None

def log_instrument(instrument_id, level, message):
  logging.log(level, str(instrument_id) + ":" + message)

with open("config.toml", "r") as config_file:
  config = toml.loads(config_file.read())

logging.basicConfig(filename="nrt_collector.log",
  format="%(asctime)s:%(levelname)s:%(message)s", level=config["Logging"]["level"])

dbconn = nrtdb.get_db_conn(config["Database"]["location"])

instruments = nrtdb.get_instrument_ids(dbconn)

for instrument_id in instruments:
  log_instrument(instrument_id, logging.DEBUG, "Processing instrument")
  instrument = nrtdb.get_instrument(dbconn, instrument_id)

  if instrument["type"] is None:
    log_instrument(instrument_id, logging.ERROR, "Configuration type not set")
  else:
    retriever = retriever_factory.get_instance(instrument["type"],
      instrument["config"])

    print(retriever)
