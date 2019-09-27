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

logging.basicConfig(stream=sys.stdout, level=logging.DEBUG)

curr_month = datetime.datetime.today().strftime('%Y%m') 
vesselnames= {'LMEL': 'G.O.Sars','OXYH2':'Nuka Arctica'} 
vessels = ['LMEL', 'OXYH2']
source_dir = 'latest' 
dim_tot = {}
file_nr = {}
daily_files = {}

cmems_db = 'files_cmems.db'

dnt_datetime_format = '%Y%m%dT%H%M%SZ'

server_location = 'ftp://nrt.cmems-du.eu/Core'

product_id = 'INSITU_GLO_CARBON_NRT_OBSERVATIONS_013_049'
nc_dir = '/' + product_id + '/NRT_201904/monthly'
dnt_dir = '/' + product_id + '/DNT'
index_dir = '/' + product_id + '/NRT_201904'
local_folder = 'monthly'


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

      # Create SQL entry

  # Add to SQL database

  # Create Index file

  # Create DNT file

  # Upload files to CMEMS-FTP



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

  index = index_header + index_info

  index_filename = 'index_monthly.txt'
  with open(index_filename,'wb') as f: f.write(index.encode())
 
  logging.debug('index file:\n' + index)

  return index_filename


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



if __name__ == '__main__':
  main()

