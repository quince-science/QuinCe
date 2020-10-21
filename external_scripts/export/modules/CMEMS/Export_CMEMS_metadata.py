
'''
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

def build_dataproduct(dataset_zip,dataset,key,destination_filename,platform):
  '''
  transforms csv-file to daily netCDF-files.
  Creates dictionary containing info on each netCDF file extracted
  requires: zip-folder, dataset-name and specific filename of csv-file.
  returns: dictionary
  '''
  # BUILD netCDF FILES

  data_filename = (dataset['name'] + '/dataset/' + "Copernicus" + key 
  + dataset['name'] + '.csv')

  # Load field config
  fieldconfig = pd.read_csv('fields.csv', delimiter=',', quotechar='\'')

  csv_file = get_file_from_zip(dataset_zip, destination_filename)  
  
  curr_date = datetime.datetime.now().strftime("%Y%m%d")
  
  if not os.path.exists(local_folder): os.mkdir(local_folder)

  logging.info(f'Creating netcdf-files based on {csv_file} to send to CMEMS')

  filedata = pd.read_csv(csv_file, delimiter=',')
  nc_files = buildnetcdfs(dataset_name, fieldconfig, filedata,platform)
   
  nc_dict = {}
  for nc_file in nc_files:
    (nc_filename, nc_content) = nc_file
    hashsum = hashlib.md5(nc_content).hexdigest()
    logging.debug(f'Processing netCDF file {nc_filename}')

    # ASSIGN DATE-VARIABLES TO netCDF FILE
    nc_filepath = local_folder + '/' + nc_filename + '.nc'   

    with open(nc_filepath,'wb') as f: f.write(nc_content)

    # reading netCDF file to memory
    nc = netCDF4.Dataset(nc_filepath,mode = 'r+')
    datasetdate = datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")

    nc.date_update = datasetdate
    nc.history = datasetdate + " : Creation"

    platform_code = nc.platform_code
    last_lat = nc.last_latitude_observation
    last_lon = nc.last_longitude_observation
    last_dt = nc.last_date_observation 

    #get list of parameters from netCDF file
    var_list = nc.variables.keys()
    var_list = list(filter(lambda x: '_' not in x, var_list))
    var_list = list(filter(lambda x: 'TIME' not in x, var_list))
    var_list = list(filter(lambda x: 'LATITUDE' not in x, var_list))
    var_list = list(filter(lambda x: 'LONGITUDE' not in x, var_list))
    parameters = ' '.join(var_list)
    nc.close()

    # create dictionary object
    date = nc_filename.split('_')[-1]
    date = datetime.datetime.strptime(date,'%Y%m%d')
    hashsum = hashlib.md5(nc_content).hexdigest()
    nc_dict[nc_filename] = ({
      'filepath':nc_filepath, 
      'hashsum': hashsum, 
      'date': date, 
      'dataset':dataset_name,
      'uploaded':False,
      'platform': platform_code,
      'parameters':parameters,
      'last_lat':last_lat,
      'last_lon':last_lon,
      'last_dt':last_dt})

  logging.debug(f'Commiting metadata to local SQL database {cmems_db}')
  sql_commit(nc_dict)
  return str(curr_date)


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
  dataset.set('DatasetName',dataset_id)

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
  dataset.set('DatasetName',dataset_id)

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

def build_index(currently_uploaded):
  '''
  Creates index-file of CMEMS directory.
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
  for file in currently_uploaded:
    local_filepath = file[2]
    ftp_filepath = file[6].replace(dataset_id,'NRT') # Upload URL differs from host URL  /NRT_202003/ --> /NRT/

    nc = netCDF4.Dataset(local_filepath,mode='r')

    lat_min = nc.geospatial_lat_min
    lat_max = nc.geospatial_lat_max
    lon_min = nc.geospatial_lon_min
    lon_max = nc.geospatial_lon_max
    time_start = nc.time_coverage_start
    time_end  = nc.time_coverage_end
    date_update = nc.date_update
    nc.close()

    parameters = str(file[11])

    index_info += ('COP-GLOBAL-01,' + server_location + ftp_filepath + ',' 
                + lat_min + ',' + lat_max + ',' + lon_min + ',' + lon_max + ',' 
                + time_start + ',' + time_end  + ',' + institution +',' 
                + date_update + ',R,' + parameters + '\n')

  index_latest = index_header + index_info

  index_filename = 'index_latest.txt'
  with open(index_filename,'wb') as f: f.write(index_latest.encode())
 
  logging.debug('index file:\n' + index_latest)

  return index_filename

def build_index_platform(c,platform):
  '''
  Creates index-file of CMEMS directory.
  Lists all platforms uploaded to the CMEMS server. 
  '''

  date_header = datetime.datetime.now().strftime('%Y%m%d%H%M%S')

  index_header = ('# Title : In Situ platforms catalog \n'\
    + '# Description : catalog of available In Situ platforms.\n'\
    + '# Project : Copernicus \n# Format version : 1.0 \n'\
    + '# Date of update : ' + date_header +'\n'
    + '# platform_code,creation_date,update_date,wmo_platform_code,data_source,'
    + 'institution,institution_edmo_code,parameter,last_latitude_observation,'
    + 'last_longitude_observation,last_date_observation \n')

  # Get unique platforms from db
  c.execute("SELECT DISTINCT platform FROM latest")
  unique_platforms = c.fetchall()
  logging.debug(unique_platforms)
  if (None,) in unique_platforms: unique_platforms.remove((None,))

  index_info = ''
  for unique_platform in unique_platforms:
    platform_id = platform[unique_platform[0]]['platform_id']
    c.execute("SELECT * FROM latest WHERE platform = ? ORDER BY last_dt DESC",
      [unique_platform[0]])
    db_last = c.fetchone()
    
    index_info += (platform[platform_id]['call_sign'] + ',' 
      + str(platform[platform_id]['creation_date']) + ','
      + str(db_last[9]) + ',' 
      + platform_id + ',' 
      + 'GL_TS_TS_' + platform[platform_id]['call_sign'] + '_XXXXXX,' 
      + institution + ',' + institution_edmo + ',' 
      + str(db_last[11]) + ',' 
      + str(db_last[12]) + ',' 
      + str(db_last[13]) + ',' 
      + str(db_last[14]) + '\n')

  index_platform = index_header + index_info

  index_filename = 'index_platform.txt'
  with open(index_filename,'wb') as f: f.write(index_platform.encode())
 
  logging.debug('index file:\n' + index_platform)

  return index_filename
