
'''
Functions specific to communication with Copernicus.

Files to Copernicus must be on netcdf format, one file for each day of data.

datasetname, csv and xml is sent to buildnetcdfs. 
This splits the csv into day long segments and sends the segments to makenetcdf_
makenetcdf_ creates the netcdf-file and appends it together with the associated date 
to results, which is returned by buildnetcdfs. 
results is on the format [[date,bytes][date,bytes]].

Files are sent to Copernicus by FTP. Each put-request must include filename[expocode], bytes, destination.

'''
import pysftp
from py_func.cmems_converter import buildnetcdfs 
from io import BytesIO

# Upload result codes
UPLOAD_OK = 0
NOT_INITIALISED = 1
FILE_EXISTS = 2

def upload_to_copernicus(csv_file,xml_file,dataset_name,config_copernicus):
  results = {}
  with open(csv_file) as f: csv = f.read()
  with open(xml_file) as f: xml = f.read()

  netcdf_files = buildnetcdfs(dataset_name,csv,xml)
  ftpconn = connect_ftp(config_copernicus)
  for netcdf_file in netcdf_files:
    filename = netcdf_file[0]                 
    contents = netcdf_file[1]

    upload_result = upload_to_ftp(ftpconn,config_copernicus,filename,contents)
    
    if upload_result is 0: results[filename]='Upload ok'
    elif upload_result is 1: results[filename]='Not initialised'
    elif upload_result is 2: results[filename]='File exists'
    else: results[filename]='No response'
  return results

#Written by Steve, copied in 2018.12.11, Maren
def connect_ftp(ftp_config):
  return pysftp.Connection(host=ftp_config["server"],
      username=ftp_config["user"], password=ftp_config["password"])

 
def upload_to_ftp(ftpconn, ftp_config, filename, contents):
  upload_result = UPLOAD_OK

  destination_folder = ftp_config["dir"]
  destination_file = destination_folder +'/'+ filename

  if not ftpconn.isdir(destination_folder):
    upload_result = NOT_INITIALISED
  elif ftpconn.exists(destination_file):
    upload_result = FILE_EXISTS
  else:
    ftpconn.putfo(BytesIO(contents), destination_file)
  return upload_result



