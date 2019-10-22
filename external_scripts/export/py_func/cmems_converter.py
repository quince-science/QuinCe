import sys, os
import tempfile
import datetime
import pandas as pd
import numpy as np
import numpy.ma as ma
import csv, json
from math import isnan
from io import BytesIO
from zipfile import ZipFile
from netCDF4 import Dataset
from re import match

# Columns in ship_categories.csv
NAME = 1
CATEGORY_CODE = 2
DATA_TYPE = 3
CALLSIGN = 4
AUTHOR_LIST = 5
YEAR = datetime.datetime.now().year

TIME_BASE = datetime.datetime(1950, 1, 1, 0, 0, 0)
QC_LONG_NAME = "quality flag"
QC_CONVENTIONS = "OceanSITES reference table 2"
QC_VALID_MIN = 0
QC_VALID_MAX = 9
QC_FILL_VALUE = -128
QC_FLAG_VALUES = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
QC_FLAG_MEANINGS = "no_qc_performed good_data probably_good_data " \
 + "bad_data_that_are_potentially_correctable bad_data value_changed " \
 + "not_used nominal_value interpolated_value missing_value"
DM_LONG_NAME = "method of data processing"
DM_CONVENTIONS = "OceanSITES reference table 5"
DM_FLAG_VALUES = "R, P, D, M"
DM_FLAG_MEANINGS = "real-time provisional delayed-mode mixed"

PLATFORM_CODES = {
  "31" : "research vessel",
  "32" : "vessel of opportunity",
  "41" : "moored surface buoy"
}

def buildnetcdfs(datasetname, fieldconfig, filedata):
  ''' Construct CMEMS complient netCDF files from filedata'''

  result = []
  currentline = 0
  currentdate = None
  daystartline = currentline
  dayendline = None

  while currentline < filedata.shape[0]:
    linedate = getlinedate(filedata.iloc[[currentline]])
    if linedate != currentdate:
      if currentdate:
        result.append(makenetcdf(datasetname, fieldconfig, filedata[daystartline:dayendline + 1]))

      currentdate = linedate
      daystartline = currentline

    dayendline = currentline
    currentline = currentline + 1

  # Make the last netCDF
  if dayendline:
    result.append(makenetcdf(datasetname, fieldconfig, filedata[daystartline:dayendline+1]))

  return result

def makenetcdf(datasetname, fieldconfig, records):
  filedate = getlinedate(records.iloc[[0]])
  ncbytes = None

  platform_code = getplatformcode(datasetname)
  filenameroot = "GL_TS_TS_" + getplatformvalue(platform_code, CALLSIGN) + "_" + str(filedate)

  # Open a new netCDF file
  ncpath = tempfile.gettempdir() + "/" + filenameroot + ".nc"
  nc = Dataset(ncpath, format="NETCDF4_CLASSIC", mode="w")

  # The DEPTH dimension is singular. Assume 5m for ships
  depthdim = nc.createDimension("DEPTH", 1)

  # Time, lat and lon dimensions are created per record
  timedim = nc.createDimension("TIME", records.shape[0])
  timevar = nc.createVariable("TIME", "d", ("TIME"), fill_value = 999999.0)
  timevar.long_name = "Time"
  timevar.standard_name = "time"
  timevar.units = "days since 1950-01-01T00:00:00Z"
  timevar.valid_min = -90000;
  timevar.valid_max = 90000;
  timevar.axis = "T"

  latdim = nc.createDimension("LATITUDE", records.shape[0])
  latvar = nc.createVariable("LATITUDE", "f", ("LATITUDE"), fill_value = 99999.0)

  latvar.long_name = "Latitude of each location"
  latvar.standard_name = "latitude"
  latvar.units = "degree_north"
  latvar.valid_min = -90
  latvar.valid_max = 90
  latvar.axis = "Y"
  latvar.reference = "WGS84"
  latvar.coordinate_reference_frame = "urn:ogc:crs:EPSG::4326"

  londim = nc.createDimension("LONGITUDE", records.shape[0])
  lonvar = nc.createVariable("LONGITUDE", "f", ("LONGITUDE"),
    fill_value = 99999.0)

  lonvar.long_name = "Longitude of each location"
  lonvar.standard_name = "longitude"
  lonvar.units = "degree_east"
  lonvar.valid_min = -180
  lonvar.valid_max = 180
  lonvar.axis = "X"
  lonvar.reference = "WGS84"
  lonvar.coordinate_reference_frame = "urn:ogc:crs:EPSG::4326"

  positiondim = nc.createDimension("POSITION", records.shape[0])

  # Fill in dimension variables
  timevar[:] = records['Timestamp'].apply(maketimefield).to_numpy()
  latvar[:] = records['ALATGP01'].to_numpy()
  lonvar[:] = records['ALONGP01'].to_numpy()

  # QC flags for dimension variables. Assume all are good
  timeqcvar = nc.createVariable("TIME_QC", "b", ("TIME"), \
   fill_value = QC_FILL_VALUE)
  assignqcvarattributes(timeqcvar)
  timeqcvar[:] = 1

  positionqcvar = nc.createVariable("POSITION_QC", "b", ("POSITION"), \
   fill_value = QC_FILL_VALUE)
  assignqcvarattributes(positionqcvar)
  positionqcvar[:] = 1

  positionvar = nc.createVariable("POSITIONING_SYSTEM", "c", ("TIME", "DEPTH"),\
    fill_value = " ")
  positionvar.longname = "Positioning system"
  positionvar.flag_values = "A, G, L, N, U"
  positionvar.flag_meanings = "Argos, GPS, Loran, Nominal, Unknown"

  positions = np.empty([records.shape[0], 1], dtype="object")
  positions[:,0] = "G"

  positionvar[:,:] = positions

  # DM values
  dms = np.empty([records.shape[0], 1], dtype="object")
  dms[:,0] = "R"


  # Fields
  for index, field in fieldconfig.iterrows():
    var = nc.createVariable(field['netCDF Name'], field['Data Type'], \
      ("TIME", "DEPTH"), fill_value=field['FillValue'])

    attributes = json.loads(field['Attributes'])
    for key, value in attributes.items():
      var.setncattr(key, value)

    # Read the data values
    datavalues = records[field['Export Column']].to_numpy()

    # Calculate QC values
    qc_values = np.empty([records.shape[0], 1])
    if field['QC'] == "Data":
      qc_values[:,0] = makeqcvalues(datavalues, records[field['Export Column'] + '_QC'].to_numpy())
    else:
      qc_values[:,0] = field['QC']


    if not isnan(field['add_offset']):
      var.setncattr('add_offset', field['add_offset'])
      var.setncattr('scale_factor', field['scale_factor'])

      # The netCDF library detects FillValues *after* it's done the packing step.
      # So we replace the NaN values so that the packing will get the correct FillValue
      datavalues[np.isnan(datavalues)] = float(field['FillValue']) * float(field['scale_factor'])

    varvalues = np.empty([records.shape[0], 1])
    varvalues[:,0] = datavalues
    var[:,:] = varvalues

    # DM variable
    dmvar = nc.createVariable(field['netCDF Name'] + '_DM', 'c', ("TIME", "DEPTH"), fill_value=' ')
    assigndmvarattributes(dmvar)
    dmvar[:,:] = dms

    qcvar = nc.createVariable(field['netCDF Name'] + '_QC', "b", ("TIME", "DEPTH"), \
      fill_value = QC_FILL_VALUE)

    assignqcvarattributes(qcvar)
    qcvar[:,:] = qc_values

  # Global attributes
  nc.id = filenameroot

  nc.data_type = "OceanSITES trajectory data"
  nc.netcdf_version = "netCDF-4 classic model"
  nc.format_version = "1.2"
  nc.Conventions = "CF-1.6 OceanSITES-Manual-1.2 Copernicus-InSituTAC-SRD-1.3 "\
    + "Copernicus-InSituTAC-ParametersList-3.1.0"

  nc.cdm_data_type = "Trajectory"
  nc.data_mode = "R"
  nc.area = "Global Ocean"

  nc.geospatial_lat_min = str(min(latvar))
  nc.geospatial_lat_max = str(max(latvar))
  nc.geospatial_lon_min = str(min(lonvar))
  nc.geospatial_lon_max = str(max(lonvar))
  nc.geospatial_vertical_min = "5.00"
  nc.geospatial_vertical_max = "5.00"
  nc.last_latitude_observation = str(records['ALATGP01'].iloc[[-1]].to_numpy()[0])
  nc.last_longitude_observation = str(records['ALONGP01'].iloc[[-1]].to_numpy()[0])
  nc.last_date_observation = records['Timestamp'].iloc[[-1]].to_numpy()[0]
  nc.time_coverage_start = records['Timestamp'].iloc[[0]].to_numpy()[0]
  nc.time_coverage_end = records['Timestamp'].iloc[[-1]].to_numpy()[0]

  nc.update_interval = "daily"

  nc.data_assembly_center = "BERGEN"
  nc.institution = "University of Bergen / Geophysical Institute"
  nc.institution_edmo_code = "4595"
  nc.institution_references = " "
  nc.contact = "steve.jones@uib.no"
  nc.title = "Global Ocean - In Situ near-real time carbon observation"
  nc.author = "cmems-service"
  nc.naming_authority = "Copernicus"
  nc.citation = AUTHOR_LIST + "(" + YEAR + "): NRT data from [platform name]. "\
    + "Made available through the Copernicus project."

  nc.platform_code = getplatformvalue(platform_code, CALLSIGN)
  nc.site_code = getplatformvalue(platform_code, CALLSIGN)

  # For buoys -> Mooring observation.
  platform_category_code = getplatformvalue(platform_code, CATEGORY_CODE)
  nc.platform_name = getplatformvalue(platform_code, NAME)
  nc.source_platform_category_code = platform_category_code
  nc.source = PLATFORM_CODES[platform_category_code]

  nc.quality_control_indicator = "6" # "Not used"
  nc.quality_index = "0"

  nc.comment = " "
  nc.summary = " "
  nc.reference = "http://marine.copernicus.eu/, https://www.icos-cp.eu/"
  #nc.citation = "These data were collected and made freely available by the " \
  #  + "Copernicus project and the programs that contribute to it."
  nc.citation = (getplatformvalue(platform_code, AUTHOR_LIST) + "(" + str(YEAR) 
    + "): NRT data from " + getplatformvalue(platform_code, NAME) +  
    ". Made available through the Copernicus project.")
  nc.distribution_statement = ("These data follow Copernicus standards; they " 
    + "are public and free of charge. User assumes all risk for use of data. " 
    + "User must display citation in any publication or product using data. " 
    + "User must contact PI prior to any commercial use of data."

  # Write the netCDF
  nc.close()

  # Read the netCDF file into memory
  with open(ncpath, "rb") as ncfile:
    ncbytes = ncfile.read()

  # Delete the temp netCDF file
  os.remove(ncpath)

  return [filenameroot, ncbytes]

def makeqcvalues(values, qc):

  result = np.empty(len(values))
  for i in range(0, len(values)):
    if isnan(values[i]):
      result[i] = 9
    else:
      result[i] = makeqcvalue(qc[i])

  return result

def getplatformvalue(platform_code,value):
  result = None

  with open("ship_categories.csv") as infile:
    reader = csv.reader(infile)
    lookups = {rows[0]:rows[value] for rows in reader}
    try:
      result = lookups[platform_code]
    except KeyError:
      logging.error(f"PLATFORM CODE '{platform_code}' not found in ship categories")

  return result

def assigndmvarattributes(dmvar):
  dmvar.long_name = DM_LONG_NAME
  dmvar.conventions = DM_CONVENTIONS
  dmvar.flag_values = DM_FLAG_VALUES
  dmvar.flag_meanings = DM_FLAG_MEANINGS

def assignqcvarattributes(qcvar):
  qcvar.long_name = QC_LONG_NAME
  qcvar.conventions = QC_CONVENTIONS
  qcvar.valid_min = QC_VALID_MIN
  qcvar.valid_max = QC_VALID_MAX
  qcvar.flag_values = QC_FLAG_VALUES
  qcvar.flag_meanings = QC_FLAG_MEANINGS

def maketimefield(timestr):
  timeobj = maketimeobject(timestr)

  diff = timeobj - TIME_BASE
  return diff.days + diff.seconds / 86400

def maketimeobject(timestr):
  timeobj = datetime.datetime(int(timestr[0:4]), int(timestr[5:7]), \
    int(timestr[8:10]), int(timestr[11:13]), int(timestr[14:16]), \
    int(timestr[17:19]))

  return timeobj

def makeqcvalue(flag):
  result = 9 # Missing

  if flag == 2:
    result = 2
  elif flag == 3:
    result = 2
  elif flag == 4:
    result = 4
  else:
    raise ValueError("Unrecognised flag value " + str(flag))

  return result

def getlinedate(line):
  return pd.to_datetime(line.Timestamp).iloc[0].date().strftime('%Y%m%d')

def getplatformcode(datasetname):
  platform_code = None

  # NRT data sets
  # Named as NRT[Platform][milliseconds]
  # Milliseconds is currently a 13 digit number. At the time of writing it
  # will be ~316 years before that changes.
  matched = match("^NRT(.....*)[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]$",
    datasetname)
  if matched is None:
    # Normal data sets - standard EXPO codes
    matched = match("^(.....*)[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]$",
      datasetname)

  if matched is None:
    raise ValueError("Cannot parse dataset name")
  else:
    platform_code = matched.group(1)

  return platform_code


def main():
  fieldconfig = pd.read_csv('fields.csv', delimiter=',', quotechar='\'')

  zipfile = sys.argv[1]

  datasetname = os.path.splitext(zipfile)[0]
  datasetpath = datasetname + "/dataset/Copernicus/" + datasetname + ".csv"

  filedata = None

  with ZipFile(zipfile, "r") as unzip:
    filedata = pd.read_csv(BytesIO(unzip.read(datasetpath)), delimiter=',')

  filedata['Timestamp'] = filedata['Timestamp'].astype(str)

  netcdfs = buildnetcdfs(datasetname, fieldconfig, filedata)

  for i in range(0, len(netcdfs)):
    with open(netcdfs[i][0] + ".nc", "wb") as outchan:
      outchan.write(netcdfs[i][1])

if __name__ == '__main__':
   main()
