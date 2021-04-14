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

def extract_measurement(infile):

  # The returned measurement
  measurement = None

  # Position flags
  found_measurement = False
  eof = False

  # The last read line
  current_line = None

  # Keep reading lines until we find a measurement or we fall off the
  # end of the file.
  while not found_measurement and not eof:
    current_line = infile.readline()
    if not current_line:
      eof = True

    elif current_line.startswith(tuple(MEASUREMENT_START_WORDS)):
      # We've found a measurement.
      found_measurement = True

      # Read the measurement lines
      date_line = extract_date_line(current_line)
      gps_line = read_gps_line(infile)
      system_line = read_system_line(infile)
      zero_on_line = read_data_line(infile)
      zero_off_line = read_data_line(infile)
      zero_post_cal_line = read_data_line(infile)
      span_on_line = read_data_line(infile)
      span_off_line = read_data_line(infile)
      span_post_cal_line = read_data_line(infile)
      equ_on_line = read_data_line(infile)
      equ_off_line = read_data_line(infile)
      air_on_line = read_data_line(infile)
      air_off_line = read_data_line(infile)
      xco2_line = read_xco2_line(infile)

      # We take the final Air Off line as the actual measurement time
      measurement_time = date_line['time'] + \
        timedelta(minutes=air_off_line['minute'])

      # Extract the simple data into the final measurement record
      measurement = {}
      measurement['timestamp'] = measurement_time
      measurement['type'] = date_line['measurement_type']
      measurement.update(gps_line)
      measurement.update(system_line)
      measurement.update(xco2_line)

      # Now add the more complex data
      measurement['zero_pres_on'] = zero_on_line['licorpress']
      measurement['zero_pres_on_sd'] = zero_on_line['licorpress_sd']
      measurement['zero_pres_off'] = zero_off_line['licorpress']
      measurement['zero_pres_off_sd'] = zero_off_line['licorpress_sd']
      measurement['zero_pres_post'] = zero_post_cal_line['licorpress']
      measurement['zero_pres_post_sd'] = zero_post_cal_line['licorpress_sd']
      measurement['zero_pres_delta'] = \
        measurement['zero_pres_post'] - measurement['zero_pres_off']

      measurement['zero_xco2_raw1'] = zero_post_cal_line['xco2raw1']
      measurement['zero_xco2_raw1_sd'] = zero_post_cal_line['xco2raw1_sd']
      measurement['zero_xco2_raw2'] = zero_post_cal_line['xco2raw2']
      measurement['zero_xco2_raw2_sd'] = zero_post_cal_line['xco2raw2_sd']
      measurement['zero_xco2'] = zero_post_cal_line['xco2']
      measurement['zero_xco2_sd'] = zero_post_cal_line['xco2_sd']

      measurement['zero_licor_temp'] = zero_post_cal_line['licortemp']
      measurement['zero_licor_temp_sd'] = zero_post_cal_line['licortemp_sd']
      measurement['zero_rh'] = zero_post_cal_line['rh']
      measurement['zero_rh_sd'] = zero_post_cal_line['rh_sd']
      measurement['zero_rh_temp'] = zero_post_cal_line['rhtemp']
      measurement['zero_rh_temp_sd'] = zero_post_cal_line['rhtemp_sd']

      measurement['span_pres_on'] = span_on_line['licorpress']
      measurement['span_pres_on_sd'] = span_on_line['licorpress_sd']
      measurement['span_pres_off'] = span_off_line['licorpress']
      measurement['span_pres_off_sd'] = span_off_line['licorpress_sd']
      measurement['span_pres_post'] = span_post_cal_line['licorpress']
      measurement['span_pres_post_sd'] = span_post_cal_line['licorpress_sd']
      measurement['span_pres_delta'] = \
        measurement['span_pres_post'] - measurement['span_pres_off']

      measurement['span_xco2_raw1'] = span_post_cal_line['xco2raw1']
      measurement['span_xco2_raw1_sd'] = span_post_cal_line['xco2raw1_sd']
      measurement['span_xco2_raw2'] = span_post_cal_line['xco2raw2']
      measurement['span_xco2_raw2_sd'] = span_post_cal_line['xco2raw2_sd']
      measurement['span_xco2'] = span_post_cal_line['xco2']
      measurement['span_xco2_sd'] = span_post_cal_line['xco2_sd']

      measurement['span_licor_temp'] = span_post_cal_line['licortemp']
      measurement['span_licor_temp_sd'] = span_post_cal_line['licortemp_sd']
      measurement['span_rh'] = span_post_cal_line['rh']
      measurement['span_rh_sd'] = span_post_cal_line['rh_sd']
      measurement['span_rh_temp'] = span_post_cal_line['rhtemp']
      measurement['span_rh_temp_sd'] = span_post_cal_line['rhtemp_sd']

      measurement['water_pres_on'] = equ_on_line['licorpress']
      measurement['water_pres_on_sd'] = equ_on_line['licorpress_sd']
      measurement['water_pres_off'] = equ_off_line['licorpress']
      measurement['water_pres_off_sd'] = equ_off_line['licorpress_sd']
      measurement['water_pres_delta'] = \
        measurement['water_pres_on'] - measurement['water_pres_off']

      measurement['water_xco2_raw1'] = equ_off_line['xco2raw1']
      measurement['water_xco2_raw1_sd'] = equ_off_line['xco2raw1_sd']
      measurement['water_xco2_raw2'] = equ_off_line['xco2raw2']
      measurement['water_xco2_raw2_sd'] = equ_off_line['xco2raw2_sd']
      measurement['water_xco2'] = equ_off_line['xco2']
      measurement['water_xco2_sd'] = equ_off_line['xco2_sd']

      measurement['water_licor_temp'] = equ_off_line['licortemp']
      measurement['water_licor_temp_sd'] = equ_off_line['licortemp_sd']
      measurement['water_rh'] = equ_off_line['rh']
      measurement['water_rh_sd'] = equ_off_line['rh_sd']
      measurement['water_rh_temp'] = equ_off_line['rhtemp']
      measurement['water_rh_temp_sd'] = equ_off_line['rhtemp_sd']

      measurement['atm_pres_on'] = air_on_line['licorpress']
      measurement['atm_pres_on_sd'] = air_on_line['licorpress_sd']
      measurement['atm_pres_off'] = air_off_line['licorpress']
      measurement['atm_pres_off_sd'] = air_off_line['licorpress_sd']
      measurement['atm_pres_delta'] = \
        measurement['atm_pres_on'] - measurement['atm_pres_off']

      measurement['atm_xco2_raw1'] = air_off_line['xco2raw1']
      measurement['atm_xco2_raw1_sd'] = air_off_line['xco2raw1_sd']
      measurement['atm_xco2_raw2'] = air_off_line['xco2raw2']
      measurement['atm_xco2_raw2_sd'] = air_off_line['xco2raw2_sd']
      measurement['atm_xco2'] = air_off_line['xco2']
      measurement['atm_xco2_sd'] = air_off_line['xco2_sd']

      measurement['atm_licor_temp'] = air_off_line['licortemp']
      measurement['atm_licor_temp_sd'] = air_off_line['licortemp_sd']
      measurement['atm_rh'] = air_off_line['rh']
      measurement['atm_rh_sd'] = air_off_line['rh_sd']
      measurement['atm_rh_temp'] = air_off_line['rhtemp']
      measurement['atm_rh_temp_sd'] = air_off_line['rhtemp_sd']

  return measurement

"""
Read a line from the input file, throwing an exception if it's empty
"""
def read_line(infile):
  line = infile.readline()
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
  result['measurement_type'] = fields[0]

  datestring = f'{fields[3]} {fields[4]}'
  result['time'] = datetime.strptime(datestring, '%Y/%m/%d %H:%M:%S') \
    .replace(tzinfo=timezone.utc)

  result['serial_no'] = fields[6]
  result['firmware_version'] = fields[7]
  result['firmware_date'] = datetime.strptime(fields[8], '%m/%d/%Y')

  return result

"""
Read a GPS line from the input file and return a dict
"""
def read_gps_line(infile):
  line = read_line(infile)
  fields = line.split()

  if len(fields) < 7:
    raise InvalidLineError

  result = {}

  # Detect missing GPS data
  if fields[0] == '00/00/0000':
    result['gps_time'] = ''
    result['longitude'] = math.nan
    result['latitude'] = math.nan
    result['gps_acquisition_time'] = -1
  else:
    datestring = f'{fields[0]} {fields[1]}'
    result['gps_time'] = datetime.strptime(datestring, '%m/%d/%Y %H:%M:%S') \
      .replace(tzinfo=timezone.utc)
    result['longitude'] = get_degrees(fields[4], fields[5])
    result['latitude'] = get_degrees(fields[2], fields[3])
    result['gps_acquisition_time'] = int(fields[6])

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
def read_system_line(infile):
  line = read_line(infile)
  fields = line.split()

  if len(fields) < 6:
    raise InvalidLineError

  result = {}
  result['logic_battery'] = float(fields[0])
  result['transmitter_battery'] = float(fields[1])
  result['zero_coefficient'] = float(fields[2])
  result['span_coefficient'] = float(fields[3])
  result['secondary_span_coefficient'] = float(fields[4])

  flags = fields[5]
  result['span_flag'] = int(flags[0:2], 16)
  result['zero_flag'] = int(flags[2:4], 16)

  return result

"""
Read a data line from the input file and return a dict
"""
def read_data_line(infile):
  line = read_line(infile)
  fields = line.split()

  if len(fields) != 17:
    raise InvalidLineError

  result = {}
  result['minute'] = int(fields[0])
  result['licortemp'] = float(fields[1])
  result['licortemp_sd'] = float(fields[2])
  result['licorpress'] = float(fields[3]) * 10
  result['licorpress_sd'] = float(fields[4]) * 10
  result['xco2'] = float(fields[5])
  result['xco2_sd'] = float(fields[6])
  result['rh'] = int(fields[9])
  result['rh_sd'] = float(fields[10])
  result['rhtemp'] = float(fields[11])
  result['rhtemp_sd'] = float(fields[12])
  result['xco2raw1'] = int(fields[13])
  result['xco2raw1_sd'] = int(fields[14])
  result['xco2raw2'] = int(fields[15])
  result['xco2raw2_sd'] = int(fields[16])

  return result

"""
Read the final xCO2 line and return a dict
"""
def read_xco2_line(infile):
  line = read_line(infile)
  fields = line.split()

  if len(fields) != 4:
    raise InvalidLineError

  result = {}
  result['water_xco2_dry'] = float(fields[1])
  result['atm_xco2_dry'] = float(fields[3])
  return result

"""
Write the CSV header to a file
"""
def write_header(outfile):
  outfile.write('Timestamp,Type,Longitude,Latitude,')
  outfile.write('Water Licor Pressure,Water xCO2 Dry,')
  outfile.write('Atm Licor Pressure,Atm xCO2 Dry,')
  outfile.write('Zero Flag,Span Flag,')
  outfile.write('Zero On Pressure,Zero Off Pressure,')
  outfile.write('Zero Post Pressure,Zero Pressure Delta,')
  outfile.write('Span On Pressure,Span Off Pressure,')
  outfile.write('Span Post Pressure,Span Pressure Delta,')
  outfile.write('Water On Pressure,Water Pressure Delta,')
  outfile.write('Atm On Pressure,Atm Pressure Delta,')
  outfile.write('Zero Licor Temp,Span Licor Temp,')
  outfile.write('Water Licor Temp,Atm Licor Temp,')
  outfile.write('Zero xCO2,Span xCO2,')
  outfile.write('Water xCO2 Wet,Atm xCO2 Wet,')
  outfile.write('Zero xCO2 Raw1,Zero xCO2 Raw2,')
  outfile.write('Span xCO2 Raw1,Span xCO2 Raw2,')
  outfile.write('Water xCO2 Raw1,Water xCO2 Raw2,')
  outfile.write('Atm xCO2 Raw1,Atm xCO2 Raw2,')
  outfile.write('Zero RH,Zero RH Temp,')
  outfile.write('Span RH,Span RH Temp,')
  outfile.write('Water RH,Water RH Temp,')
  outfile.write('Atm RH,Atm RH Temp,')
  outfile.write('GPS Time,GPS Acquisition Time,')
  outfile.write('Logic Battery,Transmitter Battery,')
  outfile.write('Zero Coefficient,Span Coefficient,Secondary Span Coefficient,')
  outfile.write('Water Licor Pressure SD,Atm Licor Pressure SD,')
  outfile.write('Zero On Pressure SD,Zero Off Pressure SD,Zero Post Pressure SD,')
  outfile.write('Span On Pressure SD,Span Off Pressure SD,Span Post Pressure SD,')
  outfile.write('Water On Pressure SD,Atm On Pressure SD,')
  outfile.write('Zero Licor Temp SD,Span Licor Temp SD,')
  outfile.write('Water Licor Temp SD,Atm Licor Temp SD,')
  outfile.write('Zero xCO2 SD,Span xCO2 SD,Water xCO2 SD,Atm xCO2 SD,')
  outfile.write('Zero xCO2 Raw1 SD,Zero xCO2 Raw2 SD,')
  outfile.write('Span xCO2 Raw1 SD,Span xCO2 Raw2 SD,')
  outfile.write('Water xCO2 Raw1 SD,Water xCO2 Raw2 SD,')
  outfile.write('Atm xCO2 Raw1 SD,Atm xCO2 Raw2 SD,')
  outfile.write('Zero RH SD,Zero RH Temp SD,')
  outfile.write('Span RH SD,Span RH Temp SD,')
  outfile.write('Water RH SD,Water RH Temp SD,')
  outfile.write('Atm RH SD,Atm RH Temp SD')

  outfile.write('\n')

"""
Write a measurement to a file
"""
def write_measurement(measurement, outfile):
  outfile.write(f'{measurement["timestamp"].isoformat()},')
  outfile.write(f'{measurement["type"]},')

  if math.isnan(measurement['longitude']):
    outfile.write('NaN,')
  else:
    outfile.write(f'{measurement["longitude"]:.4f},')

  if math.isnan(measurement['latitude']):
    outfile.write('NaN,')
  else:
    outfile.write(f'{measurement["latitude"]:.4f},')

  outfile.write(f'{measurement["water_pres_off"]:.2f},')
  outfile.write(f'{measurement["water_xco2_dry"]:.2f},')
  outfile.write(f'{measurement["atm_pres_off"]:.2f},')
  outfile.write(f'{measurement["atm_xco2_dry"]:.2f},')
  outfile.write(f'{measurement["zero_flag"]},')
  outfile.write(f'{measurement["span_flag"]},')
  outfile.write(f'{measurement["zero_pres_on"]:.2f},')
  outfile.write(f'{measurement["zero_pres_off"]:.2f},')
  outfile.write(f'{measurement["zero_pres_post"]:.2f},')
  outfile.write(f'{measurement["zero_pres_delta"]:.2f},')
  outfile.write(f'{measurement["span_pres_on"]:.2f},')
  outfile.write(f'{measurement["span_pres_off"]:.2f},')
  outfile.write(f'{measurement["span_pres_post"]:.2f},')
  outfile.write(f'{measurement["span_pres_delta"]:.2f},')
  outfile.write(f'{measurement["water_pres_on"]:.2f},')
  outfile.write(f'{measurement["water_pres_delta"]:.2f},')
  outfile.write(f'{measurement["atm_pres_on"]:.2f},')
  outfile.write(f'{measurement["atm_pres_delta"]:.2f},')
  outfile.write(f'{measurement["zero_licor_temp"]:.2f},')
  outfile.write(f'{measurement["span_licor_temp"]:.2f},')
  outfile.write(f'{measurement["water_licor_temp"]:.2f},')
  outfile.write(f'{measurement["atm_licor_temp"]:.2f},')
  outfile.write(f'{measurement["zero_xco2"]:.2f},')
  outfile.write(f'{measurement["span_xco2"]:.2f},')
  outfile.write(f'{measurement["water_xco2"]:.2f},')
  outfile.write(f'{measurement["atm_xco2"]:.2f},')
  outfile.write(f'{measurement["zero_xco2_raw1"]},')
  outfile.write(f'{measurement["zero_xco2_raw2"]},')
  outfile.write(f'{measurement["span_xco2_raw1"]},')
  outfile.write(f'{measurement["span_xco2_raw2"]},')
  outfile.write(f'{measurement["water_xco2_raw1"]},')
  outfile.write(f'{measurement["water_xco2_raw2"]},')
  outfile.write(f'{measurement["atm_xco2_raw1"]},')
  outfile.write(f'{measurement["atm_xco2_raw2"]},')
  outfile.write(f'{measurement["zero_rh"]},')
  outfile.write(f'{measurement["zero_rh_temp"]:.1f},')
  outfile.write(f'{measurement["span_rh"]},')
  outfile.write(f'{measurement["span_rh_temp"]:.1f},')
  outfile.write(f'{measurement["water_rh"]},')
  outfile.write(f'{measurement["water_rh_temp"]:.1f},')
  outfile.write(f'{measurement["atm_rh"]},')
  outfile.write(f'{measurement["atm_rh_temp"]:.1f},')

  if measurement['gps_time'] == '':
    outfile.write(',')
  else:
    outfile.write(f'{measurement["gps_time"].isoformat()},')

  outfile.write(f'{measurement["gps_acquisition_time"]},')
  outfile.write(f'{measurement["logic_battery"]:.1f},')
  outfile.write(f'{measurement["transmitter_battery"]:.1f},')
  outfile.write(f'{measurement["zero_coefficient"]:.6f},')
  outfile.write(f'{measurement["span_coefficient"]:.6f},')
  outfile.write(f'{measurement["secondary_span_coefficient"]:.6f},')

  # Standard Deviations
  outfile.write(f'{measurement["water_pres_off_sd"]:.1f},')
  outfile.write(f'{measurement["atm_pres_off_sd"]:.1f},')
  outfile.write(f'{measurement["zero_pres_on_sd"]:.1f},')
  outfile.write(f'{measurement["zero_pres_off_sd"]:.1f},')
  outfile.write(f'{measurement["zero_pres_post_sd"]:.1f},')
  outfile.write(f'{measurement["span_pres_on_sd"]:.1f},')
  outfile.write(f'{measurement["span_pres_off_sd"]:.1f},')
  outfile.write(f'{measurement["span_pres_post_sd"]:.1f},')
  outfile.write(f'{measurement["water_pres_on_sd"]:.1f},')
  outfile.write(f'{measurement["atm_pres_on_sd"]:.1f},')
  outfile.write(f'{measurement["zero_licor_temp_sd"]:.3f},')
  outfile.write(f'{measurement["span_licor_temp_sd"]:.3f},')
  outfile.write(f'{measurement["water_licor_temp_sd"]:.3f},')
  outfile.write(f'{measurement["atm_licor_temp_sd"]:.3f},')
  outfile.write(f'{measurement["zero_xco2_sd"]:.2f},')
  outfile.write(f'{measurement["span_xco2_sd"]:.2f},')
  outfile.write(f'{measurement["water_xco2_sd"]:.2f},')
  outfile.write(f'{measurement["atm_xco2_sd"]:.2f},')
  outfile.write(f'{measurement["zero_xco2_raw1_sd"]},')
  outfile.write(f'{measurement["zero_xco2_raw2_sd"]},')
  outfile.write(f'{measurement["span_xco2_raw1_sd"]},')
  outfile.write(f'{measurement["span_xco2_raw2_sd"]},')
  outfile.write(f'{measurement["water_xco2_raw1_sd"]},')
  outfile.write(f'{measurement["water_xco2_raw2_sd"]},')
  outfile.write(f'{measurement["atm_xco2_raw1_sd"]},')
  outfile.write(f'{measurement["atm_xco2_raw2_sd"]},')
  outfile.write(f'{measurement["zero_rh_sd"]:.1f},')
  outfile.write(f'{measurement["zero_rh_temp_sd"]:.2f},')
  outfile.write(f'{measurement["span_rh_sd"]:.1f},')
  outfile.write(f'{measurement["span_rh_temp_sd"]:.2f},')
  outfile.write(f'{measurement["water_rh_sd"]:.1f},')
  outfile.write(f'{measurement["water_rh_temp_sd"]:.2f},')
  outfile.write(f'{measurement["atm_rh_sd"]:.1f},')
  outfile.write(f'{measurement["atm_rh_temp_sd"]:.2f}')

  outfile.write('\n')


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
