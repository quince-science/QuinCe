'''
Formats export files from QuinCe for submission to SOCAT

Files exported from QuinCe in the SOCAT format need some changes before they
can be sent to the SOCAT upload dashboard:

- Date/Time column reformatted
  from 'YYYY-MM-DDTHH:MM:SS.SSSZ' to 'YYYY-MM-DD HH:MM:SS.SSS'
- Add header containing simple metadata
  (EXPO Code, Vessel Name, PIs, Vessel Type)
  This information will be read from the input filename and platforms.toml
- Rename QC columns

Input files are expected to have a filename of <expocode>-SOCAT.tsv
Output files will be named <expocode>.tsv

Usage:

python socat_formatter.py <input_file>

Steve Jones 2021-01-06
'''
import sys
import toml
import re
import pandas as pd

def main(infile):
  metadata = build_metadata(infile)

  # Load the csv file and make required changes

  # Reformat date/time
  data = pd.read_table(infile)
  data.iloc[:, 0] = pd.to_datetime(data.iloc[:,0]) \
    .dt.strftime('%Y-%m-%d %H:%M:%S.%f')

  # Write header
  out_file = f'{metadata["expocode"]}.tsv'
  with open(out_file, 'w') as out:
    out.write(f'Expocode: {metadata["expocode"]}\n')
    out.write(f'Vessel Name: {metadata["vessel_name"]}\n')
    out.write(f'PIs: {metadata["pis"]}\n')
    out.write(f'Vessel Type: {metadata["vessel_type"]}\n')

  data.to_csv(out_file, sep='\t', index=False, mode='a')

def build_metadata(filename):
  with open('platforms.toml') as f: platforms = toml.load(f)

  metadata = {}

  m = re.match('(.*)([0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9])-SOCAT.tsv',
    filename)

  platform = m[1]
  date = m[2]
  metadata['expocode'] = f'{platform}{date}'

  try:
    platform_info = platforms[platform]
    metadata['vessel_name'] = platform_info['name']
    metadata['vessel_type'] = \
      get_platform_type_name(platform_info['category_code'])

    metadata['pis'] = platform_info['author_list']
  except KeyError:
    raise Exception(f'Platform {platform} not found')

  return metadata

def get_platform_type_name(code):
  if code == '31':
    return 'Ship-based time series (vessel)'
  elif code == '32':
    return 'Voluntary Observing Ship '
  elif code == '3B':
    return 'Saildrone'
  else:
    raise Exception(f'Unrecognised platform code {code}')

if __name__ == '__main__':
  if len(sys.argv) != 2:
   print('Usage: python socat_formatter.py <input_file>')
  else:
    main(sys.argv[1])
