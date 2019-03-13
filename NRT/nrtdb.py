import sys
import sqlite3

# Functions for talking to the NRT scripts database

# Get a connection to the NRT database
# If it doesn't exist, create it
def get_db_conn(location):
  dbconn = None

  try:
    dbconn = sqlite3.connect(location)
    if not _is_db_setup(dbconn):
      __init_db(dbconn)
  except Exception as e:
    print("Failed to get database connection: %s" % e)

  return dbconn


# Close the database connection
def close(conn):
  conn.close()

# Get the stored instruments
def get_instruments(conn):
  result = []

  c = conn.cursor();
  c.execute("SELECT id, name, owner, type, preprocessor FROM instrument ORDER BY id")
  for row in c:
    record = {}
    record["id"] = row[0]
    record["name"] = row[1]
    record["owner"] = row[2]
    record["type"] = row[3]
    record["preprocessor"] = row[4]

    result.append(record)

  return result

# Get the IDs of the stored instruments
def get_instrument_ids(conn):
  result = []

  c = conn.cursor();
  c.execute("SELECT id FROM instrument ORDER BY id")
  for row in c:
    result.append(row[0])

  return result

# Get a stored instrument
def get_instrument(conn, instrument_id):
  result = None

  c = conn.cursor();
  c.execute("SELECT id, name, owner, type, config FROM instrument WHERE id = ?",
    (instrument_id, ))

  for row in c:
    record = {}
    record["id"] = row[0]
    record["name"] = row[1]
    record["owner"] = row[2]
    record["type"] = row[3]
    record["config"] = row[4]

    result = record

  return result

# Delete an instrument
def delete_instruments(conn, ids):
  c = conn.cursor()
  for one_id in ids:
    c.execute("DELETE FROM instrument WHERE id=?", (one_id, ))

  conn.commit()

# Add instruments with empty configuration
def add_instruments(conn, instruments, ids):
  c = conn.cursor()
  for instrument in instruments:
    if instrument["id"] in ids:
      c.execute("INSERT INTO instrument(id, name, owner, type, preprocessor, config)" \
        " VALUES (?, ?, ?, 'None', 'None', NULL)",
          (instrument["id"], instrument["name"], instrument["owner"]))
  conn.commit()

# List all instruments with no configuration
def get_unconfigured_instruments(conn):
  result = []

  c = conn.cursor()
  c.execute("SELECT id, name, owner FROM instrument WHERE type IS NULL")
  for row in c:
    record = {}
    record["id"] = row[0]
    record["name"] = row[1]
    record["owner"] = row[2]
    result.append(record)

  return result

# Store the configuration for an instrument
def store_configuration(conn, instrument_id, retriever, preprocessor):
  c = conn.cursor()
  if retriever is None:
    c.execute("UPDATE instrument SET type=NULL, config=NULL, preprocessor=NULL, WHERE id = ?",
      (instrument_id, ))
  else:
    print(retriever.get_type(), retriever.get_configuration_json(), instrument_id, preprocessor)
    c.execute("UPDATE instrument SET type=?, config=?, preprocessor=? WHERE id = ?",
      (retriever.get_type(), retriever.get_configuration_json(), preprocessor, instrument_id))

  conn.commit()

# See if the database has been
# initialised
def _is_db_setup(conn):
  result = False

  c = conn.cursor()
  c.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='instrument'")
  result = len(c.fetchall()) > 0

  return result

# Initialise the database
def __init_db(conn):
  instrument_sql = ("CREATE TABLE instrument("
                   "id INTEGER, "
                   "name TEXT, "
                   "owner TEXT, "
                   "type TEXT, "
                   "preprocessor TEXT, "
                   "config TEXT)"
                   )

  c = conn.cursor()
  c.execute(instrument_sql)
