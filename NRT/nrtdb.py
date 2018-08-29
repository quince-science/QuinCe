import sys
import sqlite3

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
  dbconn.close()


# See if the database has been
# initialised
def _is_db_setup(conn):
  result = False

  c = conn.cursor()
  c.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='instruments'")
  result = len(c.fetchall()) > 0

  return result

# Initialise the database
def __init_db(conn):
  instrument_sql = ("CREATE TABLE instrument("
                   "id INTEGER, "
                   "name TEXT, "
                   "owner TEXT)"
                   )

  c = conn.cursor()
  c.execute(instrument_sql)
