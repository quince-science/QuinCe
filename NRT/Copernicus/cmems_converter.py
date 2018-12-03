import sys, os
import tempfile
from io import BytesIO
from zipfile import ZipFile
from netCDF4 import Dataset


def buildnetcdf(datasetname, csv, xml):
  
  result = None

  ncpath = tempfile.gettempdir() + "/" + datasetname + ".nc"
  nc = Dataset(ncpath, format="NETCDF4", mode="w")

  nc.close()
  
  with open(ncpath, "rb") as ncfile:
  	result = ncfile.read()

  return result



def main():
  zipfile = sys.argv[1]
  xmlfile = sys.argv[2]

  datasetname = os.path.splitext(zipfile)[0]
  datasetpath = datasetname + "/dataset/Copernicus/" + datasetname + ".csv"

  csv = None
  xml = None

  with ZipFile(zipfile, "r") as unzip:
  	csv = unzip.read(datasetpath)

  with open(xmlfile, "r") as xmlchan:
  	xml = xmlchan.read()

  netcdf = buildnetcdf(datasetname, csv, xml)

  with open(datasetname + ".nc", "wb") as outchan:
  	outchan.write(netcdf)




if __name__ == '__main__':
   main()
