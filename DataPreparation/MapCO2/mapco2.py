"""
Functions for extracting data from MapCO2 'Iridium format' files
"""
"""
Read the next measurement from the passed in file.

Reads lines until it sees a line starting with a measurement identifier
word, then reads the next set of lines to extract all the measurement info.

The measurement is returned as a Python dictionary. The write_measurement
function will know what order to output the fields.
"""
from datetime import datetime, timezone
from datetime import timedelta
import math

MEASUREMENT_START_WORDS = ['NORM', 'FAST']

def extract_measurements(in_file, out_file):

  # Position flags
  eof = False

  # The last read line
  current_line = None

  # Keep reading lines until we find a measurement or we fall off the
  # end of the file.
  while not eof:
    current_line = in_file.readline()
    if not current_line:
      eof = True

    elif current_line.startswith(tuple(MEASUREMENT_START_WORDS)):
      # We've found a measurement.

      # Read the measurement lines
      date_line = extract_date_line(current_line)
      gps_line = read_gps_line(in_file)
      system_line = read_system_line(in_file)
      zero_on_line = read_data_line(in_file)
      zero_off_line = read_data_line(in_file)
      zero_post_cal_line = read_data_line(in_file)
      span_on_line = read_data_line(in_file)
      span_off_line = read_data_line(in_file)
      span_post_cal_line = read_data_line(in_file)
      equ_on_line = read_data_line(in_file)
      equ_off_line = read_data_line(in_file)
      air_on_line = read_data_line(in_file)
      air_off_line = read_data_line(in_file)
      xco2_line = read_xco2_line(in_file)

      # Format and write the lines to the out_file

      # Zero Line
      write_line(out_file, 'ZERO', date_line, gps_line, system_line, \
        zero_on_line, zero_off_line, zero_post_cal_line, None)

      # Span line
      write_line(out_file, 'SPAN', date_line, gps_line, system_line, \
        span_on_line, span_off_line, span_post_cal_line, None)

      # Equil line
      write_line(out_file, 'EQU', date_line, gps_line, system_line, \
        equ_on_line, equ_off_line, None, xco2_line)

      # Air line
      write_line(out_file, 'AIR', date_line, gps_line, system_line, \
        air_on_line, air_off_line, None, xco2_line)

"""
Read a line from the input file, throwing an exception if it's empty
"""
def read_line(in_file):
  line = in_file.readline()
  if not line:
    raise PartialMeasurementError

  return line

"""
Read a date line from the input file and return a dict
"""
def extract_date_line(line):
  fields = line.split()

  if len(fields) < 5:
    raise InvalidLineError

  result = {}
  result["measurement_type"] = fields[0]

  datestring = f'{fields[3]} {fields[4]}'
  result["time"] = datetime.strptime(datestring, '%Y/%m/%d %H:%M:%S') \
    .replace(tzinfo=timezone.utc)

  result["serial_no"] = fields[6]
  result["firmware_version"] = fields[7]
  result["firmware_date"] = datetime.strptime(fields[8], '%m/%d/%Y')

  return result

"""
Read a GPS line from the input file and return a dict
"""
def   read_gps_line(in_file):
  line = read_line(in_file)
  fields = line.split()

  if len(fields) < 7:
    raise InvalidLineError

  result = {}

  # Detect missing GPS data
  if fields[0] == '00/00/0000':
    result["gps_time"] = ''
    result["longitude"] = math.nan
    result["latitude"] = math.nan
    result["gps_acquisition_time"] = -1
  else:
    datestring = f'{fields[0]} {fields[1]}'
    result["gps_time"] = datetime.strptime(datestring, '%m/%d/%Y %H:%M:%S') \
      .replace(tzinfo=timezone.utc)
    result["longitude"] = get_degrees(fields[4], fields[5])
    result["latitude"] = get_degrees(fields[2], fields[3])
    result["gps_acquisition_time"] = int(fields[6])

  return result

"""
Get a decimal degrees values from degress/decimal minutes + hemisphere
"""
def get_degrees(decimal_minutes, hemisphere):
  split = decimal_minutes.split('.')

  degrees = float(split[0][:-2])
  minutes = float(split[0][-2:] + "." + split[1])
  minutes_fraction = minutes / 60

  degrees += (minutes / 60)

  if hemisphere == 'W' or hemisphere == 'S':
    degrees *= -1

  return degrees


"""
Read a system status line from the input file and return a dict
"""
def read_system_line(in_file):
  line = read_line(in_file)
  fields = line.split()

  if len(fields) < 6:
    raise InvalidLineError

  result = {}
  result["logic_battery"] = float(fields[0])
  result["transmitter_battery"] = float(fields[1])
  result["zero_coefficient"] = float(fields[2])
  result["span_coefficient"] = float(fields[3])
  result["secondary_span_coefficient"] = float(fields[4])

  flags = fields[5]
  result["span_flag"] = int(flags[0:2], 16)
  result["zero_flag"] = int(flags[2:4], 16)

  return result

"""
Read a data line from the input file and return a dict
"""
def read_data_line(in_file):
  line = read_line(in_file)
  fields = line.split()

  if len(fields) != 17:
    raise InvalidLineError

  result = {}
  result["minute"] = int(fields[0])
  result["licortemp"] = float(fields[1])
  result["licortemp_sd"] = float(fields[2])
  result["licorpress"] = float(fields[3])
  result["licorpress_sd"] = float(fields[4])
  result["xco2"] = float(fields[5])
  result["xco2_sd"] = float(fields[6])
  result["o2"] = float(fields[7])
  result["o2_sd"] = float(fields[8])
  result["rh"] = int(fields[9])
  result["rh_sd"] = float(fields[10])
  result["rhtemp"] = float(fields[11])
  result["rhtemp_sd"] = float(fields[12])
  result["xco2raw1"] = int(fields[13])
  result["xco2raw1_sd"] = int(fields[14])
  result["xco2raw2"] = int(fields[15])
  result["xco2raw2_sd"] = int(fields[16])

  return result

"""
Read the final xCO2 line and return a dict
"""
def read_xco2_line(in_file):
  line = read_line(in_file)
  fields = line.split()

  if len(fields) != 4:
    raise InvalidLineError

  result = {}
  result["equ_xco2_dry"] = float(fields[1])
  result["air_xco2_dry"] = float(fields[3])
  return result

"""
Write the CSV header to a file
"""
def write_header(out_file):

  # Measurement Type
  out_file.write('Timestamp,Type,State,')

  # GPS Info
  out_file.write('Longitude,Latitude,GPS Time,GPS Acquisition Time,')

  # System Info
  out_file.write('Logic Battery,Transmitter Battery,Zero Coefficient,')
  out_file.write('Span Coefficient,Secondary Span Coefficient,')
  out_file.write('Span Flag,Zero Flag,')

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
Write a line to the output file
"""
def write_line(out_file, state, date_line, gps_line, system_line, on_line, off_line, \
  post_cal_line, xco2_line):

  # Timestamp is from the "On" line
  line_time = calc_line_time(date_line["time"], on_line["minute"])
  out_file.write(f'{line_time.isoformat()},')

  # Measurement Type
  out_file.write(f'{date_line["measurement_type"]},')
  out_file.write(f'{state},')

  # GPS Info
  out_file.write(f'{gps_line["longitude"]:.4f},')
  out_file.write(f'{gps_line["latitude"]:.4f},')
  out_file.write(f'{gps_line["gps_time"].isoformat()},')
  out_file.write(f'{gps_line["gps_acquisition_time"]},')

  # System Info
  out_file.write(f'{system_line["logic_battery"]},')
  out_file.write(f'{system_line["transmitter_battery"]},')
  out_file.write(f'{system_line["zero_coefficient"]},')
  out_file.write(f'{system_line["span_coefficient"]},')
  out_file.write(f'{system_line["secondary_span_coefficient"]},')
  out_file.write(f'{system_line["span_flag"]},')
  out_file.write(f'{system_line["zero_flag"]},')

  # Data fields
  write_data_columns(out_file, on_line)
  write_data_columns(out_file, off_line)
  write_data_columns(out_file, post_cal_line)

  # On-Off pressure difference
  out_file.write(f'{(off_line["licorpress"] - on_line["licorpress"]):.2f},')

  # xCO2 dry
  if state == 'EQU':
    out_file.write(f'{xco2_line["equ_xco2_dry"]}')
  elif state == 'AIR':
    out_file.write(f'{xco2_line["air_xco2_dry"]}')
  else:
    out_file.write('NaN')

  out_file.write('\n')

"""
Write fields from a data line to an output file
"""
def write_data_columns(out_file, line):

  if line is None:
    for i in range(0,16):
      out_file.write('NaN,')
  else:
    out_file.write(f'{line["licortemp"]},')
    out_file.write(f'{line["licortemp_sd"]},')
    out_file.write(f'{line["licorpress"]},')
    out_file.write(f'{line["licorpress_sd"]},')
    out_file.write(f'{line["xco2"]},')
    out_file.write(f'{line["xco2_sd"]},')
    out_file.write(f'{line["o2"]},')
    out_file.write(f'{line["o2_sd"]},')
    out_file.write(f'{line["rh"]},')
    out_file.write(f'{line["rh_sd"]},')
    out_file.write(f'{line["rhtemp"]},')
    out_file.write(f'{line["rhtemp_sd"]},')
    out_file.write(f'{line["xco2raw1"]},')
    out_file.write(f'{line["xco2raw1_sd"]},')
    out_file.write(f'{line["xco2raw2"]},')
    out_file.write(f'{line["xco2raw2_sd"]},')

"""
Calculate the true date/time for a line
"""
def calc_line_time(base_time, minute):
  line_time = base_time

  # If the line minute is less than the base minute, we've rolled round an hour
  if minute < line_time.minute:
    line_time = line_time + timedelta(hours=1)

  line_time = line_time.replace(minute=minute)

  return line_time

# Exceptions

class PartialMeasurementError(Exception):
  """
  Error thrown if a file ends part way through a measurement
  or an individual line doesn't contain what we expect
  """
  pass

class InvalidLineError(Exception):
  """
  Error thrown if a line cannot be parsed
  """
  pass
