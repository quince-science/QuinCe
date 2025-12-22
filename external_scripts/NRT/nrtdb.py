import sqlite3
import json
from datetime import datetime


# Functions for talking to the NRT scripts database

# Get a connection to the NRT database
# If it doesn't exist, create it
def get_db_conn(location):
    db_conn = None

    try:
        db_conn = sqlite3.connect(location)
        if not _is_db_setup(db_conn):
            __init_db(db_conn)
    except Exception as e:
        print("Failed to get database connection: %s" % e)

    return db_conn


# Close the database connection
def close(conn):
    conn.close()


# Get the stored instruments
def get_instruments(conn):
    result = []

    c = conn.cursor()
    c.execute("SELECT id, name, owner, type, preprocessor, check_hours, paused FROM instrument ORDER BY name")
    for row in c:
        record = {"id": row[0], "name": row[1], "owner": row[2], "type": row[3], "preprocessor": row[4],
                  "check_hours": row[5], "paused": row[6]}

        if record["check_hours"] is None:
            record["check_hours"] = [0]

        result.append(record)

    return result


# Get the IDs of the stored instruments
def get_instrument_ids(conn):
    result = []

    c = conn.cursor()
    c.execute("SELECT id FROM instrument ORDER BY id")
    for row in c:
        result.append(row[0])

    return result


# Get a stored instrument
def get_instrument(conn, instrument_id):
    c = conn.cursor()
    c.execute("SELECT id, name, owner, type, preprocessor, config, preprocessor_config, "
              "check_hours, last_check, paused FROM instrument WHERE id = ?",
              (instrument_id,))

    row = c.fetchone()
    record = {"id": row[0], "name": row[1], "owner": row[2], "type": row[3], "preprocessor": row[4],
              "config": row[5], "preprocessor_config": row[6], "check_hours": row[7], "last_check": row[8],
              "paused": True if row[9] == 1 else False}

    if record["check_hours"] is None:
        record["check_hours"] = [0]
    else:
        record["check_hours"] = json.loads(record["check_hours"])

    return record


# Delete an instrument
def delete_instruments(conn, ids):
    c = conn.cursor()
    for one_id in ids:
        c.execute("DELETE FROM instrument WHERE id=?", (one_id,))

    conn.commit()


# Add instruments with empty configuration
def add_instruments(conn, instruments, ids):
    c = conn.cursor()
    for instrument in instruments:
        if instrument["id"] in ids:
            c.execute("INSERT INTO instrument(id, name, owner, type, preprocessor, config, preprocessor_config)"
                      " VALUES (?, ?, ?, 'None', 'None', NULL, NULL)",
                      (instrument["id"], instrument["name"], instrument["owner"]))
    conn.commit()


# List all instruments with no configuration
def get_unconfigured_instruments(conn):
    result = []

    c = conn.cursor()
    c.execute("SELECT id, name, owner FROM instrument WHERE type IS NULL")
    for row in c:
        record = {"id": row[0], "name": row[1], "owner": row[2]}
        result.append(record)

    return result


# Store the configuration for an instrument
def store_configuration(conn, instrument_id, retriever, preprocessor, check_hours):
    c = conn.cursor()
    if retriever is None:
        c.execute(
            "UPDATE instrument SET type=NULL, config=NULL, preprocessor=NULL, preprocessor_config=NULL,"
            "check_hours=NULL, last_check=NULL WHERE id = ?", (instrument_id,))
    else:
        c.execute("UPDATE instrument SET type=?, config=?, preprocessor=?, preprocessor_config=?, "
                  "check_hours=? WHERE id = ?",
                  (retriever.get_type(), retriever.get_configuration_json(),
                   preprocessor.get_type(), preprocessor.get_configuration_json(), str(check_hours), instrument_id))

    conn.commit()


def set_last_check(conn, instrument):
    instrument["last_check"] = int(datetime.now().timestamp())
    c = conn.cursor()
    c.execute("UPDATE instrument SET last_check=? WHERE id = ?",
              (instrument["last_check"], instrument["id"]))
    conn.commit()


# See if the database has been
# initialised
def _is_db_setup(conn):
    c = conn.cursor()
    c.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='instrument'")
    return len(c.fetchall()) > 0


# Initialise the database
def __init_db(conn):
    instrument_sql = ("CREATE TABLE instrument("
                      "id INTEGER, "
                      "name TEXT, "
                      "owner TEXT, "
                      "type TEXT, "
                      "preprocessor TEXT, "
                      "config TEXT, "
                      "preprocessor_config TEXT, "
                      "check_hours TEXT, "
                      "last_check INTEGER",
                      "paused INTEGER DEFAULT 0"
                      ")"
                      )

    c = conn.cursor()
    c.execute(instrument_sql)

def toggle_pause(conn, instrument):
    try:
        c = conn.cursor()
        c.execute("SELECT paused FROM instrument WHERE id=?", (instrument, ))
        record = c.fetchone()

        if record is not None:
            paused = record[0]
            paused = 1 if paused == 0 else 0

            c.execute("UPDATE instrument SET paused=? WHERE id=?", (paused, instrument))
            conn.commit()
    finally:
        c.close()
