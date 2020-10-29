'''
Script that generates monthly netCDF file from folder of daily netCDF files.

Retrieves total dimension size of new netCDF files.
Iterates through daily files to extract values.
Creates list of values for each variable.
Populates new netCDF file.

'''

import logging
import os
import sys
import datetime as DT
import numpy as np
import pandas as pd
import xml.etree.ElementTree as ET
import netCDF4
import sqlite3
import ftputil 
import hashlib
import toml
import time
import re

if not os.path.isdir('log'):  os.mkdir('log')
log = 'log/console_monthly.log'
logging.basicConfig(filename='log/monthly_console.log',format='%(asctime)s {%(filename)s:%(lineno)d} %(levelname)s - %(message)s', level=logging.DEBUG)
#logging.basicConfig(stream=sys.stdout,format='%(asctime)s {%(filename)s:%(lineno)d} %(levelname)s - %(message)s', level=logging.DEBUG)

VESSELS = {'LMEL': 'G.O.Sars','OXYH2':'Nuka Arctica'} 
SOURCE_DIR = 'latest' 

CMEMS_DB = 'database_cmems.db'

DNT_DATETIME_FORMAT = '%Y%m%dT%H%M%SZ'

SERVER_LOCATION = 'ftp://nrt.cmems-du.eu/Core'

PRODUCT_ID = 'INSITU_GLO_CARBON_NRT_OBSERVATIONS_013_049'
NC_DIR_MONTHLY = '/' + PRODUCT_ID + '/NRT_202003/monthly/vessel'
NC_DIR_HISTORY = '/' + PRODUCT_ID + '/NRT_202003/history/vessel'
DNT_DIR = '/' + PRODUCT_ID + '/DNT'
INDEX_DIR = '/' + PRODUCT_ID + '/NRT_202003'
MONTHLY_FOLDER = 'monthly'
HISTORY_FOLDER = 'history'
DNT_FOLDER = 'DNT/monthly_historical/'  

if not os.path.isdir(MONTHLY_FOLDER):  os.mkdir(MONTHLY_FOLDER)
if not os.path.isdir(HISTORY_FOLDER): os.mkdir(HISTORY_FOLDER)
if not os.path.isdir(DNT_FOLDER):  os.mkdir(DNT_FOLDER)

# Upload result codes
UPLOAD_OK = 0
FILE_EXISTS = 2
# Response codes
UPLOADED = 1
NOT_UPLOADED = 0
FAILED_INGESTION = -1

config_file_copernicus = 'config_copernicus.toml'
with open(config_file_copernicus) as f: ftp_config = toml.load(f)

daily_files = {}
dim_tot = {}
file_nr = {}

def main():
  nc_dict = {}
  
  # Creating monthly netCDF files
  months = (pd.date_range('2019-03-01',DT.datetime.today() 
    - DT.timedelta(days=14), freq='MS').strftime("%Y%m").tolist())
  for month in months:
    nc_dict = generating_monthly_netCDF(VESSELS,SOURCE_DIR,month,nc_dict)

  # Creating historical netCDF files
  nc_dict = generating_history_netCDF(VESSELS,SOURCE_DIR,nc_dict)

  # Add new netCDFs to SQL database
  sql_commit(nc_dict)

  # Upload new netCDF files to CMEMS
  upload_to_copernicus()


def generating_monthly_netCDF(VESSELS,SOURCE_DIR,month,nc_dict):
  ''' Creates netCDF file(s) of data based on month and vessel(s), returns dicionary containing list of file(s) created'''
  for vessel in VESSELS.keys():
    logging.debug(f'Retrieving list of daily netCDF files for {month} {vessel}')
    daily_files[vessel], file_nr[vessel], dataset, dim_tot = (
      get_daily_files(SOURCE_DIR,month,vessel))

    if file_nr[vessel] > 0:
      logging.info(f'Creating monthly netCDF file for {VESSELS[vessel]} [{vessel}], month: {month}')
      nc_name, dataset_m = create_empty_dataset(month,vessel,dim_tot)
      dataset_m = assign_attributes(dataset,dataset_m)
      dataset_m = populate_netCDF(dataset,dataset_m,daily_files[vessel],SOURCE_DIR)
      dataset_m = set_global_attributes(dataset,dataset_m)
      dataset_m.close()
      logging.info(f'Monthly netCDF file for {VESSELS[vessel]} completed')
      nc_dict[vessel+'_'+month] = sql_entry(nc_name,month)
  return nc_dict


def generating_history_netCDF(VESSELS,SOURCE_DIR,nc_dict):
  ''' Creates netCDF file(s) of all data associated with vessel(s), returns dicionary containing list of file(s) created'''
  for vessel in VESSELS.keys():
    logging.debug(f'Retrieving list of all daily netCDF files for {vessel}')
    daily_files[vessel], file_nr[vessel], dataset, dim_tot = (
      get_daily_files(SOURCE_DIR,'history',vessel))

    if file_nr[vessel] > 0:
      logging.info(f'Creating historical netCDF file for {VESSELS[vessel]} [{vessel}]')
      nc_name, dataset_m = create_empty_dataset('history',vessel,dim_tot)
      dataset_m = assign_attributes(dataset,dataset_m)
      dataset_m = populate_netCDF(dataset,dataset_m,daily_files[vessel],SOURCE_DIR)
      dataset_m = set_global_attributes(dataset,dataset_m)
      dataset_m.close()
      logging.info(f'Historical netCDF file for {VESSELS[vessel]} completed')
      nc_dict[vessel+'_history'] = sql_entry(nc_name,'history')
  return nc_dict


def get_daily_files(SOURCE_DIR,month,vessel):
  ''' Fetches applicable daily files from SOURCE_DIR
  returns filenames, number of files, last dataset 
  and combined dimension of all files
  '''
  file_nr = 0
  filenames =[]
  for root, dirs, files in os.walk( SOURCE_DIR, topdown=False):
    dim_tot=0
    for name in sorted(files):
      if (month in name or month == 'history') and vessel in name:
        file_nr+=1
        logging.debug(f'reading filename: {name}')
        filenames += [name]
        dataset = netCDF4.Dataset(os.path.join(root, name))
        dim_tot += len(dataset.dimensions['TIME'])
  
  if len(filenames) == 0: dataset = None

  return filenames, file_nr, dataset, dim_tot


def create_empty_dataset(month,vessel,dim_tot):
  ''' Creates empty netCDF file with correct dimensions '''
  if month == 'history': 
    nc_name = 'history/GL_TS_'+ vessel + '.nc'
  else:
    nc_name = 'monthly/GL_' + str(month) + '_TS_TS_'  + vessel + '.nc'
  logging.debug(f'Creating new empty file: {nc_name}')
  dataset_m = netCDF4.Dataset(nc_name,'w',format='NETCDF4_CLASSIC')

  logging.debug('Assigning dimensions')
  depth_dim = dataset_m.createDimension('DEPTH',1)
  time_dim = dataset_m.createDimension('TIME',dim_tot)
  lat_dim = dataset_m.createDimension('LATITUDE',dim_tot)
  lon_dim = dataset_m.createDimension('LONGITUDE',dim_tot)
  pos_dim = dataset_m.createDimension('POSITION',dim_tot)

  return nc_name,dataset_m


def assign_attributes(dataset,dataset_m):
  ''' Assigns attributes to dataset_m '''
  logging.debug('Assigning attributes/variables')
  variables = dataset.variables.keys()
  for var in variables:
    set_attr = {}
    if '_FillValue' in dataset[var].ncattrs():
        fill = dataset[var]._FillValue
    else: fill = None
    variable = dataset_m.createVariable(
        var,dataset[var].dtype,dataset[var].dimensions,fill_value = fill)
    for attr in dataset[var].ncattrs():
      if '_FillValue' in attr:
        continue
      attr_val = dataset[var].getncattr(attr)
      set_attr[attr] = attr_val
    variable.setncatts(set_attr)
  return dataset_m


def populate_netCDF(dataset,dataset_m,daily_files,SOURCE_DIR):
  ''' For each variable in each daily netCDF file:
  extract data from daily file; save data to new dataset; dataset_m.
  '''
  logging.debug('populating monthly nc-file')
  start = {}
  end = {}
  for var in dataset.variables.keys():        
    start[var]=0
    for file in sorted(daily_files):
      dataset_d = netCDF4.Dataset(os.path.join(SOURCE_DIR, file))
      dim_len = len(dataset_d[var].dimensions) 
      if dim_len == 1:
        array = dataset_d[var][:]
        end[var] = start[var] + len(array)
        dataset_m[var][start[var]:end[var]] = array 
        start[var] = end[var]

      elif dim_len == 2:
        array = dataset_d[var][:,:]
        end[var] = start[var] + len(array)
        dataset_m[var][start[var]:end[var]] = array 
        start[var] = end[var]   

  return dataset_m        


def set_global_attributes(dataset,dataset_m):
  ''' Assigns global attributes to new file, based on most resent dataset's attributes '''
  set_gattr = {}

  # Setting monthly file to same global attributes as a daily dataset 
  for gattr in dataset.ncattrs():
    set_gattr[gattr] = dataset.getncattr(gattr)

  # Overwriting attributes specific to this file.
  start_date = (DT.datetime(1950,1,1,0,0) 
    + DT.timedelta(min(dataset_m['TIME'][:]))).strftime("%Y-%m-%dT%H:%M:%SZ")
  end_date = (DT.datetime(1950,1,1,0,0) 
    + DT.timedelta(max(dataset_m['TIME'][:]))).strftime("%Y-%m-%dT%H:%M:%SZ")
  set_gattr['geospatial_lat_min'] = min(dataset_m['LATITUDE'][:])
  set_gattr['geospatial_lat_max'] = max(dataset_m['LATITUDE'][:])
  set_gattr['geospatial_lon_min'] = min(dataset_m['LONGITUDE'][:])
  set_gattr['geospatial_lon_max'] = max(dataset_m['LONGITUDE'][:])
  set_gattr['time_coverage_start'] = start_date
  set_gattr['time_coverage_end'] = end_date
  set_gattr['update_interval']='void'

  # Assigning attributes to dataset
  dataset_m.setncatts(set_gattr)

  return dataset_m


def sql_entry(nc_name,month):
  ''' Creates dictionary object to submit to database '''
  with open(nc_name,'rb') as f: 
    file_bytes = f.read()
    hashsum = hashlib.md5(file_bytes).hexdigest()
  entry = ({
    'filepath':nc_name, 
    'hashsum': hashsum, 
    'date': month, 
    'uploaded':False})
  return entry


def sql_commit(nc_dict):
  '''  creates SQL table if non exists
  adds new netCDF files, listed in nc_dict, to new or existing SQL-table 
  '''
  c = create_connection(CMEMS_DB)
  date = DT.datetime.now().strftime(DNT_DATETIME_FORMAT)

  for key in nc_dict:
    if nc_dict[key]['uploaded']: 
      uploaded = 1
    else: 
      uploaded = 0
    c.execute("SELECT * FROM monthly WHERE filename=? ",[key])
    filename_exists = c.fetchone()
    
    if filename_exists: # if netCDF file already in database
      logging.info(f'Updating: {key}')
      sql_req = "UPDATE monthly SET filename=?,hashsum=?,filepath=?,month=?,\
        uploaded=?,ftp_filepath=?,dnt_file=?,comment=?,export_date=? \
        WHERE filename=?"
      sql_param = ([key,nc_dict[key]['hashsum'],nc_dict[key]['filepath'],
        nc_dict[key]['date'],uploaded,None,None,None,key,date])
    else:
      logging.debug(f'Adding new entry {key}')
      sql_req = "INSERT INTO monthly(filename,hashsum,filepath,month,\
        uploaded,ftp_filepath,dnt_file,comment,export_date) \
        VALUES (?,?,?,?,?,?,?,?,?)"
      sql_param = ([key,nc_dict[key]['hashsum'],nc_dict[key]['filepath'],
        nc_dict[key]['date'],uploaded,None,None,None,date])
    c.execute(sql_req,sql_param)


def create_connection(DB):
  ''' creates connection and database if not already created '''
  conn = sqlite3.connect(DB, isolation_level=None)
  c = conn.cursor()
  c.execute('''CREATE TABLE IF NOT EXISTS monthly (
              filename TEXT PRIMARY KEY,
              hashsum TEXT NOT NULL,
              filepath TEXT NOT NULL UNIQUE,
              month TEXT,
              uploaded INTEGER,
              ftp_filepath TEXT,
              dnt_file TEXT,
              comment TEXT,
              export_date TEXT
              )''')
  return c


def upload_to_copernicus():
  dest = DT.datetime.now().strftime("%Y%m%d")
  dnt_upload = {}
  with ftputil.FTPHost(
    host=ftp_config['Copernicus']['nrt_server'],
    user=ftp_config['Copernicus']['user'],
    passwd=ftp_config['Copernicus']['password'])as ftp:
    c = create_connection(CMEMS_DB)

  # CHECK IF FTP IS EMPTY 
    logging.debug('Checking FTP directory')
    directory_empty = check_directory(ftp, NC_DIR_MONTHLY) 
    if not directory_empty:
      logging.error('Monthly: Previous export has failed, \
        clean up remanent files before re-exporting. Aborting export')
      return False 

    directory_empty = check_directory(ftp, NC_DIR_HISTORY) 
    if not directory_empty:
      logging.error('History: Previous export has failed, \
        clean up remanent files before re-exporting. Aborting export')
      return False 

  # Fetch all to be uploaded
    c.execute("SELECT * FROM monthly WHERE uploaded == 0")
    ready_for_upload = c.fetchall()  
    if ready_for_upload:
      for file in ready_for_upload:
        upload_result, ftp_filepath, t_start, t_stop = (
          upload_to_ftp(ftp, ftp_config, file[2],file[3]))
        if upload_result == 0:
          c.execute("UPDATE monthly \
            SET uploaded = ?, ftp_filepath = ?, dnt_file = ? \
            WHERE filename = ?", [UPLOADED, ftp_filepath,dest,file[0]])
          # Create DNT-entry
          dnt_upload[file[0]] = ({
            'ftp_filepath':ftp_filepath, 
            't_start':t_start, 't_stop':t_stop,
            'local_filepath':file[2]})    
          logging.debug(f'dnt entry: {dnt_upload[file[0]]}') 
        else:
          logging.debug(f'upload failed: {upload_result}')

      # Create Monthly Index file
      c.execute("SELECT * FROM monthly WHERE (uploaded == 1 and month != 'history')")
      currently_uploaded = c.fetchall()
      monthly_index_filename = build_index(currently_uploaded,'index_monthly.txt')

      try:
        upload_result, ftp_filepath, t_start, t_stop = (
          upload_to_ftp(ftp,ftp_config, monthly_index_filename))
      except Exception as e:
        logging.error('Uploading monthly index failed: ', exc_info=True)

      # Adding index file to DNT-list:
      dnt_upload[monthly_index_filename] = ({
        'ftp_filepath':ftp_filepath, 
        't_start':t_start, 't_stop':t_stop,
        'local_filepath': monthly_index_filename
        })

      # Create History Index file
      c.execute("SELECT * FROM monthly WHERE (uploaded == 1 and month == 'history')")
      currently_uploaded = c.fetchall()
      history_index_filename = build_index(currently_uploaded,'index_history.txt')

      try:
        upload_result, ftp_filepath, t_start, t_stop = (
          upload_to_ftp(ftp,ftp_config, history_index_filename))
      except Exception as e:
        logging.error('Uploading history index failed: ', exc_info=True)

      # Adding index file to DNT-list:
      dnt_upload[history_index_filename] = ({
        'ftp_filepath':ftp_filepath, 
        't_start':t_start, 't_stop':t_stop,
        'local_filepath': history_index_filename
        })

      # BUILD AND UPLOAD DNT-FILE
      logging.info('Building DNT-file')
      try:
        dnt_file, dnt_local_filepath = build_DNT(dnt_upload)
        _, dnt_ftp_filepath, _, _ = (
          upload_to_ftp(ftp, ftp_config, dnt_local_filepath))
        logging.info('Updating database to include DNT filename')
        sql_rec = "UPDATE monthly SET dnt_file = ? WHERE dnt_file = ?"
        sql_var = [dnt_local_filepath, dest]
        c.execute(sql_rec,sql_var)

        try:
          evaluate_response_file(
            ftp,dnt_ftp_filepath,dnt_local_filepath.rsplit('/',1)[0],CMEMS_DB)

        except Exception as e:
          logging.error('No response from CMEMS: ', exc_info=True)
      except Exception as exception:
        logging.error('Building DNT failed: ', exc_info=True)


def check_directory(ftp, nrt_dir):
  ''' Cleans out empty folders, checks if main directory is empty. '''
  if not ftp.path.isdir(nrt_dir): ftp.mkdir(nrt_dir)

  clean_directory(ftp, nrt_dir)

  if ftp.listdir(nrt_dir):
    logging.warning('ftp-folder is not empty')
    return False 
  else:
    logging.debug(f'{nrt_dir} is clean')
    return True


def clean_directory(ftp,nrt_dir):
  ''' Removes empty directories from ftp server '''
  uningested_files = []
  for dirpath, dirnames, files in ftp.walk(nrt_dir+'/'):
    if not dirnames and not files and not dirpath.endswith('/vessel/'):
      logging.debug(f'removing EMPTY DIRECTORY: {str(dirpath)}') 
      ftp.rmdir(dirpath)
    elif files:
      uningested_files += (
        [[('dirpath',dirpath),('dirnames',dirnames),('files',files)]])
      logging.debug(f'UNINGESTED: \
        dirpath: {dirpath}, \ndirnames: {dirnames}, \nfiles: {files}')
  return uningested_files


def upload_to_ftp(ftp, ftp_config, filepath,dest_folder=None):
  ''' Uploads file with location 'filepath' to an ftp-server, 
  server-location set by 'directory' parameter and config-file, 
  ftp is the ftp-connection

  returns 
  upload_result: upload_ok or file_exists
  dest_filepath: target filepath on ftp-server
  t_start and t_stop: timestamps of upload process
  '''
  upload_result = UPLOAD_OK
  logging.debug(f'Ready for upload: {filepath}')
  ftp_folder,ftp_filepath = get_destination(filepath,dest_folder)

  t_start = DT.datetime.now().strftime(DNT_DATETIME_FORMAT)
  if not ftp.path.isdir(ftp_folder):
    ftp.mkdir(ftp_folder)
    ftp.upload(filepath, ftp_filepath)
  elif ftp.path.isfile(ftp_filepath):
    upload_result = FILE_EXISTS
  else:
    ftp.upload(filepath, ftp_filepath)
  t_stop = DT.datetime.now().strftime(DNT_DATETIME_FORMAT)
  logging.debug(f'upload result: {upload_result}')
  return upload_result, ftp_filepath, t_start, t_stop


def get_destination(filepath,dest_folder):
  if filepath.endswith('.nc'):
    filename = filepath.rsplit('/',1)[-1]
    if 'history' in filepath:
        ftp_folder = NC_DIR_HISTORY
    else:
        ftp_folder = NC_DIR_MONTHLY + '/' + dest_folder     
    ftp_filepath = ftp_folder + '/' +  filename
  elif filepath.endswith('.xml'):
    ftp_folder = DNT_DIR
    ftp_filepath = ftp_folder + '/' + filepath.rsplit('/',1)[-1]
  elif filepath.endswith('.txt'):
    with open(filepath,'rb') as f: 
      file_bytes = f.read() 
    ftp_folder = INDEX_DIR
    ftp_filepath = ftp_folder + '/' + filepath.rsplit('/',1)[-1]
  return ftp_folder, ftp_filepath


def build_DNT(dnt_upload):
  ''' Generates delivery note for NetCDF file upload, 
  note needed by Copernicus in order to move .nc-file to public-ftp
  dnt_upload contains list of files uploaded to the ftp-server
  '''
  date = DT.datetime.now().strftime(DNT_DATETIME_FORMAT)
  curr_month = (DT.datetime.today() - DT.timedelta(days=14)).strftime('%Y%m')
  dnt_file = PRODUCT_ID + '_P' + curr_month + '.xml'
  dnt_filepath = DNT_FOLDER + dnt_file

  dnt = ET.Element('delivery')
  dnt.set('PushingEntity','CopernicusMarine-InSitu-Global')
  dnt.set('date', date)
  dnt.set('product',PRODUCT_ID)
  dataset = ET.SubElement(dnt,'dataset')
  dataset.set('DatasetName','NRT_202003')

  for item in dnt_upload:
    local_filepath = dnt_upload[item]['local_filepath']
    ftp_filepath = dnt_upload[item]['ftp_filepath'].split('/',3)[-1]
    t_start = dnt_upload[item]['t_start'] 
    t_stop = dnt_upload[item]['t_stop']

    file = ET.SubElement(dataset,'file')
    file.set('Checksum',hashlib.md5(open(local_filepath,'rb').read()).hexdigest())
    file.set('FileName',ftp_filepath)
    file.set('FinalStatus','Delivered')
    file.set('StartUploadTime',t_start)
    file.set('StopUploadTime',t_stop)

  xml_tree = ET.ElementTree(dnt)

  with open(dnt_filepath,'wb') as xml: 
    xml_tree.write(xml,xml_declaration=True,method='xml')

  return dnt_file, dnt_filepath


def build_index(results_uploaded,index_filename):
  '''  Creates index-file over CMEMS SOURCE_DIR.
  Lists all files currently uploaded to the CMEMS server. 
  '''
  date_header = DT.datetime.now().strftime('%Y%m%d%H%M%S')
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
    ftp_filepath = file[5].replace('NRT_202003','NRT')
    
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

    index_info += ('COP-GLOBAL-01,' + SERVER_LOCATION + ftp_filepath + ',' 
                + str(lat_min) + ',' + str(lat_max) + ',' 
                + str(lon_min) + ',' + str(lon_max) + ',' 
                + time_start + ',' + time_end  
                + ',University of Bergen Geophysical Institute,' 
                + date_update + ',R,' + parameters + '\n')

  index = index_header + index_info
  with open(index_filename,'wb') as f: f.write(index.encode())
  logging.debug('index file:\n' + index)

  return index_filename


def evaluate_response_file(ftp,dnt_filepath,folder_local,CMEMS_DB):
  '''  Retrieves response from cmems-ftp server.  '''
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
      logging.info('cmems response: ' + cmems_response)
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

        c = create_connection(CMEMS_DB)
        sql_req = "UPDATE monthly SET uploaded=?,comment=? WHERE filename=?"
        sql_var = ([-1,rejected_reason, rejected_filename])
        c.execute(sql_req,sql_var)

    else:
      logging.info('All files ingested')
    date = DT.datetime.now().strftime('%Y-%m-%d')
 
  logging.info({'date':date,'local folder':folder_local,'dnt_filepath':dnt_filepath,'cmems-response':cmems_response})
  
  if rejected_list:
    logging.debug('Rejected: {}'.format(rejected_list))


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


if __name__ == '__main__':
  main()