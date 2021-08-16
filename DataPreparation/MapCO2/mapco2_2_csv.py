"""
Convert 'Iridium' format data from MapCO2 sensors to CSV files.

Usage: mapco2_2_csv.py <infile> <outfile>

"""

import sys
import os
from mapco2 import write_header, extract_measurements

"""
Main processing function
"""
def main(infilename, outfilename):

  # Open the output file for writing
  with open(outfilename, 'w') as outfile:

    # Write the CSV header
    write_header(outfile)

    # Open the input file
    with open(infilename, 'r') as infile:
      extract_measurements(infile, outfile)

"""
See if a file can be written.

Shamelessly stolen from
https://www.novixys.com/blog/python-check-file-can-read-write
"""
def check_file_writable(fnm):
    if os.path.exists(fnm):
        # path exists
        if os.path.isfile(fnm): # is it a file or a dir?
            # also works when file is a link and the target is writable
            return os.access(fnm, os.W_OK)
        else:
            return False # path is a dir, so cannot write as a file
    # target does not exist, check perms on parent dir
    pdir = os.path.dirname(fnm)
    if not pdir: pdir = '.'
    # target is creatable if parent dir is writable
    return os.access(pdir, os.W_OK)

# Script run
if __name__ == '__main__':
  command_line_ok = True

  infile = None
  outfile = None

  if len(sys.argv) != 3:
    print('Usage: mapco2_2_csv.py <infile> <outfile>')
    command_line_ok = False
  else:
    infile = sys.argv[1]
    outfile = sys.argv[2]

    if not os.path.isfile(infile):
      print(f'Cannot open {infile}')
      command_line_ok = False

    if not check_file_writable(outfile):
      print(f'Cannot write output {outfile}')
      command_line_ok = False

  if command_line_ok:
    main(infile, outfile)
