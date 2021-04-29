"""
Collapses the column headers in an Aanderaa SmartGuard file,
so we have a single row of unique headers
"""
import sys
import os

HEADER_LENGTH = 7
SEPARATOR = ";"
COLUMN_ROW_COUNT = 3

def main(in_file_name, out_file_name, new_columns):

  in_file = open(in_file_name, 'r')
  out_file = open(out_file_name, 'w')

  # Copy header
  for i in range(0, HEADER_LENGTH):
    out_file.write(in_file.readline())

  header_lines = []
  for i in range(0, COLUMN_ROW_COUNT):
    line = in_file.readline()
    fields = line.split(SEPARATOR)
    header_lines.append(fields)

  headers = []

  current_header_entries = list(x[0] for x in header_lines)
  last_header = None

  for i in range(0, len(header_lines[0])):
    for j in range(0, COLUMN_ROW_COUNT):
      if header_lines[j][i].strip() != '' and current_header_entries[j] != header_lines[j][i]:
        current_header_entries[j] = header_lines[j][i]

    header = '|'.join(current_header_entries)
    if header != last_header:
      headers.append(header)
      last_header = header

  for col in new_columns:
    headers.append(col[0])

  out_file.write(SEPARATOR.join(headers))
  out_file.write('\n')

  # Now copy the data lines
  eof = False
  while not eof:
    line = in_file.readline()
    if line.strip() == '':
      eof = True
    else:
      # The last column is empty because of trailing separators. Remove it.
      fields = line.split(SEPARATOR)[:-1]
      for col in new_columns:
        fields.append(col[1])

      out_file.write(f'{SEPARATOR.join(fields)}\n')

  # Close files
  out_file.close()
  in_file.close()

def usage():
  print('Usage: python collapse_headers [-addcolumn <name> <value>] <infile>')
  exit()


if __name__ == '__main__':

  in_file = None
  out_file = None
  new_columns = []

  current_arg = 1

  while current_arg < len(sys.argv):
    if sys.argv[current_arg] == '-addcolumn':
      if len(sys.argv) < current_arg + 3:
        usage()
      else:
        colname = sys.argv[current_arg + 1]
        colvalue = sys.argv[current_arg + 2]
        new_columns.append((colname, colvalue))
        current_arg += 2
    elif sys.argv[current_arg].startswith('-'):
      usage()
    else:
      in_file = sys.argv[current_arg]
      current_arg += 1


  root, ext = os.path.splitext(in_file)
  out_file = f'{root}.singleheader{ext}'

  main(in_file, out_file, new_columns)
