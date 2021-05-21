"""
Functions for extracting data from MapCO2 'Iridium format' files
"""
import os
from datetime import datetime

"""
Write the CSV header to a file
"""
def write_header(out_file):

  # Measurement Type
  out_file.write('Timestamp,State,')

  # System Info
  out_file.write('Zero Coefficient,Span Coefficient,Secondary Span Coefficient,')
  out_file.write('Flags,')

  # Data headers
  write_data_headers(out_file, 'On')
  write_data_headers(out_file, 'Off')
  write_data_headers(out_file, 'PostCal')

  out_file.write('Off-On Pressure Difference,')

  # xCO2 dry
  out_file.write('xCO2 Dry')

  out_file.write('\n')

def write_data_headers(out_file, prefix):
  out_file.write(f'{prefix} Licor Temp,')
  out_file.write(f'{prefix} Licor Temp SD,')
  out_file.write(f'{prefix} Licor Pressure,')
  out_file.write(f'{prefix} Licor Pressure SD,')
  out_file.write(f'{prefix} xCO2,')
  out_file.write(f'{prefix} xCO2 SD,')
  out_file.write(f'{prefix} O2,')
  out_file.write(f'{prefix} O2 SD,')
  out_file.write(f'{prefix} RH,')
  out_file.write(f'{prefix} RH SD,')
  out_file.write(f'{prefix} RH Temp,')
  out_file.write(f'{prefix} RH Temp SD,')
  out_file.write(f'{prefix} xCO2Raw1,')
  out_file.write(f'{prefix} xCO2Raw1 SD,')
  out_file.write(f'{prefix} xCO2Raw2,')
  out_file.write(f'{prefix} xCO2Raw SD,')

"""
Extract all measurements from a directory structure
"""
def extract_measurements(indir, outfile):

  # Get all filenames from the STATS folder
  stats_files = os.listdir(os.path.join(indir, 'STATS'))
  stats_files.sort()

  for filename in stats_files:
    measurement = get_measurement_data(indir, filename)
    write_measurement(outfile, measurement)


"""
Make a dict of all information required for a measurement
cycle, defined by a filename
"""
def get_measurement_data(dir, filename):
  measurement = {}
  measurement["stats"] = extract_stats(dir, filename)
  measurement["dry"] = extract_dry(dir, filename)
  return measurement

"""
Extract info from the STATS file
"""
def extract_stats(dir, filename):
  stats = {}

  # Open the STATS file
  with open(os.path.join(dir, 'STATS', filename), 'r') as f:
    # First line is the header
    f.readline()

    # Read the stats CSV lines
    stats["zero"] = read_mode_stats(f, True)
    stats["span"] = read_mode_stats(f, True)
    stats["ep"] = read_mode_stats(f, False)
    stats["ap"] = read_mode_stats(f, False)

    stats["flags"] = f.readline().strip()

    # Skip 'Licor' line
    f.readline()

    # Skip LastZero line
    f.readline()

    # Zero coefficient line
    stats["zero_coefficient"] = extract_coefficient(f.readline())

    # Skip LastSpan and LastSpan2 lines
    f.readline()
    f.readline()

    # Span coefficient
    stats["span_coefficient"] = extract_coefficient(f.readline())

    # Span2 coefficient
    stats["span2_coefficient"] = extract_coefficient(f.readline())

  return stats

"""
Read the lines for a given mode.

Reads 3 lines: ON, OFF, and PCAL. PCAL is only read if
include_postcal = True

Assumes that the passed in file is in the correct position
"""
def read_mode_stats(f, include_postcal):
  mode_stats = {}

  # On line
  line = f.readline().split(',')
  mode_stats["time"] = \
    datetime.fromisoformat(line[2].strip().replace('Z', '+00:00'))
  mode_stats["on"] = extract_stats_fields(line)

  # Zero off line
  line = f.readline().split(',')
  mode_stats["off"] = extract_stats_fields(line)

  if include_postcal:
    # Zero PostCal line
    line = f.readline().split(',')
    mode_stats["postcal"] = extract_stats_fields(line)

  return mode_stats

"""
Extract required fields from a given CSV line in the STATS file
"""
def extract_stats_fields(stats_line):
  stats = {}
  stats["licor_temp"] = float(stats_line[3])
  stats["licor_temp_sd"] = float(stats_line[4])
  stats["licor_pres"] = float(stats_line[5])
  stats["licor_pres_sd"] = float(stats_line[6])
  stats["xco2"] = float(stats_line[7])
  stats["xco2_sd"] = float(stats_line[8])
  stats["o2"] = float(stats_line[9])
  stats["o2_sd"] = float(stats_line[10])
  stats["rh"] = float(stats_line[11])
  stats["rh_sd"] = float(stats_line[12])
  stats["rhtemp"] = float(stats_line[13])
  stats["rhtemp_sd"] = float(stats_line[14])
  stats["xco2raw1"] = int(stats_line[15])
  stats["xco2raw1_sd"] = int(stats_line[16])
  stats["xco2raw2"] = int(stats_line[15])
  stats["xco2raw2_sd"] = int(stats_line[16])

  return stats

"""
Extract a coefficient from a STATS file line
"""
def extract_coefficient(line):
  return float(line.split(':')[1])

"""
Extract data from a DRY file
"""
def extract_dry(dir, filename):
  dry = {}

  # Open the DRY file
  with open(os.path.join(dir, 'DRY', filename), 'r') as f:

    # First line is the header
    f.readline()

    # Now the data line
    fields = f.readline().split(',')
    dry["water"] = float(fields[1])
    dry["atmosphere"] = float(fields[2])

  return dry

"""
Write a measurement line to the output file
"""
def write_measurement(outfile, measurement):
  # Write the zero line
  write_line(outfile, measurement, 'zero', 'ZP', True, False)
  write_line(outfile, measurement, 'span', 'SP', True, False)
  write_line(outfile, measurement, 'ep', 'EP', False, True)
  write_line(outfile, measurement, 'ap', 'AP', False, True)


def write_line(outfile, measurement, mode, modestring, include_postcal, include_dry):
  outfile.write(f'{measurement["stats"][mode]["time"].isoformat()},')
  outfile.write(f'{modestring},')
  outfile.write(f'{measurement["stats"]["zero_coefficient"]},')
  outfile.write(f'{measurement["stats"]["span_coefficient"]},')
  outfile.write(f'{measurement["stats"]["span2_coefficient"]},')
  outfile.write(f'{measurement["stats"]["flags"]},')

  write_line_section(outfile, measurement["stats"][mode]["on"])
  write_line_section(outfile, measurement["stats"][mode]["off"])

  if include_postcal:
    write_line_section(outfile, measurement["stats"][mode]["postcal"])
  else:
    write_empty_line_section(outfile)

  # Pressure difference
  press_diff = measurement["stats"][mode]["off"]["licor_pres"] - \
    measurement["stats"][mode]["on"]["licor_pres"]
  outfile.write(f'{press_diff:.2f},')

  # Add the Dry fields
  if not include_dry:
    outfile.write('NaN')
  elif modestring == 'EP':
    outfile.write(f'{measurement["dry"]["water"]}')
  else:
    outfile.write(f'{measurement["dry"]["atmosphere"]}')

  outfile.write('\n')

"""
Write the stats sub-part of a line to the output file
"""
def write_line_section(outfile, section):
  outfile.write(f'{section["licor_temp"]},')
  outfile.write(f'{section["licor_temp_sd"]},')
  outfile.write(f'{section["licor_pres"]},')
  outfile.write(f'{section["licor_pres_sd"]},')
  outfile.write(f'{section["xco2"]},')
  outfile.write(f'{section["xco2_sd"]},')
  outfile.write(f'{section["o2"]},')
  outfile.write(f'{section["o2_sd"]},')
  outfile.write(f'{section["rh"]},')
  outfile.write(f'{section["rh_sd"]},')
  outfile.write(f'{section["rhtemp"]},')
  outfile.write(f'{section["rhtemp_sd"]},')
  outfile.write(f'{section["xco2raw1"]},')
  outfile.write(f'{section["xco2raw1_sd"]},')
  outfile.write(f'{section["xco2raw2"]},')
  outfile.write(f'{section["xco2raw2_sd"]},')

"""
Write an empty stats sub-part of a line to the output file
"""
def write_empty_line_section(outfile):
  for i in range(0, 16):
    outfile.write('NaN,')