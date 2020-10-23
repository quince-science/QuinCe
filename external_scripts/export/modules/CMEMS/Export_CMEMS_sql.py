
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

import xml.etree.ElementTree as ET
import sqlite3
import json
import time


# Response codes
UPLOADED = 1
NOT_UPLOADED = 0
FAILED_INGESTION = -1

DNT_DATETIME_FORMAT = '%Y-%m-%dT%H:%M:%SZ'

CURRENT_DATE = datetime.datetime.now().strftime("%Y%m%d")


def update_db_new_submission(UPLOADED,filepath_ftp,filename):
  c.execute("UPDATE latest \
    SET uploaded = ?, ftp_filepath = ?, dnt_file = ? \
    WHERE filename = ?", 
    [UPLOADED, filepath_ftp, CURRENT_DATE ,filename])

def update_db_dnt(dnt_local_filepath):
  logging.debug('Updating database to include DNT filename')
  sql_rec = "UPDATE latest SET dnt_file = ? WHERE dnt_file = ?"
  sql_var = [dnt_local_filepath, CURRENT_DATE]
  c.execute(sql_rec,sql_var)

def abort_upload_db(error_msg):
  sql_req = ("UPDATE latest \
    SET uploaded = ?, ftp_filepath = ?, dnt_file = ?, comment = ? \
    WHERE dnt_file = ?")
  sql_var = [0,None,None,error_msg, CURRENT_DATE]

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

def sql_commit(nc_dict,c):
  '''  creates SQL table if non exists
  adds new netCDF files, listed in nc_dict, to new or existing SQL-table 
  '''
  date = datetime.datetime.now().strftime(DNT_DATETIME_FORMAT)

  for key in nc_dict:
    logging.debug(f'Commiting {nc_dict[key]["filepath"]} to local SQL database')
 
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
