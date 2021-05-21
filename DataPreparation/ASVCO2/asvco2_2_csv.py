"""
Convert data in an ASVCO2 directory structure to a CSV file
"""
import sys
import os
from asvco2 import write_header, extract_measurements

def main(indir, outfilename):

  with open(outfilename, 'w') as outfile:

    # Write the CSV header
    write_header(outfile)

    # Extract measurements from the data directory
    # and write them to the output file
    extract_measurements(indir, outfile)

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

"""
Check that a folder contains expected ASVCO2 contents
"""
def check_indir(indir):
  subdirs = ['STATS', 'DRY']
  for subdir in subdirs:
    if not os.path.isdir(os.path.join(indir, subdir)):
      return False

  return True

if __name__ == '__main__':
  command_line_ok = True

  dir = None
  outfile = None

  if len(sys.argv) != 2:
    print('Usage: mapco2_2_csv.py <data_dir>')
    command_line_ok = False
  else:
    indir = sys.argv[1]

    if not os.path.isdir(indir):
      print(f'{indir} is not a directory')
      command_line_ok = False

    if not check_indir(indir):
      print('Invalid directory structure')
      command_line_ok = False

    outfile = f'{indir}.csv'

    if not check_file_writable(outfile):
      print(f'Cannot write output {outfile}')
      command_line_ok = False

  if command_line_ok:
    main(indir, outfile)
