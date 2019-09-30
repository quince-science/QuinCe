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
import datetime
import numpy as np
import netCDF4
import sqlite3
import ftputil 
import hashlib
import toml

logging.basicConfig(stream=sys.stdout, level=logging.DEBUG)

curr_month = datetime.datetime.today().strftime('%Y%m') 
vesselnames= {'LMEL': 'G.O.Sars','OXYH2':'Nuka Arctica'} 
vessels = ['LMEL', 'OXYH2']
source_dir = 'latest' 
dim_tot = {}
file_nr = {}
daily_files = {}
nc_dict = {}

cmems_db = 'files_cmems.db'

dnt_datetime_format = '%Y%m%dT%H%M%SZ'

server_location = 'ftp://nrt.cmems-du.eu/Core'

product_id = 'INSITU_GLO_CARBON_NRT_OBSERVATIONS_013_049'
nc_dir = '/' + product_id + '/NRT_201904/monthly/vessel'
dnt_dir = '/' + product_id + '/DNT'
index_dir = '/' + product_id + '/NRT_201904'
local_folder = 'monthly'

# Upload result codes
UPLOAD_OK = 0
FILE_EXISTS = 2

# Response codes
UPLOADED = 1
NOT_UPLOADED = 0
FAILED_INGESTION = -1

config_file_copernicus = 'config_copernicus.toml'
with open(config_file_copernicus) as f: ftp_config = toml.load(f)


def main():
  logging.debug('Retrieving list of daily netCDF files')
  for vessel in vessels:
    daily_files[vessel], file_nr[vessel], dataset, dim_tot = (get_daily_files(source_dir,curr_month,vessel))
    if file_nr[vessel] > 0:
      logging.info(f'Creating monthly netCDF file for {vesselnames[vessel]} [{vessel}], month: {curr_month}')
      nc_name, dataset_m = create_empty_dataset(curr_month,vessel,dim_tot)
      dataset_m = assign_attributes(dataset,dataset_m)
      dataset_m = populate_netCDF(dataset,dataset_m,daily_files[vessel],source_dir)
      dataset_m = set_global_attributes(dataset,dataset_m)
      dataset_m.close()
      logging.info(f'Monthly netCDF file for {vesselnames[vessel]} completed')
      
      nc_dict[vessel+'_'+curr_month] = sql_entry(nc_name,curr_month)

  # Add to SQL database
  sql_commit(nc_dict)

  # upload nc files to cmems
  upload_to_copernicus()
  # create ftp-connection

def upload_to_copernicus():
  curr_date = datetime.datetime.now().strftime("%Y%m%d")
  dnt_upload = {}
  with ftputil.FTPHost(
    host=ftp_config['Copernicus']['nrt_server'],
    user=ftp_config['Copernicus']['user'],
    passwd=ftp_config['Copernicus']['password'])as ftp:

    c = create_connection(cmems_db)

  # CHECK IF FTP IS EMPTY 
    logging.debug('Checking FTP directory')
    directory_empty = check_directory(ftp, nc_dir) 
    if directory_empty:
      logging.error('Previous export has failed, \
        clean up remanent files before re-exporting')
      return False 

  # Fetch all to be uploaded
    c.execute("SELECT * FROM monthly WHERE uploaded == 0")
    ready_for_upload = c.fetchall()  
    if ready_for_upload:
      logging.debug(f'ready for upload: {ready_for_upload[0]}')
      for file in ready_for_upload:
        filepath_local = file[2]

        upload_result, ftp_filepath, start_upload_time, stop_upload_time = (
          upload_to_ftp(ftp, ftp_config, filepath_local))
        logging.debug(f'upload result: {upload_result}')
        if upload_result == 0:
          # Setting dnt-variable to temp variable: curr_date.
          # After DNT is created, the DNT-filepath is updated for all  
          # instances where DNT-filetpath is curr_date
          print(type(UPLOADED),type(ftp_filepath),type(curr_date),type(file))
          c.execute("UPDATE monthly \
            SET uploaded = ?, ftp_filepath = ?, dnt_file = ? \
            WHERE filename = ?", [UPLOADED, ftp_filepath,curr_date,file[0]])

          # create DNT-entry
          dnt_upload[file[0]] = ({'ftp_filepath':ftp_filepath, 
            'start_upload_time':start_upload_time, 
            'stop_upload_time':stop_upload_time,
            'local_filepath':file[2]})    
          logging.debug(f'dnt entry: {dnt_upload[file[0]]}') 
        else:
          logging.debug(f'upload failed: {upload_result}')

      # Create Index file
      c.execute("SELECT * FROM monthly WHERE uploaded == 1")
      currently_uploaded = c.fetchall()
      index_filename = build_index(currently_uploaded)

      # Create DNT file
      

      # Upload dnt files to CMEMS-FTP


def check_directory(ftp, nrt_dir):
  '''   Cleans out empty folders, checks if main directory is empty. 
  returns True when empty 
  '''
  uningested_files = clean_directory(ftp, nrt_dir)
#  with open (not_ingested,'a+') as f:
#    for item in uningested_files:
#      f.write(str(datetime.datetime.now()) + ': ' + str(item) + '\n')
  if ftp.listdir(nrt_dir):
    logging.warning('ftp-folder is not empty')
    return True 
  else:
    return False

def clean_directory(ftp,nrt_dir):
  ''' removes empty directories from ftp server '''
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


def get_daily_files(source_dir,curr_month,vessel):
  '''Fetches applicable daily files from source_dir
  returns filenames, number of files, last dataset 
  and combined dimension of all files
  '''
  file_nr = 0
  filenames =[]
  for root, dirs, files in os.walk( source_dir, topdown=False):
    dim_tot=0
    for name in sorted(files):
      if curr_month in name and vessel in name:
        file_nr+=1
        logging.debug(f'reading filename: {name}')
        filenames += [name]
        dataset = netCDF4.Dataset(os.path.join(root, name))
        dim_depth = len(dataset.dimensions['DEPTH'])
        dim_time = len(dataset.dimensions['TIME'])
        dim_lat = len(dataset.dimensions['LATITUDE'])
        dim_lon = len(dataset.dimensions['LONGITUDE'])
        dim_pos = len(dataset.dimensions['POSITION'])
        dim_tot += dim_time

  return filenames, file_nr, dataset, dim_tot

def create_empty_dataset(curr_month,vessel,dim_tot):
  ''' Creates empty monthly file with correct dimensions
  returns dataset-object
  '''
  nc_name = 'monthly/GL_' + str(curr_month) + '_TS_TS_'  + vessel + '.nc'
  logging.debug(f'Creating new empty monthly file: {nc_name}')
  dataset_m = netCDF4.Dataset(nc_name,'w',format='NETCDF4_CLASSIC')

  logging.debug('Assigning dimensions')
  depth_dim = dataset_m.createDimension('DEPTH',1)
  time_dim = dataset_m.createDimension('TIME',dim_tot)
  lat_dim = dataset_m.createDimension('LATITUDE',dim_tot)
  lon_dim = dataset_m.createDimension('LONGITUDE',dim_tot)
  pos_dim = dataset_m.createDimension('POSITION',dim_tot)

  return nc_name,dataset_m

def assign_attributes(dataset,dataset_m):
  ''' Assigns attributes to dataset_m
  returns dataset_m
  '''
  logging.debug('Assigning attributes/variables')
  variables = dataset.variables.keys()
  for var in variables:
    fill = dataset[var]._FillValue
    variable = dataset_m.createVariable(var,dataset[var].dtype,dataset[var].dimensions,fill_value = fill)
    set_attr = {}
    for attr in dataset[var].ncattrs():
      if '_FillValue' in attr:
        continue
      attr_val = dataset[var].getncattr(attr)
      set_attr[attr] = attr_val
    variable.setncatts(set_attr)
  return dataset_m
      

def populate_netCDF(dataset,dataset_m,daily_files,source_dir):
  ''' For each variable in each daily netCDF file:
  extract data from daily file
  save data to monthly dataset.
  '''
  logging.debug('populating monthly nc-file')
  start = {}
  end = {}
  for var in dataset.variables.keys():        
    start[var]=0
    for file in sorted(daily_files):
      dataset_d = netCDF4.Dataset(os.path.join(source_dir, file))
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
  set_gattr = {}

  # Setting monthly file to same global attributes as daily dataset 
  for gattr in dataset.ncattrs():
    set_gattr[gattr] = dataset.getncattr(gattr)

  # Overwriting attributes specific to this file.
  start_date = (datetime.datetime(1950,1,1,0,0) + datetime.timedelta(min(dataset_m['TIME'][:]))).strftime("%Y-%m-%dT%H:%M:%SZ")
  end_date = (datetime.datetime(1950,1,1,0,0) + datetime.timedelta(max(dataset_m['TIME'][:]))).strftime("%Y-%m-%dT%H:%M:%SZ")
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

def build_index(results_uploaded):
  '''
  Creates index-file over CMEMS source_dir.

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
    ftp_filepath = file[5]

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
                + str(lat_min) + ',' + str(lat_max) + ',' + str(lon_min) + ',' + str(lon_max) + ',' 
                + time_start + ',' + time_end  
                + ',University of Bergen Geophysical Institute,' 
                + date_update + ',R,' + parameters + '\n')

  index = index_header + index_info

  index_filename = 'index_monthly.txt'
  with open(index_filename,'wb') as f: f.write(index.encode())
 
  logging.debug('index file:\n' + index)

  return index_filename


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


def sql_entry(nc_name,curr_month):
  ''' Creates dictionary object to submit to database
  '''
  with open(nc_name,'rb') as f: 
    file_bytes = f.read()
    hashsum = hashlib.md5(file_bytes).hexdigest()
  entry = ({
    'filepath':nc_name, 
    'hashsum': hashsum, 
    'date': curr_month, 
    'uploaded':False})
  return entry


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
    ftp_folder = nc_dir + '/' + curr_month     
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



if __name__ == '__main__':
  main()

