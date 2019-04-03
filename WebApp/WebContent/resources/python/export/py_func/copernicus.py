
'''
Functions specific to communication with Copernicus.

Files to Copernicus must be on netcdf format, one file for each day of data.

datasetname, csv and xml is sent to buildnetcdfs.
This splits the csv into day long segments and sends the segments to makenetcdf_
makenetcdf_ creates the netcdf-file and appends it together with the associated date
to results, which is returned by buildnetcdfs.
results is on the format [[date,bytes][date,bytes]].

Files are sent to Copernicus by FTP.
Each put-request must include filename[expocode], bytes, destination.

The Copernicus FTP requires an Index file and a DNT file describing all
files uploaded to the server to complete the ingestion.
The index file reflects all the files in the FTP folder.
The DNT file triggers the ingestion.
A DNT file must be submitted for the index-file as well.

Example of DNT file format provided by mail from Antoine.Queric@ifremer.fr 2019-03-07
<?xml version="1.0" ?>
<delivery PushingEntity="CopernicusMarine-InSitu-Global" date="20190306T070107Z" product="INSITU_GLO_CARBON_NRT_OBSERVATIONS_013_049">
  <dataset DatasetName="NRT">
    <file Checksum="936999b6a47731e8aa763ec39b3af641" FileName="latest/20190306/A.nc" FinalStatus="Delivered" StartUploadTime="20190306T070107Z" StopUploadTime="20190306T070107Z"/>
    <file Checksum="d763859d86284add3395067fe9f8e3a0" FileName="latest/20190306/B.nc" FinalStatus="Delivered" StartUploadTime="20190306T070108Z" StopUploadTime="20190306T070108Z"/>

    <file FileName="latest/20190306/C.nc">
      <KeyWord>Delete</KeyWord>
    </file>

  </dataset>
</delivery>

Example of index file format provided by Corentin.Guyot@ifremer.fr 2019-03-06
# Title : Carbon in-situ observations catalog
# Description : catalog of available in-situ observations per platform.
# Project : Copernicus
# Format version : 1.0
# Date of update : 20190305080103
# catalog_id,file_name,geospatial_lat_min,geospatial_lat_max,geospatial_lon_min,geospatial_lon_max,time_coverage_start,time_coverage_end,provider,date_update,data_mode,parameters
COP-GLOBAL-01,ftp://nrt.cmems-du.eu/Core/INSITU_GLO_CARBON_NRT_OBSERVATIONS_013_049/nrt/latest/20190221/GL_LATEST_PR_BA_7JXZ_20190221.nc,19.486,19.486,-176.568,-176.568,2019-02-21T17:50:00Z,2019-02-21T17:50:00Z,Unknown institution,2019-02-24T04:10:11Z,R,DEPH TEMP
'''
import logging
import pysftp
import ftplib
import ftputil
import os
import sys
import hashlib
import datetime
import netCDF4
from py_func.cmems_converter import buildnetcdfs

import xml.etree.ElementTree as ET
import sqlite3 as lite
import json
import time

from py_func.meta_handling import get_file_from_zip

# Upload result codes
UPLOAD_OK = 0
NOT_INITIALISED = 1
FILE_EXISTS = 2

dnt_datetime_format = '%Y%m%dT%H%M%SZ'
server_location = 'ftp://nrt.cmems-du.eu/Core'
index_dir = '/INSITU_GLO_CARBON_NRT_OBSERVATIONS_013_049/NRT_201904'
nrt_dir='/INSITU_GLO_CARBON_NRT_OBSERVATIONS_013_049/NRT_201904/latest'
dnt_dir='/INSITU_GLO_CARBON_NRT_OBSERVATIONS_013_049/DNT'
my_dir=''

nc_nan = 'tmp_nan_update.nc'
log_latest = 'cmems_uploads_latest.json'
log_file = 'cmems_log.txt'

#con = lite.connect('uploaded_files.db')
#cur = con.cursor()
#cur.execute(''' CREATE TABLE  uploads
#  (date text, filename text, time_coverage_start text, time_coverage_end text, hashsum text)''')


def send_to_copernicus(
  filename,dataset_zip,dataset_name,destination,ftp_config,server,
  delete_file=False):
  '''
  connects to copernicus server, creates and uploads netCDF4-files and
  corresponding dnt-file

  csv_file: file retrieved from QuinCe
  dataset_name: dataset name
  ftp_config contains login-information, server-information and filepaths.
  server denotes if the file should be uploaded to the 'near real time'-server
  or the 'multi year'-server.
  delete_file indicates that the file should be deleted from the Copernicus server,
  triggers the sending of a DNT 'delete'-file.

  returns: upload results

  '''

  csv_file = get_file_from_zip(dataset_zip, dataset_name
     + '/dataset/Copernicus/' + destination)
#  csv_file = 'NRTNRTA1542040749136.csv'

  logging.info(
  'Creating netcdf-files based on {:s} to send to Copernicus'
  .format(filename))

  folder_date = (datetime.datetime.now().strftime("%Y%m%d_%H%M%S"))
  try:
    os.mkdir(folder_date)
  except: pass
  uploaded_to_ftp = True
  upload_response_log = ''
  dnt_upload_list=[]
  results = {}
  hashsum_no_date={}
  files_uploaded = False
  with open(csv_file) as f:
    csv = f.read()
    nc_files = buildnetcdfs(dataset_name,csv)

    with ftputil.FTPHost(
    host=ftp_config['Copernicus'][server],
    user=ftp_config['Copernicus']['user'],
    passwd=ftp_config['Copernicus']['password'])as ftp:

      logging.info(
      'Sending netcdf-files based on {:s} to Copernicus'
      .format(filename))
      ftp_dir = ftp.listdir

      for nc_file in nc_files:  #nc_file = [filename, bytes]
        nc_filename = nc_file[0]
        nc_content = nc_file[1]
        nc_filepath = folder_date + '/' + nc_filename + '.nc'
        with open(nc_filepath,'wb') as f: f.write(nc_content)
        #with open(folder_date+'/' + nc_nan,'wb') as f: f.write(nc_content)

        hashsum_no_date[nc_filename] = get_netCDF_hashsum_no_date(nc_filepath)

        try:
          with open(log_latest,'r') as log:
            uploads_latest = eval(log.read())

            if nc_filename in uploads_latest:
              logging.debug('filename already uploaded')
              logging.debug('old checksum: ' + str(uploads_latest[nc_filename]))
              logging.debug('new checksum: ' + str(hashsum_no_date[nc_filename]))
              if uploads_latest[nc_filename] == hashsum_no_date[nc_filename]:
                logging.debug('hashsum already uploaded, skipping to next file')
                continue
        except:
          logging.debug('no log of prior uploads found')
          uploads_latest = {}

        #assigning date_update and history
        nc = netCDF4.Dataset(nc_filepath,mode = 'a')
        datasetdate = datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")
        nc.date_update = datasetdate
        nc.history = datasetdate + " : Creation"
        nc.close()

        upload_result, ftp_filepath, start_upload_time, stop_upload_time \
          = upload_to_ftp(ftp,ftp_config,nc_filename, nc_filepath)

        if upload_result is 0:
          results[nc_filename]='netCDF upload ok '
          files_uploaded = True
        else:
          results[nc_filename]='no netCDF uploaded'
          uploaded_to_ftp = False

        if upload_result is 0 or delete_file == True:
          ftp_filepath = ftp_filepath.split('NRT_201904/')[-1]
          #dnt_folder = ftp_config['Copernicus']['dnt_dir']
          dnt_filename, dnt_filepath = DNT_create(
            ftp_filepath, start_upload_time, stop_upload_time, folder_date,
            delete_file)

            # If a DNT file with the same timestamp/filename already exists,
            # create a new DNT-file.
          dnt_list = ftp.listdir(dnt_dir)
          if dnt_filename in dnt_list:
            logging.debug('Duplicate DNT filename occured')
            dnt_filename, dnt_filepath = \
            DNT_create(ftp_filepath,start_upload_time,
              stop_upload_time,delete_file)

          dnt_upload_list += [[nc_filename,
                               dnt_filepath,
                               dnt_dir+'/'+dnt_filename]]

        elif upload_result is 2:
          results[nc_filename] = 'netCDF file exists'
        else:
          results[nc_filename] = 'netCDF: No response'
          uploaded_to_ftp = False

      index_status = index_file(ftp,dnt_dir,folder_date,results)
      if 'failed' in index_status:
        uploaded_to_ftp = False

      for nc_filename, dnt_source, dnt_target in dnt_upload_list:
        try:
          ftp.upload(dnt_source,dnt_target)
          results[nc_filename] += ', DNT upload ok'
        except:
          results[nc_filename] += ', DNT: No response'
          logging.debug('DNT: No response')

      # get response file and generate upload-log and report-log
      if files_uploaded == True:
        upload_response_log, uploads_latest = evaluate_response_file(
          dnt_upload_list,ftp,folder_date,
          upload_response_log,uploads_latest,hashsum_no_date)
      else:
        logging.info('No files uploaded to cmems')

    logging.debug('Copernicus upload results: ' + str(results))
    logging.debug('index status: '+ str(index_status))


  exists = os.path.isfile(log_file)
  if exists:
    with open(log_file,'a') as log: log.write(upload_response_log)
  else:
    with open(log_file,'w') as log:
      log.write('upload_date, netCDF filename, DNT filename, DNT response \n')
      log.write(upload_response_log)

  with open(log_latest,'w') as log:
    json.dump(uploads_latest,log)

  if uploaded_to_ftp:
    return dataset_name + ': successfully uploaded to the CMEMS FTP server'
  else:
    return dataset_name + ': failed correct upload procedure to the CMEMS FTP server'


def upload_to_ftp(ftp, ftp_config, filename, filepath):
  ''' Uploads file with location 'filepath' to an ftp-server,
  server-location set by 'directory' parameter and config-file,
  ftp is the ftp-connection

  returns
  upload_result: upload_ok or file_exists
  dest_filepath: target filepath on ftp-server
  start_upload_time and stop_upload_time: timestamps of upload process
  '''
  upload_result = UPLOAD_OK

  date = filename.split('_')[-1]
  dest_folder = nrt_dir+ '/' + date
  dest_filepath = dest_folder + '/' + filename + '.nc'

  start_upload_time = datetime.datetime.now().strftime(dnt_datetime_format)
  if not ftp.path.isdir(dest_folder):
    ftp.mkdir(dest_folder)
    ftp.upload(filepath, dest_filepath)
  elif ftp.path.isfile(dest_filepath):
    upload_result = FILE_EXISTS
  else:
    ftp.upload(filepath, dest_filepath)
  stop_upload_time = datetime.datetime.now().strftime(dnt_datetime_format)

  return upload_result, dest_filepath, start_upload_time, stop_upload_time


def DNT_create(
  filepath,start_upload_time,stop_upload_time,folder_date,delete_file=False):
  ''' Generates delivery note for NetCDF file upload,
  note needed by Copernicus in order to move .nc-file to public-ftp
  filepath is name of file that has been uploaded to the server
  start_upload_time and stop_upload_time are the timestamps associated with
  file upload
  returns the filename and filepath of the dnt-file

  The parameter delete_file can be set to True or False.
  If set to True, the dnt file will request the deletion of filename.

  '''
  product_id = 'INSITU_GLO_CARBON_NRT_OBSERVATIONS_013_049'
  date = datetime.datetime.now().strftime(dnt_datetime_format)

  with open(folder_date +'/'+ filepath.split('/')[-1],'rb') as f:
    nc_bytes = f.read()

  dnt = ET.Element('delivery')
  dnt.set('PushingEntity','CopernicusMarine-InSitu-Global')
  dnt.set('date', date)
  dnt.set('product',product_id)
  dataset = ET.SubElement(dnt,'dataset')
  dataset.set('DatasetName','NRT_201904')
  if delete_file == True:
    file_del = ET.SubElement(dataset,'file')
    file_del.set('FileName',filepath)
    key_word = ET.SubElement(file_del,'KeyWord')
    key_word.text = 'Delete'
  else:
    file = ET.SubElement(dataset,'file')
    file.set('Checksum',hashlib.md5(nc_bytes).hexdigest())
    file.set('FileName',filepath)
    file.set('FinalStatus','Delivered')
    file.set('StartUploadTime',start_upload_time)
    file.set('StopUploadTime',stop_upload_time)

  xml_tree = ET.ElementTree(dnt)

  logging.debug('DNT file:\n' + str(ET.dump(xml_tree)))

  dnt_file = product_id + '_P' + date + '.xml'
  dnt_filepath = folder_date + '/' + dnt_file

  with open(dnt_filepath,'wb') as xml:
    xml_tree.write(xml,xml_declaration=True,method='xml')

  return dnt_file, dnt_filepath


def index_file(ftp,dnt_dir,folder_date,upload_result):
  '''
  Creates index file to be updated in CMEMS FTP-server
  Describes all files uploaded to the FTP folder during this session
  requires an ftp connection, the local folder for the current session
  and the upload status of the files associated with this session.
  returns the success/failure status of the index-upload-procedure
  '''
  index_status = 'index upload ok'
  index_info = ''

  dir_list = ftp.listdir(index_dir+'/latest')

  date_header = datetime.datetime.now().strftime('%Y%m%d%H%M%S')
  index_header = ('# Title : Carbon in-situ observations catalog \n'\
    + '# Description : catalog of available in-situ observations per platform.\n'\
    + '# Project : Copernicus \n# Format version : 1.0 \n'\
    + '# Date of update : ' + date_header +'\n'
    + '# catalog_id,file_name,geospatial_lat_min,geospatial_lat_max,'\
    + 'geospatial_lon_min,geospatial_lon_max,time_coverage_start,'\
    + 'time_coverage_end,provider,date_update,data_mode,parameters\n')

  for file, result  in upload_result.items():
    if 'upload ok' in result:
      file_path = folder_date +'/' + file + '.nc'
      nc = netCDF4.Dataset(file_path,mode='r')

      lat_min = nc.geospatial_lat_min
      lat_max = nc.geospatial_lat_max
      lon_min = nc.geospatial_lon_min
      lon_max = nc.geospatial_lon_max
      time_start = nc.time_coverage_start
      time_end  = nc.time_coverage_end
      date_update = nc.date_update

      #get list of parameters from netCDF file
      var_list = nc.variables.keys()
      var_list = list(filter(lambda x: '_' not in x, var_list))
      var_list = list(filter(lambda x: 'TIME' not in x, var_list))
      var_list = list(filter(lambda x: 'LATITUDE' not in x, var_list))
      var_list = list(filter(lambda x: 'LONGITUDE' not in x, var_list))
      nc.close()
      #reformat to index-format: string of parameters separated by space
      parameters = ''
      for item in var_list:
        parameters += item +' '
      parameters = parameters[:-1] #removes final space

      index_info += ('COP-GLOBAL-01' + ',' + (server_location + file) + ','
                  + lat_min + ',' + lat_max + ',' + lon_min + ',' + lon_max
                  + ',' + time_start + ',' + time_end + ','
                  + 'University of Bergen Geophysical Institute' + ','
                  + date_update + ',' + 'R' + ',' + parameters + '\n')

  index_latest = index_header + index_info
  index_filename = folder_date +'/index_latest.txt'
  with open(index_filename,'w') as f: f.write(index_latest)

  ftp_index_location = index_dir+'/index_latest.txt'
  logging.debug('index file:\n'+index_latest)

  try:
    start_upload_time = datetime.datetime.now().strftime(dnt_datetime_format)
    ftp.upload(index_filename, ftp_index_location)
    stop_upload_time = datetime.datetime.now().strftime(dnt_datetime_format)

    try:
      dnt_file, dnt_filepath = DNT_create(
      index_filename.split('/')[-1],
      start_upload_time,
      stop_upload_time,
      folder_date)

      ftp.upload(dnt_filepath,dnt_dir+'/'+dnt_file)

    except:
      index_status = 'index dnt upload failed'

  except:
    index_status = 'index dnt upload failed'
  return index_status

def get_response(ftp,dnt_filename,folder_date):
  '''
  Function to retrieve the status of any file uploaded to CMEMS server

  requires login information and the filename of the DNT associated with the upload.
  returns the string of the xml responsefile generated by the CMEMS server.
  '''
  source = dnt_filename.split('.')[0].replace('DNT','DNT_response') + '_response.xml'
  target = folder_date + '/' +  source.split('/')[-1]

  ftp.download(source,target)

  with open(target,'r') as response_file:
    response = response_file.read()
  return response

def evaluate_response_file(dnt_upload_list,ftp,folder_date,upload_response_log,uploads_latest,hashsum_no_date):
  logging.debug('waiting for response_files')

  response_received = False
  loop_iter = 0
  while response_received == False and loop_iter < 50 :
    loop_iter += 1
    time.sleep(10)
    logging.debug('waiting ' + str(loop_iter*10))
    try:
      for nc_filename, dnt_source, dnt_target in dnt_upload_list:
        try:
          cmems_response = get_response(ftp,dnt_target,folder_date)
          logging.debug('cmems response: ' + cmems_response)
          upload_response_log += (
            folder_date + ',' + nc_filename + ','
            + dnt_source + ',' + cmems_response + '\n' )
          if 'Ingested="True"' in cmems_response:
            path = os.getcwd()
            nc_path = (folder_date + '/' + nc_filename + '.nc')

            uploads_latest[nc_filename] = hashsum_no_date[nc_filename]
          response_received = True
        except:
          response_received = False
    except:
      response_received = False
  if response_received == False: logging.info('no response from cmems')

  return upload_response_log, uploads_latest

def get_netCDF_hashsum_no_date(nc_path):
  with open(nc_path,'rb') as f:
    nc_bytes = f.read()
    hashsum_no_date =  hashlib.md5(nc_bytes).hexdigest()
  return hashsum_no_date
