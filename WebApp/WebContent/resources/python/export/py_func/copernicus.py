
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
#import pysftp
import ftplib
import ftputil 
import os
import sys
import re
import hashlib
import datetime
import netCDF4
from py_func.cmems_converter import buildnetcdfs 

import xml.etree.ElementTree as ET
import sqlite3
import json
import time

from py_func.meta_handling import get_file_from_zip

# Upload result codes
UPLOAD_OK = 0
NOT_INITIALISED = 1
FILE_EXISTS = 2

# Response codes
UPLOADED = 1
NOT_UPLOADED = 0
FAILED_INGESTION = -1

dnt_datetime_format = '%Y%m%dT%H%M%SZ'
server_location = 'ftp://nrt.cmems-du.eu/Core'
my_dir=''
lt = 'latest'

nc_nan = 'tmp_nan_update.nc'

cmems_log = 'log/cmems_response.log'
error_log = 'log/errors.log'
database_file = 'log/database.csv'
failed_ingestion_log = 'log/failed_ingestion.csv'
not_ingested = 'log/log_uningested_files.csv'
delay_log = 'log/delay.log'

cmems_db = 'files_cmems.db'
delay_db = 'log/delay.db'


nrt_dir='/INSITU_GLO_CARBON_NRT_OBSERVATIONS_013_049/NRT_201904/latest'
dnt_dir='/INSITU_GLO_CARBON_NRT_OBSERVATIONS_013_049/DNT'
index_dir = '/INSITU_GLO_CARBON_NRT_OBSERVATIONS_013_049/NRT_201904'

product_id = 'INSITU_GLO_CARBON_NRT_OBSERVATIONS_013_049'

def build_netCDF(dataset_zip,dataset_name,data_filename):
  '''
  transforms csv-file to daily netCDF-files.
  Creates dictionary containing info on each netCDF file extracted
  requires: zip-folder, dataset-name and specific filename of csv-file.
  returns: dictionary
  '''
  # BUILD netCDF FILES

  csv_file = get_file_from_zip(dataset_zip, data_filename)  

  if not os.path.isdir(lt):
    os.mkdir(lt)

  local_folder = (
    lt + '/' + (datetime.datetime.now().strftime("%Y%m%d_%H%M%S")))
  if not os.path.isdir(local_folder):
    os.mkdir(local_folder); 

    
  logging.info(
    'Creating netcdf-files based on {:s} to send to Copernicus'
    .format(csv_file))
  with open(csv_file) as f: 
    csv = f.read()
    nc_files = buildnetcdfs(dataset_name,csv)
   
  nc_dict = {}
  for nc_file in nc_files:
    nc_filename = nc_file[0]
    nc_content = nc_file[1] 
    hashsum = hashlib.md5(nc_content).hexdigest()

    # ASSIGN DATE-VARIABLES TO netCDF FILE
    #assigning date_update and history
    nc_filepath = local_folder + '/' + nc_filename + '.nc'   

    with open(nc_filepath,'wb') as f: f.write(nc_content)

    nc = netCDF4.Dataset(nc_filepath,mode = 'a')
    datasetdate = datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")
    nc.date_update = datasetdate
    nc.history = datasetdate + " : Creation"
    nc.close()
 

    # create dictionary object
    date = nc_filename.split('_')[-1]
    date = datetime.datetime.strptime(date,'%Y%m%d')
    nc_dict[nc_filename] = ({
      'filepath':nc_filepath, 
      'hashsum': hashsum, 
      'date': date, 
      'uploaded':False,
      'dataset':dataset_name}
      )
  # Commit dict object to database
  sql_commit(nc_dict)


def sql_commit(nc_dict,table="latest"):
  ''' 
  creates SQL table if non exists

  adds new netCDF files, listed in nc_dict, to new or existing SQL-table 

  '''
  conn = sqlite3.connect(cmems_db)

  c = conn.cursor()

  # if not table: 
  #logging.info('Creating database {}'.format(table))
  c.execute(''' CREATE TABLE IF NOT EXISTS latest (
              filename TEXT PRIMARY KEY,
              hashsum TEXT NOT NULL,
              filepath TEXT NOT NULL UNIQUE,
              nc_date TEXT,
              dataset TEXT,
              uploaded INTEGER,
              ftp_filepath TEXT,
              dnt_file TEXT,
              comment TEXT
              )''')

  for key in nc_dict:

    if nc_dict[key]['uploaded']: uploaded = 1
    else: uploaded = 0

    c.execute("SELECT hashsum FROM latest WHERE filename=? ",[key])
    filename_exists = c.fetchone() #set to hashsum if filename exists
    try:
      if filename_exists: # if netCDF file already in database
        if filename_exists[0] == nc_dict[key]['hashsum']:#hashsums match
          logging.info(f'{key} Already in the database')
        else:
          logging.info(f'Updating: {key}')
          c.execute("UPDATE latest SET \
            hashsum=?,filepath=?,nc_date=?,dataset=?,uploaded=?,ftp_filepath=?,\
            dnt_file=?,comment=? WHERE filename = ?",\
            (nc_dict[key]['hashsum'], nc_dict[key]['filepath'],
            nc_dict[key]['date'], nc_dict[key]['dataset'],  
            uploaded, None, None, None, key))
        conn.commit()
      else:
        logging.debug(f'Adding new entry {key}')
        c.execute("INSERT INTO latest \
          (filename,hashsum,filepath,nc_date,dataset,uploaded,\
          ftp_filepath,dnt_file,comment) \
          VALUES (?,?,?,?,?,?,?,?,?)",(
          key, 
          nc_dict[key]['hashsum'], 
          nc_dict[key]['filepath'], 
          nc_dict[key]['date'],
          nc_dict[key]['dataset'], 
          uploaded, 
          None, 
          None,
          None))
        conn.commit()
    except Exception as e:
      logging.error(f'Adding/Updating database failed:  {key} ', exc_info=True)


def upload_to_copernicus(ftp_config,server,dataset):
  '''
  Creates a FTP-connection.
  Uploads netCDF files, creates and uploads index file and DNT file.
  Checks response file generated by cmems to identify any failed uploads.

  netCDF uploads are determined based on date-range and current upload-status.

  ftp_config contains login information
  server is the url of the cmems ftp-server 

  '''

  OK = True
  error = ''

  if not os.path.isdir('log'):
    os.mkdir('log')
  if not os.path.isdir('DNT'):
    os.mkdir('DNT')

  # create ftp-connection
  with ftputil.FTPHost(
    host=ftp_config['Copernicus'][server],
    user=ftp_config['Copernicus']['user'],
    passwd=ftp_config['Copernicus']['password'])as ftp:

    conn = sqlite3.connect(cmems_db)
    c = conn.cursor()

  # CHECK IF FTP IS EMPTY 
    directory_empty = check_directory(ftp, nrt_dir) 
    if directory_empty:
      return False

  # CHECK DICTONARY : DELETE FILES ON SERVER; OLDER THAN 30 DAYS
  # Dictionary structure: 
  # filename, hashsum, filepath, nc_date, dataset, uploaded, ftp_filepath, dnt_file, comment
    c.execute("SELECT * FROM latest \
     WHERE (nc_date < date('now','-30 day') AND uploaded == ?)",[UPLOADED]) 
    results_delete = c.fetchall()
    logging.debug(f'delete: {results_delete}')
    
    dnt_delete = {}
    for item in results_delete: 
      filename, filepath_local  = item[0], item[2]
      dnt_delete[filename] = item[6].rsplit('/NRT/')[-1] #ftp_filepath
      logging.debug(f'To be deleted: {dnt_delete[filename]}')
      c.execute("UPDATE latest SET uploaded = ? \
        WHERE filename = ?", [NOT_UPLOADED, filename])
      conn.commit()

  # CHECK DICTIONARY: UPLOAD FILES NOT ON SERVER; YOUNGER THAN 30 DAYS
    c.execute("SELECT * FROM latest \
      WHERE (nc_date >= date('now','-30 day') \
      AND uploaded == ?)",[NOT_UPLOADED]) 
    results_upload = c.fetchall()    
    logging.debug(f'upload: {results_upload}')
    
    local_folder = ''; csv_file = ''; filename = ''

    dnt_upload = {}
    for item in results_upload:
      filename, filepath_local, csv_file  = item[0], item[2], item[4]
      
      upload_result, filepath_ftp, start_upload_time, stop_upload_time = (
        upload_to_ftp(ftp, ftp_config, filepath_local))
      logging.debug(f'upload result: {upload_result}')
      
      # Setting dnt-variable to temp variable: local folder.
      # After DNT is created, DNT-filepath is updated for all instances where 
      # DNT-filetpath is local_folder
      local_folder = filepath_local.rsplit('/',1)[0] 
      if upload_result == 0: #upload ok
        c.execute("UPDATE latest \
          SET uploaded = ?, ftp_filepath = ?, dnt_file = ? \
          WHERE filename = ?", 
          [UPLOADED, filepath_ftp, local_folder ,filename])
        conn.commit()

        #log time delay
        try:
          log_time(csv_file,filename,stop_upload_time)
        except Exception as e:
          logging.error('Logging time failed:', exc_info=True)

        # create DNT-entry
        dnt_upload[filename] = ({'ftp_filepath':filepath_ftp, 
          'start_upload_time':start_upload_time, 
          'stop_upload_time':stop_upload_time,
          'local_folder':local_folder})    
        logging.debug(f'dnt entry: {dnt_upload[filename]}') 


      else:
        logging.debug(f'Upload failed: {upload_result}')
        OK = False
        error += filename + ': uploading nc-file failed.'
        c.execute("UPDATE latest \
          SET uploaded = ? WHERE filename = ?", 
          [FAILED_INGESTION, filename])
        conn.commit()


    if dnt_upload or dnt_delete:
      # BUILD INDEX
      c.execute("SELECT * FROM latest WHERE uploaded == 1")
      currently_uploaded = c.fetchall()
      try:
        index_filepath = build_index(currently_uploaded)
      except Exception as e:
        logging.error('Building index failed: ', exc_info=True)
        OK = False
        error += 'Building index failed: ' + str(e)
        c.execute("UPDATE latest \
          SET uploaded = ? WHERE filename = ?", 
          [FAILED_INGESTION, filename])
        conn.commit()        
 
      # UPLOAD INDEX 
      try:
        upload_result, ftp_filepath, start_upload_time, stop_upload_time = (
          upload_to_ftp(ftp,ftp_config, index_filepath))
        logging.debug(f'index upload result: {upload_result}')
      except Exception as e:
        logging.error('Uploading index failed: ', exc_info=True)
        OK = False      
        error += 'Uploading index failed: ' + str(e)
        c.execute("UPDATE latest \
          SET uploaded = ? WHERE filename = ?", 
          [FAILED_INGESTION, filename])
        conn.commit()
    
      # BUILD DNT-FILE
      # Adding index file to DNT-list:
      dnt_upload[index_filepath] = ({
        'ftp_filepath':ftp_filepath, 
        'start_upload_time':start_upload_time, 
        'stop_upload_time':stop_upload_time,
        'local_folder':index_filepath.rsplit('/',1)[0]
        })

      logging.info('Building DNT-file')
      try:
        dnt_file, dnt_local_filepath = build_DNT(dnt_upload,dnt_delete)
        
        logging.info('Updating database to include DNT filename')
        c.execute("UPDATE latest SET dnt_file = ? \
          WHERE dnt_file = ?", [dnt_local_filepath, local_folder])
        conn.commit()

      except Exception as e:
        logging.error('Building DNT failed: ', exc_info=True)
        OK = False
        error += 'Building DNT failed: ' + str(e)
        c.execute("UPDATE latest \
          SET uploaded = ? WHERE filename = ?", 
          [FAILED_INGESTION, filename])
        conn.commit()

      # UPLOAD DNT-FILE
      try: 
        #using underscores to unpack the function without storing unused vars.
        _, dnt_ftp_filepath, _, _ = (
          upload_to_ftp(ftp, ftp_config, dnt_local_filepath))
    
        try:
          response, rejected_files= evaluate_response_file(
            ftp,dnt_ftp_filepath,dnt_local_filepath.rsplit('/',1)[0])
          if rejected_files:
            logging.info(f'CMEMS rejection: {rejected_files}')
            for file in rejected_files:
              logging.debug(f'Updating {file[0]} to upload-status: \
                Failed ingestion in CMEMS-database, {file[1]}')
              
              try:
                filename = file[0].split('.')[0].rsplit('/')[-1]
                c.execute("UPDATE latest \
                  SET uploaded = ?, comment = ? WHERE filename = ?", 
                  [FAILED_INGESTION,file[1],str(filename)])
                conn.commit()
                logging.debug('database updated')
              except Exception as e:
                logging.error(f'Failed to update database')


        except Exception as e:
          logging.error('No response from CMEMS: ', exc_info=True)
          error_msg = "No CMEMS-response"
          c.execute("UPDATE latest \
            SET uploaded = -1, comment = ? \
            WHERE dnt_file = ?", 
            [error_msg, local_folder])
          logging.debug(f'local_folder: {local_folder}')
          conn.commit()

      except Exception as e:
        logging.error('Uploading DNT failed: ', exc_info=True)
        OK = False
        error += 'Uploading DNT failed: ' + str(e)
        c.execute("UPDATE latest \
          SET uploaded = ? WHERE filename = ?", 
          [FAILED_INGESTION, filename])
        logging.debug(f'DNT failed: {filename}')
        conn.commit()



    # writing database to csv-file for debugging
    c.execute("SELECT * FROM latest")
    database = c.fetchall()
    with open(database_file,'w') as f:
      for item in database:
        f.write(str(item)+'\n')

    #Writing failed ingestions to csv-file for debugging
    c.execute("SELECT * FROM latest WHERE uploaded = -1")
    failed_ingestion = c.fetchall()
    if failed_ingestion:
      OK = False
      error = str(item[-1])
      with open(failed_ingestion_log,'a+') as f:
        for item in failed_ingestion:
          f.write(str(item)+'\n')

    if not OK:
      logging.warning('Aborting upload')
      abort_upload(error, local_folder, ftp, csv_file, filename)


    #upload complete
    return OK


def check_directory(ftp, nrt_dir):
  ''' 
  Cleans out empty folders, checks if main directory is empty. 
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
  if filepath.endswith('.nc'): # Datafile
    filename = filepath.rsplit('/',1)[-1]
    date = filename.split('_')[-1].split('.')[0]
    ftp_folder = nrt_dir + '/' + date     
    ftp_filepath = ftp_folder + '/' +  filename

  elif filepath.endswith('.xml'): # DNT file
    ftp_folder = dnt_dir
    ftp_filepath = ftp_folder + '/' + filepath.rsplit('/',1)[-1]

  elif filepath.endswith('.txt'): # index file
    ftp_folder = nrt_dir.rsplit('/',1)[0]
    
    ftp_filepath = ftp_folder + '/' + filepath.rsplit('/',1)[-1]

    with open(filepath,'rb') as f: 
      file_bytes = f.read()

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
  ''' 
  Generates delivery note for NetCDF file upload, 
  note needed by Copernicus in order to move .nc-file to public-ftp
  
  dnt_upload contains list of files uploaded to the ftp-server
  dnt_delete contains list of files to be deleted from the ftp-server

  returns the dnt filename and filepath
  '''
  date = datetime.datetime.now().strftime(dnt_datetime_format)

  dnt = ET.Element('delivery')
  dnt.set('PushingEntity','CopernicusMarine-InSitu-Global')
  dnt.set('date', date)
  dnt.set('product',product_id)
  dataset = ET.SubElement(dnt,'dataset')
  dataset.set('DatasetName','NRT_201904')

  # items to be uploaded
  for item in dnt_upload:
    local_folder = dnt_upload[item]['local_folder'] + '/'
    ftp_filepath = dnt_upload[item]['ftp_filepath'].split('/',3)[-1]
    start_upload_time = dnt_upload[item]['start_upload_time'] 
    stop_upload_time = dnt_upload[item]['stop_upload_time']
    if ('index_' in local_folder):
      with open(ftp_filepath.split('/')[-1],'rb') as f: 
        file_bytes = f.read()
    else:
      with open(local_folder + ftp_filepath.split('/')[-1],'rb') as f: 
        file_bytes = f.read()

    file = ET.SubElement(dataset,'file')
    file.set('Checksum',hashlib.md5(file_bytes).hexdigest())
    file.set('FileName',ftp_filepath)
    file.set('FinalStatus','Delivered')
    file.set('StartUploadTime',start_upload_time)
    file.set('StopUploadTime',stop_upload_time)

  # items to be deleted
  for item in dnt_delete:
    ftp_filepath = dnt_delete[item]

    file_del = ET.SubElement(dataset,'file')
    file_del.set('FileName',ftp_filepath)
    key_word = ET.SubElement(file_del,'KeyWord')
    key_word.text = 'Delete'

  xml_tree = ET.ElementTree(dnt)

  dnt_file = product_id + '_P' + date + '.xml'
  dnt_folder = 'DNT/' + local_folder  
  dnt_filepath = dnt_folder + '/' + dnt_file

  if not os.path.isdir(dnt_folder):
    os.mkdir(dnt_folder)

  with open(dnt_filepath,'wb') as xml: 
    xml_tree.write(xml,xml_declaration=True,method='xml')

  logging.debug('DNT file: ' + str(dnt_filepath))
  return dnt_file, dnt_filepath


def build_index(results_uploaded):
  '''
  Creates index-file over CMEMS directory.
  Lists all files currently uploaded to the CMEMS server. 

  results_uploaded is a list of files currently uploaded to the FTP-server
  according to the CMEMS-database.

  returns the filename of the generated index-file
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

    # retrieving list of parameters from netCDF file
    var_list = nc.variables.keys()
    var_list = list(filter(lambda x: '_' not in x, var_list))
    var_list = list(filter(lambda x: 'TIME' not in x, var_list))
    var_list = list(filter(lambda x: 'LATITUDE' not in x, var_list))
    var_list = list(filter(lambda x: 'LONGITUDE' not in x, var_list))
    nc.close()

    # reformat parameter list to index-format: 
    #   string of parameters separated by space
    parameters = ''
    for item in var_list: parameters += item +' '
    parameters = parameters[:-1] #removes final space added by loop.
  
    index_info += ('COP-GLOBAL-01' +','+ server_location + ftp_filepath +',' 
                + lat_min + ',' + lat_max + ',' + lon_min + ',' + lon_max  
                + ',' + time_start + ',' + time_end + ',' 
                + 'University of Bergen Geophysical Institute' + ',' 
                + str(date_update) + ',' + 'R' + ',' + parameters + '\n')

  index_latest = index_header + index_info

  index_filename = 'index_latest.txt'
  #index_filename = 'latest/index_latest.txt'
  with open(index_filename,'wb') as f: f.write(index_latest.encode())
 
  logging.debug('index file:\n' + index_latest)

  return index_filename


def get_response(ftp,dnt_filepath,folder_local):
  '''
  Retrieves the status of any file uploaded to CMEMS server

  requires login information and the filename of the DNT associated with 
  the upload.
  returns the string of the xml responsefile generated by the CMEMS server. 
  '''
  source = (dnt_filepath.split('.')[0]
    .replace('DNT','DNT_response') + '_response.xml')
  target = folder_local + '/' +  source.split('/')[-1]

  ftp.download(source,target)

  with open(target,'r') as response_file:
    response = response_file.read()
  return response


def evaluate_response_file(ftp,dnt_filepath,folder_local):
  '''
  Retrieves response from cmems-ftp server.

  returns a list of rejected files

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
    logging.info('No response from received from cmems ')
  else:
    if 'RejectionReason' in cmems_response: #ingestion failed or partial
      rejected = re.search(
        'FileName=(.+?)RejectionReason=(.+?)Status',cmems_response)
      rejected_file, rejected_reason = [rejected.group(1), rejected.group(2)]
      logging.info('Rejected: {}, {}'.format(rejected_file, rejected_reason))

      rejected_list += [[rejected_file,rejected_reason]] 

    else:
      logging.info('All files ingested')
    date = datetime.datetime.now().strftime('%Y-%m-%d')
    upload_response_log += ( 
      date + ',' + 
      folder_local + ',' + 
      dnt_filepath + ',' + 
      cmems_response + ',' + '\n')

  
  cmems_log_exists = os.path.isfile(cmems_log)
  if cmems_log_exists:
    with open(cmems_log,'a+') as log: 
      log.write(upload_response_log)
  else: 
    with open(cmems_log,'w') as log: 
      log.write('date, local filepath, cmems filepath, cmems response\n')
      log.write(upload_response_log)
      logging.debug('log-message: {}'.format(upload_response_log))

  if rejected_list:
    return cmems_response, rejected_list
  else: return cmems_response, None


def abort_upload(error,local_folder,ftp,csv_file,nc_file):
  '''
  Protocol to clear system when a process fails.
  error contains an error message describing where the script failed, and the
  console log of the exception.

  If the script fails the database must be updated to reflect the incident.
  Uploaded must be set to False, DNT and FTP fields must be set to None and 
  the error-message should be added to the comment field.  

  If the script fails after the initial upload of netCDF-files, these must be 
  deleted from the CMEMS production FTP server as part of the clean up.

  Finally the error should be logged including: date, csv-file, 
  error-message. 

  '''
  
  # Clear FTP 
  conn = sqlite3.connect(cmems_db)
  c = conn.cursor()
  c.execute("SELECT * FROM latest WHERE (uploaded = -1)")
  failed_ingestion = c.fetchall()
  logging.debug(f'failed ingestion: \n {failed_ingestion}')

  if 'index' or 'DNT' in error: 
    logging.debug('Cleaning up FTP-server in connection with ingestion-failure')
    current_ftp = ftp.listdir(nrt_dir)
    logging.debug(f'Removing: {set(current_ftp).intersection(failed_ingestion)}')
    clear_FTP(ftp, set(current_ftp).intersection(failed_ingestion))

  # Update database : set uploaded to 0 where upload failed
  error_msg = "Failed ingestion: " + local_folder + ', ' + error 
  try:
    for item in failed_ingestion:
      if item[8]:
        prev_error = str( item[8])
      else:
        prev_error = ''
      error_msg = prev_error + error_msg
      c.execute("UPDATE latest SET uploaded = 0, comment = ? WHERE hashsum = ?",[error_msg, str(item[1])])
    conn.commit()
  except:
    logging.error('Failed to reset database after export-failure')
    logging.error('Exception occurred: ', exc_info=True)
  
  # Add log-entry denoting files and failure-point
  # errors.log: date, dataset, nc-file,  error-msg.
  log_error(csv_file,nc_file, error_msg)


def clear_FTP(ftp,files):
  '''
  Deletes files in list 'files' from CMEMS ftp server
  '''
  for file in files:
    logging.debug(f'removing {str(file)} from ftp-server')
    try:
      ftp.remove(str(file))
    except Exception as e:
      logging.debug(f'Failed to delete {file}')
      logging.error('Exception occurred: ', exc_info=True)

def log_time(csv_file,nc_file, t_upload):
  '''
  Generates and populates database of uploaded files and potential time-delay.
  csv-file is original dataset-filename as retrieved from QuinCe
  nc-file is netCDF filename, file that was uploaded
  d_measured is the date the data was measured, retrieved from nc_file
  d_retrieved is the date the data was retrieved from QuinCe, current date
  t_upload is the timestamp at end of upload to ftp du

  t_target is calculated based on d_measured
  t_delay is the estimated delay, if on-time, t_delay is set to 0

  database filename is delivery_log.db  

  '''
  
  #if database not exist create database
  
  conn = sqlite3.connect('delivery_log.db')

  c = conn.cursor()

  c.execute(''' CREATE TABLE IF NOT EXISTS log (
              filename TEXT PRIMARY KEY,
              csv_filename TEXT NOT NULL,
              measured TEXT,
              received TEXT,
              uploaded TEXT,
              target TEXT,
              delay INT,
              comment TEXT
              )''')
  c.execute("SELECT * FROM log WHERE filename=? ",[nc_file])
  filename_exists = c.fetchone() #set to hashsum if filename exists
  
  if not filename_exists:
    nc_date = nc_file.split('.')[0].rsplit('_')[-1]
    d_measured = datetime.datetime.strptime(nc_date,'%Y%m%d')
    # t_target = d_measured + 12 hours
    t_target = d_measured + datetime.timedelta(hours=36)
    t_upload = datetime.datetime.strptime(t_upload,'%Y%m%dT%H%M%SZ')
    
    # t_received set to 1h prior, awaiting QuinCe-update 
    t_received = datetime.datetime.now() + datetime.timedelta(hours=1) 

    # estimate t_delay
    t_delay = (t_upload - t_target).total_seconds()/60
    if t_delay < 0: t_delay = 0


    c.execute("INSERT INTO log \
      (filename,csv_filename,measured,received,uploaded,target,delay,comment)\
      VALUES (?,?,?,?,?,?,?,?)",(
      nc_file, csv_file, d_measured, 
      t_received, t_upload, t_target, t_delay,''))
    conn.commit()

    c.execute("SELECT * FROM log")
    database = c.fetchall()

    with open(delay_log,'w') as log:
      log.write('filename, csv_filename, measured, received, uploaded, target, delay, comment\n')
      for item in database:
        log.write(str(item)+'\n')


def log_error(dataset, filename, error_msg):
  error_log_msg = (str(datetime.datetime.now()) + ', ' + dataset + ', '+ 
    filename + ', ' + error_msg)
  error_log_exists = os.path.isfile(error_log)
  if error_log_exists:
    with open(error_log,'a+') as log: 
      log.write(error_log_msg + '\n')
  else: 
    with open(error_log,'w') as log: 
      log.write('date, dataset, netCDF-file, error-message\n')
      log.write(error_log_msg + '\n')
  logging.debug('"error.log"-message: {}'.format(error_log))

