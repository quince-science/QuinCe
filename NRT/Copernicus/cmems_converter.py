import sys, os
import tempfile
import datetime
import numpy as np
import csv
from io import BytesIO
from zipfile import ZipFile
from netCDF4 import Dataset
from xml.etree import ElementTree as ET
from re import match

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
  "34" : "vessel at fixed position"
}

def buildnetcdfs(datasetname, csv, xmlcontent):

  result = []

  # Parse the XML file
  xml = ET.fromstring(xmlcontent)

  csvlines = str.split(csv, "\n")
  currentline = 1
  currentdate = None

  dailylines = []

  while currentline < len(csvlines):
    if (csvlines[currentline].strip() == ""):
      break

    linedate = getlinedate_(csvlines[currentline])

    if linedate != currentdate:
      if currentdate is not None:
        result.append(makenetcdf_(datasetname, dailylines, xml))

      currentdate = linedate
      dailylines = []

    dailylines.append(csvlines[currentline])
    currentline = currentline + 1

  # Make the last netCDF
  if len(dailylines) > 0:
    result.append(makenetcdf_(datasetname, dailylines, xml))

  return result

def makenetcdf_(datasetname, lines, xml):
  filedate = getlinedate_(lines[0])
  ncbytes = None


  platform_code = getplatformcode_(datasetname)
  filenameroot = "GL_LATEST_TS_" + getplatformdatatype_(platform_code) + "_" \
    + platform_code + "_" + filedate

  # Open a new netCDF file
  ncpath = tempfile.gettempdir() + "/" + filenameroot + ".nc"
  nc = Dataset(ncpath, format="NETCDF4", mode="w")

  # The DEPTH dimension is singular. Assume 5m for ships
  depthdim = nc.createDimension("DEPTH", 1)

  # Time, lat and lon dimensions are created per record
  timedim = nc.createDimension("TIME", None)
  timevar = nc.createVariable("TIME", "d", ("TIME"), fill_value = 999999.0)
  timevar.long_name = "Time"
  timevar.standard_name = "time"
  timevar.units = "days since 1950-01-01T00:00:00Z"
  timevar.valid_min = -90000;
  timevar.valid_max = 90000;
  timevar.axis = "T"

  latdim = nc.createDimension("LATITUDE", len(lines))
  latvar = nc.createVariable("LATITUDE", "f", ("LATITUDE"),
    fill_value = 99999.0)

  latvar.long_name = "Latitude of each location"
  latvar.standard_name = "latitude"
  latvar.units = "degree_north"
  latvar.valid_min = -90
  latvar.valid_max = 90
  latvar.axis = "Y"
  latvar.reference = "WGS84"
  latvar.coordinate_reference_frame = "urn:ogc:crs:EPSG::4326"

  londim = nc.createDimension("LONGITUDE", len(lines))
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

  positiondim = nc.createDimension("POSITION", len(lines))

  # Record position bounds
  minlon = 180
  maxlon = -180
  minlat = 90
  maxlat = -90

  # Fill in dimension variables

  times = [0] * len(lines)
  lats = [0] * len(lines)
  lons = [0] * len(lines)
  for i in range(0, len(lines)):
    fields = str.split(lines[i], ",")
    times[i] = maketimefield_(fields[0])
    if i == 0:
      starttime = maketimeobject_(fields[0])
    elif i == len(lines) - 1:
      endtime = maketimeobject_(fields[0])

    lats[i] = float(fields[2])
    if lats[i] < minlat:
      minlat = lats[i]

    if lats[i] > maxlat:
      maxlat = lats[i]

    lons[i] = float(fields[1])
    if lons[i] < minlon:
      minlon = lons[i]

    if lons[i] > maxlon:
      maxlon = lons[i]

  timevar[:] = times
  latvar[:] = lats
  lonvar[:] = lons

  # QC flags for dimension variables. Assume all are good
  timeqcvar = nc.createVariable("TIME_QC", "b", ("TIME"), \
   fill_value = QC_FILL_VALUE)
  assignqcvarattributes_(timeqcvar)
  timeqcvar[:] = 1

  positionqcvar = nc.createVariable("POSITION_QC", "b", ("POSITION"), \
   fill_value = QC_FILL_VALUE)
  assignqcvarattributes_(positionqcvar)
  positionqcvar[:] = 1

  # Now we create the data variables
  depthvar = nc.createVariable("DEPH", "f", ("TIME", "DEPTH"), \
    fill_value = -99999.0)
  depthvar.long_name = "Depth"
  depthvar.standard_name = "depth"
  depthvar.units = "m"
  depthvar.positive = "down"
  depthvar.valid_min = -12000.0
  depthvar.valid_max = 12000.0
  depthvar.axis = "Z"
  depthvar.reference = "sea_level"
  depthvar.coordinate_reference_frame = "urn:ogc:crs:EPSG::5113"

  depthqcvar = nc.createVariable("DEPH_QC", "b", ("TIME", "DEPTH"), \
    fill_value = QC_FILL_VALUE)
  assignqcvarattributes_(depthqcvar)
  depthqcvar[:] = 7

  positionvar = nc.createVariable("POSITIONING_SYSTEM", "c", ("TIME", "DEPTH"),\
    fill_value = " ")
  positionvar.longname = "Positioning system"
  positionvar.flag_values = "A, G, L, N, U"
  positionvar.flag_meanings = "Argos, GPS, Loran, Nominal, Unknown"

  sstvar = nc.createVariable("TEMP", "f", ("TIME", "DEPTH"), \
    fill_value = -9999)
  sstvar.long_name = "Sea temperature"
  sstvar.standard_name = "sea_water_temperature"
  sstvar.units = "degrees_C"

  # SST is not explicitly QCed with a flag, so set to Unknown.
  # Once we have per-sensor QC flags we can fill this in properly
  sstqcvar = nc.createVariable("TEMP_QC", "b", ("TIME", "DEPTH"), \
   fill_value = QC_FILL_VALUE)
  assignqcvarattributes_(sstqcvar)
  sstqcvar[:] = 0

  sssvar = nc.createVariable("PSAL", "f", ("TIME", "DEPTH"), \
    fill_value = -9999)
  sssvar.long_name = "Practical salinity"
  sssvar.standard_name = "sea_water_practical_salinity"
  sssvar.units = "0.001"

  # SSS is not explicitly QCed with a flag, so set to Unknown.
  # Once we have per-sensor QC flags we can fill this in properly
  sssqcvar = nc.createVariable("PSAL_QC", "b", ("TIME", "DEPTH"), \
   fill_value = QC_FILL_VALUE)
  assignqcvarattributes_(sssqcvar)
  sssqcvar[:] = 0

  pco2var = nc.createVariable("PCO2", "f", ("TIME", "DEPTH"), \
    fill_value = -9999)
  pco2var.long_name = "CO2 partial pressure"
  pco2var.standard_name = "surface_partial_pressure_of_carbon_dioxide_in_sea_water"
  pco2var.units = "µatm"

  # We do have QC flags for the pco2
  pco2qcvar = nc.createVariable("PCO2_QC", "b", ("TIME", "DEPTH"), \
    fill_value="-128")
  assignqcvarattributes_(pco2qcvar)

  # Dimension variables for data variables
  depthdmvar = nc.createVariable("DEPH_DM", "c", ("TIME", "DEPTH"), \
    fill_value = " ")
  assigndmvarattributes_(depthdmvar)

  sstdmvar = nc.createVariable("TEMP_DM", "c", ("TIME", "DEPTH"), \
    fill_value = " ")
  assigndmvarattributes_(sstdmvar)

  sssdmvar = nc.createVariable("PSAL_DM", "c", ("TIME", "DEPTH"), \
    fill_value = " ")
  assigndmvarattributes_(sssdmvar)

  pco2dmvar = nc.createVariable("PCO2_DM", "c", ("TIME", "DEPTH"), \
    fill_value = " ")
  assigndmvarattributes_(pco2dmvar)

  # And populate them
  depths = np.empty([len(lines), 1])
  positions = np.empty([len(lines), 1], dtype="object")
  temps = np.empty([len(lines), 1])
  sals = np.empty([len(lines), 1])
  pco2s = np.empty([len(lines), 1])
  pco2qcs = np.empty([len(lines), 1])
  dms = np.empty([len(lines), 1], dtype="object")

  for i in range(0, len(lines)):
    fields = str.split(lines[i], ",")
    depths[i, 0] = 5
    positions[i, 0] = "G"
    dms[i, 0] = "R"

    if fields[3] == "":
      temps[i, 0] = -9999
    else:
      temps[i, 0] = fields[3]

    if fields[4] == "":
      sals[i, 0] = -9999
    else:
      sals[i, 0] = fields[4]

    if fields[5] == "":
      pco2s[i, 0] = -9999
    else:
      pco2s[i, 0] = fields[5]

    if len(fields[6]) == 0:
      pco2qcs[i, 0] = -128
    else:
      pco2qcs[i, 0] = makeqcvalue_(int(fields[6]))

  depthvar[:,:] = depths
  positionvar[:,:] = positions
  sstvar[:,:] = temps
  sssvar[:,:] = sals
  pco2var[:,:] = pco2s
  pco2qcvar[:,:] = pco2qcs
  depthdmvar[:,:] = dms
  sstdmvar[:,:] = dms
  sssdmvar[:,:] = dms
  pco2dmvar[:,:] = dms

  # Global attributes
  nc.id = filenameroot

  nc.data_type = "OceanSITES trajectory data"
  nc.netcdf_version = "4"
  nc.format_version = "1.2"
  nc.Conventions = "CF-1.6 OceanSITES-Manual-1.2 Copernicus-InSituTAC-SRD-1.3 "\
    + "Copernicus-InSituTAC-ParametersList-3.1.0"

  nc.cdm_data_type = "Trajectory"
  nc.data_mode = "R"
  nc.area = "Global Ocean"

  nc.geospatial_lat_min = str(minlat)
  nc.geospatial_lat_max = str(maxlat)
  nc.geospatial_lon_min = str(minlon)
  nc.geospatial_lon_max = str(maxlon)
  nc.geospation_vertical_min = "5.00"
  nc.geospation_vertical_max = "5.00"

  nc.last_latitude_observation = lats[-1]
  nc.last_longitude_observation = lons[-1]
  nc.last_date_observation = endtime.strftime("%Y-%m-%dT%H:%M:%SZ")
  nc.time_coverage_start = starttime.strftime("%Y-%m-%dT%H:%M:%SZ")
  nc.time_coverage_end = endtime.strftime("%Y-%m-%dT%H:%M:%SZ")

  datasetdate = datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")
  nc.date_update = datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")
  nc.history = datasetdate + " : Creation"

  nc.update_interval = "daily"

  nc.data_assembly_center = "BERGEN"
  nc.institution = "University of Bergen / Geophysical Institute"
  nc.institution_edmo_code = "4595"
  nc.institution_references = " "
  nc.contact = "steve.jones@uib.no"
  nc.title = "Global Ocean NRT Carbon insitu observations"
  nc.author = "cmems-service"
  nc.naming_authority = "Copernicus"

  nc.platform_code = platform_code
  nc.site_code = platform_code

  # For buoys -> Mooring observation. Can get this from the metadata xml
  # /metadata/variable[3]/observationType (replace 3 with pCO2)
  platform_category_code = getplatformcategorycode_(platform_code)
  nc.platform_name = getplatformname_(platform_code)
  nc.source_platform_category_code = platform_category_code
  nc.source = PLATFORM_CODES[platform_category_code]

  nc.quality_control_indicator = "6" # "Not used"
  nc.quality_index = "0"

  nc.comment = " "
  nc.summary = " "
  nc.reference = "http://marine.copernicus.eu/, https://www.icos-cp.eu/"
  nc.citation = "These data were collected and made freely available by the " \
    + "Copernicus project and the programs that contribute to it."
  nc.distribution_statement = "These data follow Copernicus standards; they " \
    + "are public and free of charge. User assumes all risk for use of data. " \
    + "User must display citation in any publication or product using data. " \
    + "User must contact PI prior to any commercial use of data."

  # Write the netCDF
  nc.close()

  # Read the netCDF file into memory
  with open(ncpath, "rb") as ncfile:
    ncbytes = ncfile.read()

  # Delete the temp netCDF file
  os.remove(ncpath)

  return [filenameroot, ncbytes]

def getplatformcategorycode_(platform_code):
  result = None

  with open("ship_categories.csv") as infile:
    reader = csv.reader(infile)
    lookups = {rows[0]:rows[2] for rows in reader}
    try:
      result = lookups[platform_code]
    except KeyError:
      print("PLATFORM CODE '" + platform_code + "' not found in ship categories")

  return result

def getplatformname_(platform_code):
  result = None

  with open("ship_categories.csv") as infile:
    reader = csv.reader(infile)
    lookups = {rows[0]:rows[1] for rows in reader}
    try:
      result = lookups[platform_code]
    except KeyError:
      print("PLATFORM CODE '" + platform_code + "' not found in ship categories")

  return result

def getplatformdatatype_(platform_code):
  result = None

  with open("ship_categories.csv") as infile:
    reader = csv.reader(infile)
    lookups = {rows[0]:rows[3] for rows in reader}
    try:
      result = lookups[platform_code]
    except KeyError:
      print("PLATFORM CODE '" + platform_code + "' not found in ship categories")

  return result

def assigndmvarattributes_(dmvar):
  dmvar.long_name = DM_LONG_NAME
  dmvar.conventions = DM_CONVENTIONS
  dmvar.flag_values = DM_FLAG_VALUES
  dmvar.flag_meanings = DM_FLAG_MEANINGS

def assignqcvarattributes_(qcvar):
  qcvar.long_name = QC_LONG_NAME
  qcvar.conventions = QC_CONVENTIONS
  qcvar.valid_min = QC_VALID_MIN
  qcvar.valid_max = QC_VALID_MAX
  qcvar.flag_values = QC_FLAG_VALUES
  qcvar.flag_meanings = QC_FLAG_MEANINGS

def maketimefield_(timestr):
  timeobj = maketimeobject_(timestr)

  diff = timeobj - TIME_BASE
  return diff.days + diff.seconds / 86400

def maketimeobject_(timestr):
  timeobj = datetime.datetime(int(timestr[0:4]), int(timestr[5:7]), \
    int(timestr[8:10]), int(timestr[11:13]), int(timestr[14:16]), \
    int(timestr[17:19]))

  return timeobj


def makeqcvalue_(flag):
  result = 9 # Missing

  if flag == 2:
    result = 1
  elif flag == 3:
    result = 2
  elif flag == 4:
    result = 4
  else:
    raise ValueError("Unrecognised flag value " + flag)

  return result

def getlinedate_(line):
  datefield = str.split(line, ",")[0]
  return datefield[0:4] + datefield[5:7] + datefield[8:10]

def getplatformcode_(datasetname):
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
  zipfile = sys.argv[1]
  xmlfile = sys.argv[2]

  datasetname = os.path.splitext(zipfile)[0]
  datasetpath = datasetname + "/dataset/Copernicus/" + datasetname + ".csv"

  csv = None
  xml = None

  with ZipFile(zipfile, "r") as unzip:
  	csv = unzip.read(datasetpath).decode("utf-8")

  with open(xmlfile, "r") as xmlchan:
    xml = xmlchan.read()

  netcdfs = buildnetcdfs(datasetname, csv, xml)

  for i in range(0, len(netcdfs)):
    with open(netcdfs[i][0] + ".nc", "wb") as outchan:
      outchan.write(netcdfs[i][1])

if __name__ == '__main__':
   main()
