
'''
Functions specific to communication with Copernicus.

Files to Copernicus must be on netcdf format.

Files are sent to Copernicus by FTP. 

The Copernicus FTP requires an Index file and a DNT file describing all 
files uploaded to the server to complete the ingestion. 
The index file reflects all the files in the FTP folder.
The DNT file triggers the ingestion. 

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

To delete en empty directory, you can use the following syntax inside your DNT file :

        <directory DestinationFolderName="" SourceFolderName="directoryName">
          <KeyWord>Delete</KeyWord>
        </directory>


To move an existing file, you can use following syntax in your DNT file :

        <file Checksum="fileChecksum" FileName="path/to/existing/file.nc" NewFileName="path/to/new_folder/file.nc">
            <KeyWord>Move</KeyWord>
        </file>

'''
import logging 
import ftputil 
import os
import sys
import re
import hashlib
import datetime
import pandas as pd
import numpy as np
import netCDF4
from py_func.cmems_converter import buildnetcdfs 

import xml.etree.ElementTree as ET
import sqlite3
import json
import time

from py_func.carbon import get_file_from_zip

# Upload result codes
UPLOAD_OK = 0
FILE_EXISTS = 2

# Response codes
UPLOADED = 1
NOT_UPLOADED = 0
FAILED_INGESTION = -1

dnt_datetime_format = '%Y%m%dT%H%M%SZ'
server_location = 'ftp://nrt.cmems-du.eu/Core'

log_file = 'log/cmems_log.txt'
not_ingested = 'log/log_uningested_files.csv'
cmems_db = 'files_cmems.db'

product_id = 'INSITU_GLO_CARBON_NRT_OBSERVATIONS_013_049'

nrt_dir = '/' + product_id + '/NRT_201904/latest'
dnt_dir = '/' + product_id + '/DNT'
index_dir = '/' + product_id + '/NRT_201904'

local_folder = 'latest'

def build_dataproduct(dataset_zip,dataset_name,destination_filename):
  '''
  transforms csv-file to daily netCDF-files.
  Creates dictionary containing info on each netCDF file extracted
  requires: zip-folder, dataset-name and specific filename of csv-file.
  returns: dictionary
  '''
  # BUILD netCDF FILES

  # Load field config
  fieldconfig = pd.read_csv('fields.csv', delimiter=',', quotechar='\'')

  csv_file = get_file_from_zip(dataset_zip, destination_filename)  
  
  curr_date = datetime.datetime.now().strftime("%Y%m%d")
  
  if not os.path.exists(local_folder): os.mkdir(local_folder)

  logging.info(f'Creating netcdf-files based on {csv_file} to send to CMEMS')

  filedata = pd.read_csv(csv_file, delimiter=',')
  nc_files = buildnetcdfs(dataset_name, fieldconfig, filedata)
   
  nc_dict = {}
  for nc_file in nc_files:
    (nc_filename, nc_content) = nc_file
    hashsum = hashlib.md5(nc_content).hexdigest()
    logging.debug(f'Processing netCDF file {nc_filename}')

    # ASSIGN DATE-VARIABLES TO netCDF FILE
    nc_filepath = local_folder + '/' + nc_filename + '.nc'   

    logging.debug('Writing netCDF bytes to disk')
    with open(nc_filepath,'wb') as f: f.write(nc_content)

    # reading netCDF file to memory
    nc = netCDF4.Dataset(nc_filepath,mode = 'a')
    datasetdate = datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")
    nc.date_update = datasetdate
    nc.history = datasetdate + " : Creation"
    nc.close() # commits the changes to disk.

    # create dictionary object
    date = nc_filename.split('_')[-1]
    date = datetime.datetime.strptime(date,'%Y%m%d')
    hashsum = hashlib.md5(nc_content).hexdigest()
    nc_dict[nc_filename] = ({
      'filepath':nc_filepath, 
      'hashsum': hashsum, 
      'date': date, 
      'dataset':dataset_name,
      'uploaded':False})

  logging.debug(f'Commiting metadata to local SQL database {cmems_db}')
  sql_commit(nc_dict)
  return str(curr_date)


def upload_to_copernicus(ftp_config,server,dataset,curr_date):
  '''
  - Creates a FTP-connection
  - Uploads netCDF files
  - Creates and uploads index file and DNT file(s).
  - Checks response file generated by cmems to identify any failed uploads.

  ftp_config contains login information
  '''

  OK = True
  error = curr_date

  # create ftp-connection
  with ftputil.FTPHost(
    host=ftp_config['Copernicus'][server],
    user=ftp_config['Copernicus']['user'],
    passwd=ftp_config['Copernicus']['password'])as ftp:

    c = create_connection(cmems_db)

  # CHECK IF FTP IS EMPTY 
    logging.debug('Checking FTP directory')
    directory_empty = check_directory(ftp, nrt_dir) 
    if directory_empty:
      logging.error('Previous export has failed, \
        clean up remanent files before re-exporting')
      return False 

  # CHECK DICTONARY : DELETE FILES ON SERVER; OLDER THAN 30 DAYS
    logging.debug('Checking local database')
    c.execute("SELECT * FROM latest \
     WHERE (nc_date < date('now','-30 day') AND uploaded == ?)",[UPLOADED]) 
    results_delete = c.fetchall()
    logging.debug(f'delete {len(results_delete)}; {results_delete}')
    
    dnt_delete = {}
    for item in results_delete: 
      filename, filepath_local  = item[0], item[2]
      dnt_delete[filename] = item[6]
      c.execute("UPDATE latest SET uploaded = ? \
        WHERE filename = ?", [NOT_UPLOADED, filename])

  # CHECK DICTIONARY: UPLOAD FILES NOT ON SERVER; YOUNGER THAN 30 DAYS
    c.execute("SELECT * FROM latest \
      WHERE (nc_date >= date('now','-30 day') \
      AND NOT uploaded == ?)",[UPLOADED]) 
    results_upload = c.fetchall()    
    logging.debug(f'upload {len(results_upload)}: {results_upload}')

    dnt_upload = {}
    for item in results_upload:
      filename, filepath_local  = item[0], item[2]
      
      upload_result, filepath_ftp, start_upload_time, stop_upload_time = (
        upload_to_ftp(ftp, ftp_config, filepath_local))
      logging.debug(f'upload result: {upload_result}')
      
      if upload_result == 0: #upload ok
        # Setting dnt-variable to temp variable: curr_date.
        # After DNT is created, the DNT-filepath is updated for all  
        # instances where DNT-filetpath is curr_date
        c.execute("UPDATE latest \
          SET uploaded = ?, ftp_filepath = ?, dnt_file = ? \
          WHERE filename = ?", 
          [UPLOADED, filepath_ftp, curr_date ,filename])

        # create DNT-entry
        dnt_upload[filename] = ({'ftp_filepath':filepath_ftp, 
          'start_upload_time':start_upload_time, 
          'stop_upload_time':stop_upload_time,
          'local_filepath':local_folder+'/'+filename +'.nc'})    
        logging.debug(f'dnt entry: {dnt_upload[filename]}') 
      else:
        logging.debug(f'upload failed: {upload_result}')

    if dnt_upload or dnt_delete:
      # FETCH INDEX
      c.execute("SELECT * FROM latest WHERE uploaded == 1")
      currently_uploaded = c.fetchall()

      try:
        index_filename = build_index(currently_uploaded)
      except Exception as e:
        logging.error('Building index failed: ', exc_info=True)
        OK = False
        error += 'Building index failed: ' + str(e)
 
      # UPLOAD INDEX 
      try:
        upload_result, ftp_filepath, start_upload_time, stop_upload_time = (
          upload_to_ftp(ftp,ftp_config, index_filename))
        logging.debug(f'index upload result: {upload_result}')
      except Exception as e:
        logging.error('Uploading index failed: ', exc_info=True)
        OK = False      
        error += 'Uploading index failed: ' + str(e)
    
      # BUILD DNT-FILE
      # Adding index file to DNT-list:
      dnt_upload[index_filename] = ({
        'ftp_filepath':ftp_filepath, 
        'start_upload_time':start_upload_time, 
        'stop_upload_time':stop_upload_time,
        'local_filepath': index_filename
        })

      logging.info('Building DNT-file')
      try:
        dnt_file, dnt_local_filepath = build_DNT(dnt_upload,dnt_delete)

        # UPLOAD DNT-FILE
        _, dnt_ftp_filepath, _, _ = (
          upload_to_ftp(ftp, ftp_config, dnt_local_filepath))
        
        logging.info('Updating database to include DNT filename')
        sql_rec = "UPDATE latest SET dnt_file = ? WHERE dnt_file = ?"
        sql_var = [dnt_local_filepath, curr_date]
        c.execute(sql_rec,sql_var)

        try:
          response = evaluate_response_file(
            ftp,dnt_ftp_filepath,dnt_local_filepath.rsplit('/',1)[0],cmems_db)
          logging.debug('cmems dnt-response: {}'.format(response))

        except Exception as e:
          logging.error('No response from CMEMS: ', exc_info=True)
          OK = False
          error += 'No response from CMEMS: ' + str(e)

      except Exception as exception:
        logging.error('Building DNT failed: ', exc_info=True)
        OK = False
        error += 'Building DNT failed: ' + str(exception)

      # FOLDER CLEAN UP
      if dnt_delete:
        logging.debug('Delete empty directories')
        try: 
          _, dnt_local_filepath_f = build_fDNT(dnt_delete)

          _, dnt_ftp_filepath_f, _, _ = (
            upload_to_ftp(ftp, ftp_config, dnt_local_filepath_f))  
          try:
            response = evaluate_response_file(
              ftp,dnt_ftp_filepath_f,dnt_local_filepath_f.rsplit('/',1)[0],cmems_db)
            logging.debug('cmems fDNT-response, folders: {}'.format(response))

          except Exception as e:
            logging.error('No response from CMEMS: ', exc_info=True)
            OK = False
            error += 'No response from CMEMS: ' + str(e)

        except Exception as e:
          logging.error('Uploading fDNT failed: ', exc_info=True)
          OK = False
          error += 'Uploading fDNT failed: ' + str(e)

    if not OK:
      logging.error('Upload failed')
      abort_upload(error, ftp, nrt_dir, c, curr_date)

    return OK

def abort_upload(error,ftp,nrt_dir,c,curr_date):
  # Remove currently updated files on ftp-server
  uningested_files = clean_directory(ftp, nrt_dir)

  c.execute("SELECT * FROM latest WHERE (dnt_file = ?)",[curr_date])
  failed_ingestion = c.fetchall()
  
  logging.debug(f'failed ingestion: \n{failed_ingestion}')
  logging.debug(f'uningested files: \n {uningested_files}')

  # Update database : set uploaded to 0 where index-file is current date
  error_msg = "failed ingestion: " + error 
  sql_req = ("UPDATE latest \
    SET uploaded = ?, ftp_filepath = ?, dnt_file = ?, comment = ? \
    WHERE dnt_file = ?")
  sql_var = [0,None,None,error_msg, curr_date]
  c.execute(sql_req,sql_var)



def check_directory(ftp, nrt_dir):
  '''   Cleans out empty folders, checks if main directory is empty. 
  returns True when empty 
  '''
  uningested_files = clean_directory(ftp, nrt_dir)
  with open (not_ingested,'a+') as f:
    for item in uningested_files:
      f.write(str(datetime.datetime.now()) + ': ' + str(item) + '\n')
  if ftp.listdir(nrt_dir):
    logging.warning('ftp-folder is not empty')
    return True 
  else:
    return False

def clean_directory(ftp,nrt_dir):
  ''' removes empty directories from ftp server '''
  uningested_files = []
  for dirpath, dirnames, files in ftp.walk(nrt_dir+'/'):
    if not dirnames and not files and not dirpath.endswith('/latest/'):
      logging.debug(f'removing EMPTY DIRECTORY: {str(dirpath)}') 
      ftp.rmdir(dirpath)
    elif files:
      uningested_files += (
        [[('dirpath',dirpath),('dirnames',dirnames),('files',files)]])
      logging.debug(f'UNINGESTED: \
        dirpath: {dirpath}, \ndirnames: {dirnames}, \nfiles: {files}')
  return uningested_files

def create_connection(DB):
  ''' creates connection and database if not already created '''
  conn = sqlite3.connect(DB, isolation_level=None)
  c = conn.cursor()
  c.execute('''CREATE TABLE IF NOT EXISTS latest (
              filename TEXT PRIMARY KEY,
              hashsum TEXT NOT NULL,
              filepath TEXT NOT NULL UNIQUE,
              nc_date TEXT,
              dataset TEXT,
              uploaded INTEGER,
              ftp_filepath TEXT,
              dnt_file TEXT,
              comment TEXT,
              export_date TEXT
              )''')
  return c

def sql_commit(nc_dict):
  '''  creates SQL table if non exists
  adds new netCDF files, listed in nc_dict, to new or existing SQL-table 
  '''
  c = create_connection(cmems_db)
  date = datetime.datetime.now().strftime(dnt_datetime_format)

  for key in nc_dict:
    if nc_dict[key]['uploaded']: 
      uploaded = 1
    else: 
      uploaded = 0
    c.execute("SELECT * FROM latest WHERE filename=? ",[key])
    filename_exists = c.fetchone()
    
    if filename_exists: # if netCDF file already in database
      logging.info(f'Updating: {key}')
      sql_req = "UPDATE latest SET filename=?,hashsum=?,filepath=?,nc_date=?,\
        dataset=?,uploaded=?,ftp_filepath=?,dnt_file=?,comment=?,export_date=? \
        WHERE filename=?"
      sql_param = ([key,nc_dict[key]['hashsum'],nc_dict[key]['filepath'],
        nc_dict[key]['date'],nc_dict[key]['dataset'],uploaded,None,None,None,key,date])
    else:
      logging.debug(f'Adding new entry {key}')
      sql_req = "INSERT INTO latest(filename,hashsum,filepath,nc_date,\
        dataset,uploaded,ftp_filepath,dnt_file,comment,export_date) \
        VALUES (?,?,?,?,?,?,?,?,?,?)"
      sql_param = ([key,nc_dict[key]['hashsum'],nc_dict[key]['filepath'],
        nc_dict[key]['date'],nc_dict[key]['dataset'],uploaded,None,None,None,date])

    c.execute(sql_req,sql_param)

def upload_to_ftp(ftp, ftp_config, filepath):
  ''' Uploads file with location 'filepath' to an ftp-server, 
  server-location set by 'directory' parameter and config-file, 
  ftp is the ftp-connection

  returns 
  upload_result: upload_ok or file_exists
  dest_filepath: target filepath on ftp-server
  start_upload_time and stop_upload_time: timestamps of upload process
  '''
 
  upload_result = UPLOAD_OK
  if filepath.endswith('.nc'):
    filename = filepath.rsplit('/',1)[-1]
    date = filename.split('_')[-1].split('.')[0]
    ftp_folder = nrt_dir + '/' + date     
    ftp_filepath = ftp_folder + '/' +  filename

  elif filepath.endswith('.xml'):
    ftp_folder = dnt_dir
    ftp_filepath = ftp_folder + '/' + filepath.rsplit('/',1)[-1]

  elif filepath.endswith('.txt'):
    with open(filepath,'rb') as f: 
      file_bytes = f.read() 
    ftp_folder = index_dir
    ftp_filepath = ftp_folder + '/' + filepath.rsplit('/',1)[-1]


  start_upload_time = datetime.datetime.now().strftime(dnt_datetime_format)
  if not ftp.path.isdir(ftp_folder):
    ftp.mkdir(ftp_folder)
    ftp.upload(filepath, ftp_filepath)
  elif ftp.path.isfile(ftp_filepath):
    upload_result = FILE_EXISTS
  else:
    ftp.upload(filepath, ftp_filepath)
  stop_upload_time = datetime.datetime.now().strftime(dnt_datetime_format)

  return upload_result, ftp_filepath, start_upload_time, stop_upload_time


def build_DNT(dnt_upload,dnt_delete):
  ''' Generates delivery note for NetCDF file upload, 
  note needed by Copernicus in order to move .nc-file to public-ftp
  
  dnt_upload contains list of files uploaded to the ftp-server
  dnt_delete contains list of files to be deleted from the ftp-server

  '''
  date = datetime.datetime.now().strftime(dnt_datetime_format)

  dnt = ET.Element('delivery')
  dnt.set('PushingEntity','CopernicusMarine-InSitu-Global')
  dnt.set('date', date)
  dnt.set('product',product_id)
  dataset = ET.SubElement(dnt,'dataset')
  dataset.set('DatasetName','NRT_201904')

# upload
  for item in dnt_upload:
    local_filepath = dnt_upload[item]['local_filepath']
    ftp_filepath = dnt_upload[item]['ftp_filepath'].split('/',3)[-1]
    start_upload_time = dnt_upload[item]['start_upload_time'] 
    stop_upload_time = dnt_upload[item]['stop_upload_time']
    with open(local_filepath,'rb') as f: 
      file_bytes = f.read()

    file = ET.SubElement(dataset,'file')
    file.set('Checksum',hashlib.md5(file_bytes).hexdigest())
    file.set('FileName',ftp_filepath)
    file.set('FinalStatus','Delivered')
    file.set('StartUploadTime',start_upload_time)
    file.set('StopUploadTime',stop_upload_time)

# delete
  for item in dnt_delete:
    ftp_filepath = dnt_delete[item].split('/',3)[-1]

    file_del = ET.SubElement(dataset,'file')
    file_del.set('FileName',ftp_filepath)
    key_word = ET.SubElement(file_del,'KeyWord')
    key_word.text = 'Delete'

  xml_tree = ET.ElementTree(dnt)

  dnt_file = product_id + '_P' + date + '.xml'
  dnt_folder = 'DNT/' + local_folder + '/'  
  dnt_filepath = dnt_folder + dnt_file

  if not os.path.isdir(dnt_folder):  os.mkdir(dnt_folder)

  with open(dnt_filepath,'wb') as xml: 
    xml_tree.write(xml,xml_declaration=True,method='xml')

  return dnt_file, dnt_filepath

def build_fDNT(dnt_delete):
  ''' Generates delivery note for NetCDF folder clean up '''
  date = datetime.datetime.now().strftime(dnt_datetime_format)

  dnt = ET.Element('delivery')
  dnt.set('PushingEntity','CopernicusMarine-InSitu-Global')
  dnt.set('date', date)
  dnt.set('product',product_id)
  dataset = ET.SubElement(dnt,'dataset')
  dataset.set('DatasetName','NRT_201904')

# delete
  for item in dnt_delete:
    ftp_filepath = dnt_delete[item].split('/',3)[-1]

    file_del = ET.SubElement(dataset,'directory')
    file_del.set('DestinationFolderName','')
    file_del.set('SourceFolderName',ftp_filepath.rsplit('/',1)[0])
    key_word = ET.SubElement(file_del,'KeyWord')
    key_word.text = 'Delete'

  xml_tree = ET.ElementTree(dnt)
 # logging.debug('DNT file:\n' + str(ET.dump(xml_tree)))

  dnt_file = product_id + '_P' + date + '.xml'
  dnt_folder = 'DNT/' + local_folder + '/'  
  dnt_filepath = dnt_folder + dnt_file

  try: os.mkdir(dnt_folder); 
  except Exception as e:
    pass

  with open(dnt_filepath,'wb') as xml: 
    xml_tree.write(xml,xml_declaration=True,method='xml')

  return dnt_file, dnt_filepath

def build_index(results_uploaded):
  '''
  Creates index-file over CMEMS directory.

  Lists all files currently uploaded to the CMEMS server. 
  '''

  date_header = datetime.datetime.now().strftime('%Y%m%d%H%M%S')

  index_header = ('# Title : Carbon in-situ observations catalog \n'\
    + '# Description : catalog of available in-situ observations per platform.\n'\
    + '# Project : Copernicus \n# Format version : 1.0 \n'\
    + '# Date of update : ' + date_header +'\n'
    + '# catalog_id,file_name,geospatial_lat_min,geospatial_lat_max,'\
    + 'geospatial_lon_min,geospatial_lon_max,time_coverage_start,'\
    + 'time_coverage_end,provider,date_update,data_mode,parameters\n')

  index_info = ''
  for file in results_uploaded:
    local_filepath = file[2]
    ftp_filepath = file[6]

    nc = netCDF4.Dataset(local_filepath,mode='r')

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
    parameters = ' '.join(var_list)

    index_info += ('COP-GLOBAL-01,' + server_location + ftp_filepath + ',' 
                + lat_min + ',' + lat_max + ',' + lon_min + ',' + lon_max + ',' 
                + time_start + ',' + time_end  
                + ',University of Bergen Geophysical Institute,' 
                + date_update + ',R,' + parameters + '\n')

  index_latest = index_header + index_info

  index_filename = 'index_latest.txt'
  with open(index_filename,'wb') as f: f.write(index_latest.encode())
 
  logging.debug('index file:\n' + index_latest)

  return index_filename


def evaluate_response_file(ftp,dnt_filepath,folder_local,cmems_db):
  '''  Retrieves response from cmems-ftp server.
  '''
  response_received = False
  loop_iter = 0
  upload_response_log = ''
  rejected_list = []

  logging.debug('waiting for dnt-response')
  while response_received == False and loop_iter < 50 :
    time.sleep(10)
    logging.debug('checking for dnt-response ' + str(loop_iter*10))
    try:
      cmems_response = get_response(ftp,dnt_filepath,folder_local)
      response_received = True
      logging.debug('cmems response: ' + cmems_response)
    except:
      response_received = False
      logging.debug('no response found')
    loop_iter += 1

  if response_received == False: 
    return 'No response received'
  else:
    if not 'Ingested="True"' in cmems_response: #ingestion failed or partial
      rejected = re.search(
        'FileName=(.+?)RejectionReason=(.+?)Status',cmems_response)
      if rejected:
        rejected_file, rejected_reason = [rejected.group(1), rejected.group(2)]
        logging.info('Rejected: {}, {}'.format(rejected_file, rejected_reason))

        rejected_list += [[rejected_file,rejected_reason]] 
        rejected_filename = rejected_file.split('/')[-1].split('.')[0]

        c = create_connection(cmems_db)
        sql_req = "UPDATE latest SET uploaded=?,comment=? WHERE filename=?"
        sql_var = ([-1,rejected_reason, rejected_filename])
        c.execute(sql_req,sql_var)


    else:
      logging.info('All files ingested')
    date = datetime.datetime.now().strftime('%Y-%m-%d')
    upload_response_log += ( 
      date + ',' + 
      folder_local + ',' + 
      dnt_filepath + ',' + 
      cmems_response + ',' + '\n')
  
  if os.path.isfile(log_file):
    with open(log_file,'a+') as log: 
      log.write(upload_response_log)
  else: 
    with open(log_file,'w') as log: 
      log.write('date, local filepath, cmems filepath, cmems response\n')
      log.write(upload_response_log)
      logging.debug(f'log-message: {upload_response_log}')

  if rejected_list:
    return rejected_list
  else: return True

def get_response(ftp,dnt_filepath,folder_local):
  '''  Retrieves the status of any file uploaded to CMEMS server
  returns the string of the xml responsefile generated by the CMEMS server. 
  '''
  source = (dnt_filepath.split('.')[0]
    .replace('DNT','DNT_response') + '_response.xml')
  target = folder_local + '/' +  source.split('/')[-1]

  ftp.download(source,target)
  with open(target,'r') as response_file:
    response = response_file.read()
  return response