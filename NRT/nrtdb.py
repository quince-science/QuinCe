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


# Get the stored instrument IDs
def get_instruments(conn):
  result = []

  c = conn.cursor();
  c.execute("SELECT id, name, owner FROM instrument ORDER BY id")
  for row in c:
    record = {}
    record["id"] = row[0]
    record["name"] = row[1]
    record["owner"] = row[2]

    result.append(record)

  return result

# Delete an instrument
def delete_instruments(conn, ids):
  c = conn.cursor()
  stmt = "DELETE FROM instrument WHERE id IN (%s)" % ",".join(map(str, ids))
  c.execute(stmt)
  conn.commit()

# Add instruments with empty configuration
def add_instruments(conn, instruments, ids):
  c = conn.cursor()
  for instrument in instruments:
    if instrument["id"] in ids:
      c.execute("INSERT INTO instrument(id, name, owner, type, config) VALUES (?, ?, ?)",
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
def store_configuration(conn, instrument_id, retriever):
  c = conn.cursor()
  c.execute("UPDATE instrument SET type=?, config=? WHERE id = ?",
    (retriever.get_type(), retriever.get_configuration_json(), instrument_id))
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
                   "config TEXT)"
                   )

  c = conn.cursor()
  c.execute(instrument_sql)
