"""
Add a Year column to an output file from the SAMI pCO2 processing software.

The software only includes the decimal day of the year, but not the
year itself. Which is dumb.
"""

import argparse
from datetime import datetime

def current_year():
    return datetime.now().strftime('%Y')


parser = argparse.ArgumentParser(
                    prog='sami_year.py',
                    description='Adds a year column to the output from the SAMI CO2 data processing software')

parser.add_argument('in_file', help='SAMI output file')
parser.add_argument('out_file', help='Destination file')
parser.add_argument('-y', '--year', required=False, default=current_year(), type=int, help='The year to add. Defaults to current year if not specified')

args = parser.parse_args()

if args.in_file == args.out_file:
    exit('in_file and out_file cannot be the same')

lines_copied = 0

with open(args.in_file) as input:
    with open(args.out_file, 'w') as output:

        in_line = input.readline()
        stripped = in_line.strip()
        while len(in_line) > 0:
            if len(stripped) > 0:
                if lines_copied == 0:
                    output.write(f'Year\t{stripped}\n')
                else:
                    output.write(f'{args.year}\t{stripped}\n')

            lines_copied += 1
            in_line = input.readline()
            stripped = in_line.strip()
            print(in_line)
