'''
Carbon Portal module
Contains functions related to SQL-queries

Maren K. Karlsen 2020.10.29
'''

import urllib
import http.cookiejar
import json
import sys
import os
import traceback
import logging
import hashlib
import datetime
import sqlite3
from zipfile import ZipFile
import io

from modules.Common.data_processing import get_file_from_zip

CP_DB = 'database_carbon_portal.db'

def sql_investigate(export_filename, hashsum,level,NRT,platform,err_msg):
  '''  Checks the sql database for identical filenames and hashsums
  returns 'exists', 'new', old_hashsum if 'update' and 'error' if failure. 
  '''
  db = create_connection(CP_DB)
  is_next_version = None

  status = {}
  try:

    if NRT and level == 'L1':
      db.execute("SELECT hashsum FROM cp_export WHERE export_filename LIKE ? ORDER BY export_date desc",
        [platform + '%']) # % is wildcard
    else:
      db.execute("SELECT hashsum FROM cp_export WHERE export_filename=? ORDER BY export_date desc",
        [export_filename])
    
    filename_exists = db.fetchone()
    if filename_exists: 
      if filename_exists[0] == hashsum:
        logging.info(f'{export_filename}: PREEXISTING entry')
        status = {'status':'EXISTS', 'info':'No action required'}
      else:
        logging.info(f'{export_filename}: UPDATE')
        status = ({'status':'UPDATE',
          'info':'Previously exported, updating entry',
          'old_hashsum':filename_exists[0]})
    else:
      logging.info(f'{export_filename}: NEW entry.')
      status = {'status':'NEW','info':'Adding new entry'}
  except Exception as e:
    err_msg +=(f'Checking database failed.{prev_exp["ERROR"]}')
    logging.error(f'Checking database failed:  {export_filename} ', exc_info=True)

  if status['status'] == 'UPDATE': 
    is_next_version = status['old_hashsum'] #old hashsum

  logging.debug(status['info'])

  return status, is_next_version, err_msg

def sql_commit(export_filename,hashsum,filename,level,L1_filename):
  '''  Updates existing entries or inserts new entries after export.  '''
  logging.debug(f'Adding/updating {export_filename} to SQL database')
  status = 'FAILED'

  today = datetime.datetime.now().strftime('%Y-%m-%d')
  db = create_connection(CP_DB)
  db.execute("SELECT * FROM cp_export WHERE export_filename=? ",[export_filename])

  try:
    filename_exists = db.fetchone() 
    if filename_exists:
      logging.debug(f'Update to {export_filename}')
      db.execute("UPDATE cp_export SET \
        hashsum=?,export_date=? WHERE export_filename = ?",\
        (hashsum, today, export_filename))
      logging.debug(f'{export_filename} SQL database update: Success')
      status = 'SUCCESS'
    else:
      db.execute("INSERT INTO cp_export \
        (export_filename,hashsum,filename,level,L1_filename,export_date) \
        VALUES (?,?,?,?,?,?)",
         (export_filename, hashsum, filename, level, L1_filename, today))
      logging.debug(f'{export_filename} SQL database commit: Success')
      status = 'SUCCESS'
  except Exception as e:
    raise Exception(f'Adding/Updating database failed: {export_filename}', exc_info=True)
  return status


def create_connection(CP_DB):
  ''' creates connection and database if not already created '''
  conn = sqlite3.connect(CP_DB, isolation_level=None)
  db = conn.cursor()
  db.execute(''' CREATE TABLE IF NOT EXISTS cp_export (
              export_filename TEXT PRIMARY KEY,
              hashsum TEXT NOT NULL UNIQUE,
              filename TEXT NOT NULL UNIQUE,
              level TEXT,
              L1_filename TEXT,
              export_date TEXT 
              )''')
  return db

