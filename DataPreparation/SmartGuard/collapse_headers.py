"""
Collapses the column headers in an Aanderaa SmartGuard file,
so we have a single row of unique headers
"""
import sys
import os

HEADER_LENGTH = 7
SEPARATOR = ";"
COLUMN_ROW_COUNT = 3

def main(in_file_name, out_file_name):

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

  out_file.write(';'.join(headers))
  out_file.write('\n')

  # Now copy the data lines
  eof = False
  while not eof:
    line = in_file.readline()
    if line.strip() == '':
      eof = True
    else:
      out_file.write(line)

  # Close files
  out_file.close()
  in_file.close()

if __name__ == '__main__':
  if len(sys.argv) != 2:
    print("Usage: python collapse_headers.py <in_file>")
    exit()

  in_file = sys.argv[1]
  root, ext = os.path.splitext(in_file)
  out_file = f'{root}.singleheader{ext}'
  main(in_file, out_file)
