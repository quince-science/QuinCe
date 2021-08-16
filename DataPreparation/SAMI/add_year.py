"""
Output files from the SAMI client software contain a decimal day of year
but no year. QuinCe requires a year.

Eventually I'll build suitable functionality into QuinCe, but until then
this utility can be used to add a Year column to SAMI output files.

This is in no way good Python code. Do not use it for reference.
"""
import sys
import os
import pandas as pd

# Suppress warnings because I'm doing dumb shit. As I said above,
# I'm not pretending this is good code.
pd.options.mode.chained_assignment = None

def main(in_file, out_file, year):
  data = pd.read_csv(in_file, sep="\t")

  # Add the year column
  data['Year'] = 0

  current_year = year

  for index, row in data.iterrows():
    if index == 0:
      data['Year'][index] = current_year
    else:

      if row['Year Day'] < data['Year Day'][index - 1]:
        current_year += 1

      data['Year'][index] = current_year

  data.to_csv(out_file, sep="\t", index=False)




def usage():
  print('Usage: python add_year.py <infile> <year>')
  exit()


if __name__ == '__main__':

  in_file = None
  year = 0

  if len(sys.argv) != 3:
    usage()

  in_file = sys.argv[1]

  try:
    year = int(sys.argv[2])
  except:
    print('Year must be an integer')
    exit()

  root, ext = os.path.splitext(in_file)
  out_file = f'{root}.withyear{ext}'

  main(in_file, out_file, year)