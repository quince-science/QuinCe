
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
              export_date TEXT,
              platform TEXT,
              parameters TEXT,
              last_lat TEXT,
              last_lon TEXT,
              last_dt TEXT
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
      logging.debug(f'Updating: {key}')
      sql_req = "UPDATE latest SET hashsum=?,filepath=?,nc_date=?,\
        dataset=?,uploaded=?,ftp_filepath=?,dnt_file=?,comment=?,\
        platform=?,parameters=?,last_lat=?,last_lon=?,last_dt=?\
        WHERE filename=?"
      sql_param = ([nc_dict[key]['hashsum'],nc_dict[key]['filepath'],
        nc_dict[key]['date'],nc_dict[key]['dataset'],uploaded,None,None,
        'last updated:'+date,nc_dict[key]['platform'],nc_dict[key]['parameters'],
        nc_dict[key]['last_lat'],nc_dict[key]['last_lon'],nc_dict[key]['last_dt'],key])
    else:
      logging.info(f'Adding new entry {key}')
      sql_req = "INSERT INTO latest(filename,hashsum,filepath,nc_date,\
        dataset,uploaded,ftp_filepath,dnt_file,comment,export_date,\
        platform,parameters,last_lat,last_lon,last_dt)\
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
      sql_param = ([key,nc_dict[key]['hashsum'],nc_dict[key]['filepath'],
        nc_dict[key]['date'],nc_dict[key]['dataset'],uploaded,None,None,None,
        date,nc_dict[key]['platform'],nc_dict[key]['parameters'],
        nc_dict[key]['last_lat'],nc_dict[key]['last_lon'],nc_dict[key]['last_dt']])

    c.execute(sql_req,sql_param)
