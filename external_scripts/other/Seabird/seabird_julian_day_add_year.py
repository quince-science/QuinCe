"""
Add a Year column to an output file from Seabird TSG sensors.

This is to be used when the Seabird output contains a decimal Julian Day.
It will not work if the time column contains Seconds or Hours from the
start of the file, but QuinCe can already handle that.
"""

import argparse
import calendar
from datetime import datetime
import re

def current_year():
    return datetime.now().strftime('%Y')


parser = argparse.ArgumentParser(
                    prog='seabird_julian_day_add_year.py',
                    description='Adds a year column to a Seabird TSG which contains Julian days in the time column')

parser.add_argument('in_file', help='Input file')
parser.add_argument('out_file', help='Destination file')

args = parser.parse_args()

if args.in_file == args.out_file:
    exit('in_file and out_file cannot be the same')

lines_copied = 0
year = None
days_in_year = None
days_to_subtract = 0

mode = 'HEADER'

with open(args.in_file) as input:
    with open(args.out_file, 'w') as output:

        in_line = input.readline()
        stripped = in_line.strip()

        while len(in_line) > 0:
            if len(stripped) > 0:
                if mode == 'HEADER':
                    # Copy the line to the output
                    output.write(f'{stripped}\n')

                    # See if we have the start time
                    if stripped.startswith('# start_time'):
                        start_date_line = stripped[15:35]

                        start_date = datetime.strptime(start_date_line, "%b %d %Y %H:%M:%S")
                        start_year = start_date.year
                        year = start_year
                        days_in_year = 366 if calendar.isleap(year) else 365

                    # See if we've hit the end of the header
                    if stripped == '*END*':
                        mode = 'DATA'

                elif mode == 'DATA':
                    # Split the julian day off from the rest of the line
                    split = stripped.split(' ', 1)
                    line_date = float(split[0])
                    rest = split[1]

                    # Subtract the day count from previous years
                    line_date = line_date - days_to_subtract

                    # Detect change of year
                    if int(line_date) > days_in_year:
                        
                        # All dates from now must subtract the
                        # number of days corresponding to previous years
                        days_to_subtract += days_in_year
                        line_date = line_date - days_to_subtract
                        
                        # Increment the year
                        year += 1
                        days_in_year = 366 if calendar.isleap(year) else 365

                    # Write the year, the adjusted Julian day, and the rest of the line
                    output.write(f'{year}    {line_date:.6f} {rest}\n')

            # Read the next line
            lines_copied += 1
            in_line = input.readline()
            stripped = in_line.strip()
