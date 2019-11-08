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

      # Make sure the mapping names exist in the output column list
      for mapping_column in table["mapping"]:
        column_found = False

        if len(mapping_column) > 0:
          for outputcol in self._config["output"]["columns"]:
            for col in outputcol.split("~"):
              if col == mapping_column:
                column_found = True


          if not column_found:
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

    out_row = []

    for outcol in self._config["output"]["columns"]:
      #print(outcol)
      out_values = []

      for col in outcol.split("~"):

        in_index = None

        try:
          in_index = mapping.index(col)
        except ValueError:
          pass

        if in_index is not None:
          out_values.append(row[in_index])

      #print(out_values)
      if len(out_values) == 0:
        out_row.append(None)
      elif len(out_values) == 1:
        out_row.append(out_values[0])
      else:
        out_row.append("~".join(str(v) for v in out_values))

    return out_row