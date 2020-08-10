import os, datetime
import sqlite3
import collections
import pandas as pd

class DatabaseExtractor:
  """ Functions for dealing with the sqlite database """

  def __init__(self, filename, config):
    """ Constructor """
    if not os.path.isfile(filename):
      print("SQLite file " + filename + " does not exist")
      exit()

    self._config = config

    self._conn = sqlite3.connect(filename)

    if not self._check_config():
      self.disconnect()
      exit()

  def __del__(self):
    self.disconnect()

  def _check_config(self):
    """ Verify that the input table configuration is valid """

    result = True
    output_names = self._config['output']['columns']

    for table in self._config['input']['tables']:

      # Make sure the mapping input columns are in the table, and the named
      # output columns exist
      c = self._conn.cursor()
      c.execute("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='" + table['name'] + "'")
      if c.fetchone()[0] == 0:
        print("Table " + table['name'] + " does not exist in database")
        result = False
      c.close()

      # Make sure the specified input columns exist in the table, and the output columns are configured
      c = self._conn.cursor()
      c.execute("select * from " + table['name'] + " limit 1")
      input_names = [description[0] for description in c.description]
      c.close()

      table_outfields = []

      for outfield, infield in table['mapping']:

        table_outfields.append(outfield)
        if outfield not in output_names:
          print("Output field " + outfield + " not in configured output columns")
          result = False

        # Combined input fields are split by "~"
        if infield != "":
          for sub_infield in infield.split("~"):
            if sub_infield not in input_names:
              print("Field " + sub_infield + " is not in table " + table['name'])
              result = False

      # Check that table_outfields and output_names are identical
      if collections.Counter(table_outfields) != collections.Counter(output_names):
        print("Outputs for table " + table['name'] + " do not match main output config")

    return result

  def disconnect(self):
    """ Close the database connection """
    if self._conn is not None:
      self._conn.close()


  def get_dataset(self, table_name):
    """ Generate a dataset from the specified table """

    result = None

    # Build the SQL
    sql = "SELECT "

    for table in self._config['input']['tables']:
      if table['name'] == table_name:

        selects = []

        for outfield, infield in table['mapping']:
          if infield == "":
            infield_select = "NULL"
          else:
            infield_select = " || '~' || ".join(infield.split("~"))

          selects.append(infield_select + " AS '" + outfield + "'")

        sql += ",".join(selects)

        sql += " FROM " + table_name

        result = pd.read_sql_query(sql, self._conn)

    return result
