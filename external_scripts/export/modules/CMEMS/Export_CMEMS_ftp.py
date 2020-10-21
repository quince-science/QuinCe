

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
from modules.CMEMS.cmems_converter import buildnetcdfs 
from modules.Local.data_processing import get_file_from_zip

import xml.etree.ElementTree as ET
import sqlite3
import json
import time

# Upload result codes
UPLOAD_OK = 0
FILE_EXISTS = 2

# Response codes
UPLOADED = 1
NOT_UPLOADED = 0
FAILED_INGESTION = -1

dnt_datetime_format = '%Y-%m-%dT%H:%M:%SZ'
server_location = 'ftp://nrt.cmems-du.eu/Core'

log_file = 'log/cmems_log.txt'
not_ingested = 'log/log_uningested_files.csv'
cmems_db = 'files_cmems.db'

product_id = 'INSITU_GLO_CARBON_NRT_OBSERVATIONS_013_049'
dataset_id = 'NRT_202003'
institution = 'University of Bergen Geophysical Institute'
institution_edmo = '4595'

nrt_dir = '/' + product_id + '/' + dataset_id + '/latest'
dnt_dir = '/' + product_id + '/DNT'
index_dir = '/' + product_id + '/' + dataset_id

local_folder = 'latest'

def upload_to_copernicus(ftp_config,server,dataset,curr_date,platform):
  '''
  - Creates a FTP-connection
  - Uploads netCDF files
  - Creates and uploads index file and DNT file(s).
  - Checks response file generated by cmems to identify any failed uploads.

  ftp_config contains login information
  '''
  status = 0
  error = curr_date
  error_msg = ''


  # create ftp-connection
  with ftputil.FTPHost(
    host=ftp_config['Copernicus'][server],
    user=ftp_config['Copernicus']['user'],
    passwd=ftp_config['Copernicus']['password'])as ftp:

    c = create_connection(cmems_db)

  # CHECK IF FTP IS EMPTY 
    logging.debug('Checking FTP directory')
    directory_not_empty = check_directory(ftp, nrt_dir) 
    if directory_not_empty:
      logging.error('Previous export has failed, \
        clean up remanent files before re-exporting')
    else:
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
      if len(results_upload) == 0:
        status = 2 
        logging.debug('All files already exported')
      else:
        logging.debug(f'Upload {len(results_upload)}: {results_upload}')

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
          status = 0
          error += 'Building index failed: ' + str(e)
   
        # UPLOAD INDEX 
        if index_filename:
          try:
            upload_result, ftp_filepath, start_upload_time, stop_upload_time = (
              upload_to_ftp(ftp,ftp_config, index_filename))
            logging.debug(f'index upload result: {upload_result}')
          except Exception as e:
            logging.error('Uploading index failed: ', exc_info=True)
            status = 0      
            error += 'Uploading index failed: ' + str(e)
        
          # BUILD DNT-FILE
          # Adding index file to DNT-list:
          dnt_upload[index_filename] = ({
            'ftp_filepath':ftp_filepath, 
            'start_upload_time':start_upload_time, 
            'stop_upload_time':stop_upload_time,
            'local_filepath': index_filename,
            })

        
        # INDEX platform
        try:
          index_platform = build_index_platform(c,platform)
        except Exception as e:
          logging.error('Building platform index failed: ', exc_info=True)
          status = 0
          error += 'Building platform index failed: ' + str(e)
   
        if index_platform:
          try:
            upload_result, ftp_filepath, start_upload_time, stop_upload_time = (
              upload_to_ftp(ftp,ftp_config, index_platform))
            logging.debug(f'index platform upload result: {upload_result}')
          except Exception as e:
            logging.error('Uploading platform index failed: ', exc_info=True)
            status = 0      
            error += 'Uploading platform index failed: ' + str(e)
        
          # BUILD DNT-FILE
          # Adding index file to DNT-list:
          dnt_upload[index_platform] = ({
            'ftp_filepath':ftp_filepath, 
            'start_upload_time':start_upload_time, 
            'stop_upload_time':stop_upload_time,
            'local_filepath': index_platform,

            })

        logging.info('Building and uploading DNT-file')
        try:
          dnt_file, dnt_local_filepath = build_DNT(dnt_upload,dnt_delete)

          # UPLOAD DNT-FILE
          _, dnt_ftp_filepath, _, _ = (
            upload_to_ftp(ftp, ftp_config, dnt_local_filepath))
          
          logging.debug('Updating database to include DNT filename')
          sql_rec = "UPDATE latest SET dnt_file = ? WHERE dnt_file = ?"
          sql_var = [dnt_local_filepath, curr_date]
          c.execute(sql_rec,sql_var)

          try:
            response = evaluate_response_file(
              ftp,dnt_ftp_filepath,dnt_local_filepath.rsplit('/',1)[0],cmems_db)
            logging.debug('cmems dnt-response: {}'.format(response))
            if len(response) == 0: status = 1

          except Exception as e:
            logging.error('No response from CMEMS: ', exc_info=True)
            status = 0
            error += 'No response from CMEMS: ' + str(e)

        except Exception as exception:
          logging.error('Building DNT failed: ', exc_info=True)
          status = 0
          error += 'Building DNT failed: ' + str(exception)

        # FOLDER CLEAN UP
        if dnt_delete:
          logging.info('Delete empty directories')
          try: 
            _, dnt_local_filepath_f = build_fDNT(dnt_delete)

            _, dnt_ftp_filepath_f, _, _ = (
              upload_to_ftp(ftp, ftp_config, dnt_local_filepath_f))  
            try:
              response = evaluate_response_file(
                ftp,dnt_ftp_filepath_f,dnt_local_filepath_f.rsplit('/',1)[0],cmems_db)
              logging.debug('cmems fDNT-response, delete empty folders: {}'.format(response))

            except Exception as e:
              logging.error('No response from CMEMS: ', exc_info=True)
              error += 'No response from CMEMS: ' + str(e)

          except Exception as e:
            logging.error('Uploading fDNT failed: ', exc_info=True)
            error += 'Uploading fDNT failed: ' + str(e)

      if status == 0:
        logging.error('Upload failed')
        error_msg = abort_upload(error, ftp, nrt_dir, c, curr_date)
        
    return status, error_msg

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
  return responsedef abort_upload(error,ftp,nrt_dir,c,curr_date):
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

  return error_msg

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
    upload_response_log += (date + ',' + folder_local + ',' + 
      dnt_filepath + ',' + cmems_response + ',\n')
  
  if os.path.isfile(log_file):
    with open(log_file,'a+') as log: 
      log.write(upload_response_log)
  else: 
    with open(log_file,'w') as log: 
      log.write('date, local filepath, cmems filepath, cmems response\n')
      log.write(upload_response_log)
      logging.debug(f'log-message: {upload_response_log}')

  return rejected_list
  
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
