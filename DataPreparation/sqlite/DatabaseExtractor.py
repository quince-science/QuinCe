import os, sqlite3, datetime

class DatabaseExtractor:

  # Constructor and table inisialiser
  def __init__(self, filename, config):
    if not os.path.isfile(filename):
      print("SQLite file " + filename + " does not exist")
      exit()

    self._config = config

    self._conn = sqlite3.connect(filename)
    self._conn.row_factory = sqlite3.Row

    self._cursors = []
    self._next_rows = []
    self._date_columns = []
    
    if not self._check_config():
      self.disconnect()
      exit()

    self.make_cursors()

  # Check the configuration
  def _check_config(self):
    result = True

    for table in self._config["input"]["tables"]:
    
      self._date_columns.append(table["datecol"])

      # Make sure the number of table mappings matches
      # the number of output columns
#      if len(table["mapping"]) != len(self._config["output"]["columns"]):
#        print("Mapping list for table " + table["name"] + 
#                 " is not the same length as the output mappings")
#        result = False

      # Make sure the mapping names exist in the output column list
      for mapping_column in table["mapping"]:
        if len(mapping_column) > 0 and \
             mapping_column not in self._config["output"]["columns"]:
        
          print("Mapping " + mapping_column + " in table " + table["name"] +
               " not specified in output columns")
          result = False

    return result

  # Close all cursors and connections
  def disconnect(self):
    for cursor in self._cursors:
      cursor.close() 

    if self._conn is not None:
      self._conn.close()

  # Create the table cursors to be used during extraction
  def make_cursors(self):
    for i in range(0, len(self._config["input"]["tables"])):
      table = self._config["input"]["tables"][i]
      cursor = self._conn.execute("SELECT * FROM " + table["name"])
      self._cursors.append(cursor)
      self._next_rows.append(None)
      self.load_next_row(i)


  # Get the next row chosen from all
  # tables by time
  def get_next_row_table(self):
    earliest_date_table = None
    earliest_date = None

    for i in range(0, len(self._next_rows)):

      row = self._next_rows[i]
      if row is not None:
        row_date = datetime.datetime.strptime(
            self._next_rows[i][self._date_columns[i]], "%Y-%m-%d %H:%M:%S")

        if earliest_date is None:
          earliest_date_table = i
          earliest_date = row_date
        elif row_date < earliest_date:
          earliest_date_table = i
          earliest_date = row_date

    return earliest_date_table

  # Load the next row from the specified table
  def load_next_row(self, table_id):
    row_found = False

    while not row_found:
      next_row = self._cursors[table_id].fetchone()
      if next_row is None or not self._ignore_row(table_id, next_row):
        self._next_rows[table_id] = next_row
        row_found = True

  # Determine whether a row should be ignored
  def _ignore_row(self, table_id, row):
    result = False

    table_config = self._config["input"]["tables"][table_id]

    if "ignore" in table_config.keys():
      ignore_col = table_config["ignore"]["column_index"]
      ignore_val = table_config["ignore"]["value"]

      result = (row[ignore_col] == ignore_val)

    return result

  # Get the current row from the specified table,
  # with values mapped to the output columns
  def get_mapped_row(self, table_id):
    row = self._next_rows[table_id]

    mapping = self._config["input"]["tables"][table_id]["mapping"]
    
    out_fields = []

    for outcol in self._config["output"]["columns"]:
      in_index = None

      try:
        in_index = mapping.index(outcol)
      except ValueError:
        pass

      if in_index is None:
        out_fields.append(None)
      else:
        out_fields.append(row[in_index])

    return out_fields