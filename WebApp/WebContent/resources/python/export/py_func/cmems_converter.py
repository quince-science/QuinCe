import sys, os
import tempfile
import datetime
import numpy as np
from io import BytesIO
from zipfile import ZipFile
from netCDF4 import Dataset

TIME_BASE = datetime.datetime(1950, 1, 1, 0, 0, 0)
QC_LONG_NAME = "quality flag"
QC_CONVENTIONS = "OceanSITES reference table 2"
QC_VALID_MIN = 0
QC_VALID_MAX = 9
QC_FLAG_VALUES = [0, 1, 2, 3, 4, 5, 7, 8, 9]
QC_FLAG_MEANINGS = "no_qc_performed good_data probably_good_data " \
 + "bad_data_that_are_potentially_correctable bad_data value_changed " \
 + "not_used nominal_value interpolated_value missing_value"
DM_LONG_NAME = "method of data processing"
DM_CONVENTIONS = "OceanSITES reference table 5"
DM_FLAG_VALUES = ["R", "P", "D", "M"]
DM_FLAG_MEANINGS = "realtime post-recovery delayed-mode mixed"

def buildnetcdfs(datasetname, csv, xml):
  
  result = []

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
        result.append(makenetcdf_(dailylines))

      currentdate = linedate
      dailylines = []

    dailylines.append(csvlines[currentline])
    currentline = currentline + 1

  # Make the last netCDF
  if len(dailylines) > 0:
    result.append(makenetcdf_(dailylines))

  return result

def makenetcdf_(lines):
  filedate = getlinedate_(lines[0])
  ncbytes = None

  # Open a new netCDF file
  ncpath = tempfile.gettempdir() + "/" + filedate + ".nc"
  nc = Dataset(ncpath, format="NETCDF4", mode="w")

  # The DEPTH dimension is singular. Assume 5m for ships
  depthdim = nc.createDimension("DEPTH", 1)

  # Time, lat and lon dimensions are created per record
  timedim = nc.createDimension("TIME", None)
  timevar = nc.createVariable("TIME", "d", ("TIME"))
  timevar.long_name = "time"
  timevar.standard_name = "time"
  timevar.units = "days since 1950-01-01T00:00:00Z"
  timevar.valid_min = 0;
  timevar.valid_max = 90000;
  timevar.axis = "T"

  latdim = nc.createDimension("LATITUDE", len(lines))
  latvar = nc.createVariable("LATITUDE", "f", ("LATITUDE"))
  latvar.long_name = "Latitude of each location"
  latvar.standard_name = "latitude"
  latvar.units = "degrees_north"
  latvar.valid_min = -90
  latvar.valid_max = 90
  latvar.axis = "Y"
  latvar.reference = "WGS84"
  latvar.coordinate_reference_frame = "urn:ogc:crs:EPSG::4326"

  londim = nc.createDimension("LONGITUDE", len(lines))
  lonvar = nc.createVariable("LONGITUDE", "f", ("LONGITUDE"))
  lonvar.long_name = "Longitude of each location"
  lonvar.standard_name = "longitude"
  lonvar.units = "degrees_east"
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


  # Now we create the variables
  depthvar = nc.createVariable("DEPTH", "f", ("TIME", "DEPTH"), \
    fill_value = -9999)
  depthvar.long_name = "Depth of each measurement"
  depthvar.standard_name = "depth"
  depthvar.units = "meters"
  depthvar.positive = "down"
  depthvar.valid_min = 0
  depthvar.valid_max = 12000
  depthvar.axis = "Z"
  depthvar.reference = "sea_level"
  depthvar.coordinate_reference_frame = "urn:ogc:crs:EPSG::5113"

  positionvar = nc.createVariable("POSITIONING_SYSTEM", "c", ("TIME", "DEPTH"),\
    fill_value = " ")
  
  sstvar = nc.createVariable("TEMP", "f", ("TIME", "DEPTH"), \
    fill_value = -9999)
  sstvar.long_name = "Sea temperature"
  sstvar.standard_name = "sea_water_temperature"
  sstvar.units = "degrees_c"

  sssvar = nc.createVariable("PSAL", "f", ("TIME", "DEPTH"), \
    fill_value = -9999)
  sssvar.long_name = "Practical salinity"
  sssvar.standard_name = "sea_water_practical_salinity"
  sssvar.units = "0.001"

  pco2var = nc.createVariable("PCO2", "f", ("TIME", "DEPTH"), \
    fill_value = -9999)
  pco2var.long_name = "CO2 partial pressure"
  pco2var.standard_name = "surface_partial_pressure_of_carbon_dioxide_in_air"
  pco2var.units = "ppm"

  pco2qcvar = nc.createVariable("PCO2_QC", "b", ("TIME", "DEPTH"), \
    fill_value="-128")
  assignqcvarattributes(pco2qcvar)

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
  sstdmvar[:,:] = dms
  sssdmvar[:,:] = dms
  pco2dmvar[:,:] = dms

  # Global attributes
  nc.data_type = "OceanSITES trajectory data"
  nc.format_version = "1.2"
  nc.data_mode = "R"
  nc.geospatial_lat_min = str(minlat)
  nc.geospatial_lat_max = str(maxlat)
  nc.geospatial_lon_min = str(minlon)
  nc.geospatial_lon_max = str(maxlon)
  nc.date_update = datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")
  nc.data_assembly_centre = "BERGEN"
  nc.platform_code = " "
  nc.site_code = " "

  # Write the netCDF
  nc.close()
  
  # Read the netCDF file into memory
  with open(ncpath, "rb") as ncfile:
    ncbytes = ncfile.read()

  # Delete the temp netCDF file
  os.remove(ncpath)

  return [filedate, ncbytes]

def assigndmvarattributes_(dmvar):
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

def maketimefield_(timestr):
  timeobj = datetime.datetime(int(timestr[0:4]), int(timestr[5:7]), \
    int(timestr[8:10]), int(timestr[11:13]), int(timestr[14:16]), \
    int(timestr[17:19]))

  diff = timeobj - TIME_BASE
  return diff.days + diff.seconds / 86400

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
