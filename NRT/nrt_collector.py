
import logging
import toml, json

# Local modules
import RetrieverFactory, nrtdb

dbconn = None
ftpconn = None
logger = None

# Log a message for a specific instrument
def log_instrument(instrument_id, level, message):
  logger.log(level, str(instrument_id) + ":" + message)

# Read in the config
with open("config.toml", "r") as config_file:
  config = toml.loads(config_file.read())

# Set up logging
logging.basicConfig(filename="nrt_collector.log",
  format="%(asctime)s:%(levelname)s:%(message)s")
logger = logging.getLogger('nrt_collector')
logger.setLevel(level=config["Logging"]["level"])

# Connect to NRT database and get instrument list
dbconn = nrtdb.get_db_conn(config["Database"]["location"])
instruments = nrtdb.get_instrument_ids(dbconn)

for instrument_id in instruments:
  log_instrument(instrument_id, logging.INFO, "Processing instrument")
  instrument = nrtdb.get_instrument(dbconn, instrument_id)

  if instrument["type"] is None:
    log_instrument(instrument_id, logging.ERROR, "Configuration type not set")
  else:
    retriever = RetrieverFactory.get_instance(instrument["type"],
      instrument_id, logger, json.loads(instrument["config"]))

    if not retriever.test_configuration():
      log_instrument(instrument_id, logging.ERROR, "Configuration invalid")
    elif not retriever.startup():
      log_instrument(instrument_id, logging.ERROR, "Could not initialise retriever")
    else:
      while retriever.load_next_file():
        print("FILE")
        retriever.file_failed()

      retriever.shutdown()
