'''
Script that generates monthly netCDF file from folder of daily netCDF files.

Retrieves total dimension size of new netCDF files.
Iterates through daily files to extract values.
Creates list of values for each variable.
Populates new netCDF file.

'''

import netCDF4
import os
import numpy as np
import datetime
import logging
import sys

logging.basicConfig(stream=sys.stdout, level=logging.DEBUG)

curr_month = datetime.datetime.today().strftime('%Y%m') 
vesselnames= {'LMEL': 'G.O.Sars','OXYH2':'Nuka Arctica'} 
vessels = ['LMEL', 'OXYH2']
directory = 'latest' 
dim_tot = {}
file_nr = {}
daily_files = {}

def main():
  logging.debug('Retrieving list of daily netCDF files')
  for vessel in vessels:
    daily_files[vessel], file_nr[vessel], dataset, dim_tot = (get_daily_files(directory,curr_month,vessel))
    if file_nr[vessel] > 0:
      logging.info(f'Creating monthly netCDF file for {vesselnames[vessel]} [{vessel}], month: {curr_month}')
      dataset_m = create_empty_dataset(curr_month,vessel,dim_tot)
      dataset_m = assign_attributes(dataset,dataset_m)
      dataset_m = populate_netCDF(dataset,dataset_m,daily_files[vessel],directory)
      dataset_m = set_global_attributes(dataset,dataset_m)
      dataset_m.close()
      logging.info(f'Monthly netCDF file for {vesselnames[vessel]} completed')

      # Create Index file

      # Create DNT file

      # Upload files to CMEMS-FTP



def get_daily_files(directory,curr_month,vessel):
  '''Fetches applicable daily files from directory
  returns filenames, number of files, last dataset 
  and combined dimension of all files
  '''
  file_nr = 0
  filenames =[]
  for root, dirs, files in os.walk( directory, topdown=False):
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

  return dataset_m

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
      

def populate_netCDF(dataset,dataset_m,daily_files,directory):
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
      dataset_d = netCDF4.Dataset(os.path.join(directory, file))
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


if __name__ == '__main__':
  main()

