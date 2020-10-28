import urllib
import http.cookiejar
import json
import sys
import os
import traceback
import logging
import hashlib
import sqlite3
from zipfile import ZipFile
import io


from modules.Local.data_processing import get_file_from_zip, get_hashsum, get_platform_code, is_NRT, get_L1_filename, get_export_filename
from modules.CarbonPortal.Export_CarbonPortal_metadata import  build_metadata_package
from modules.CarbonPortal.Export_CarbonPortal_SQL import sql_investigate, sql_commit
from modules.CarbonPortal.Export_CarbonPortal_http import upload_to_cp

#from py_func.meta_handling import get_hashsum, get_file_from_zip
'''Carbon Portal submission process
https://github.com/ICOS-Carbon-Portal/meta#data-object-registration-and-upload-instructions

1. post metadata package describing the data-object (JSON object)
2. put dataobject
'''
OBJ_SPEC_URI = {}
OBJ_SPEC_URI['L0'] = 'http://meta.icos-cp.eu/resources/cpmeta/otcL0DataObject'
OBJ_SPEC_URI['L1'] = 'http://meta.icos-cp.eu/resources/cpmeta/icosOtcL1Product_v2' 

def export_file_to_cp(manifest,filename,dataset_zip,index,auth_cookie,level,upload,err_msg,L0_hashsums=[]):
  ''' Upload routine for NRT data files

  Uploads both metadata and data object

  L0_hashsums is list of L0 hashsums associated with L1 object, only applicable 
  for L1 exports. Currently not in use on request by Oleg.

  '''
  success = 0
  response = ''
  CP_pid = ''

  logging.debug(f'\n\n --- Processing {level} file: {filename}')
    
  file = get_file_from_zip(dataset_zip,filename)
  
  hashsum = get_hashsum(file)  
  platform_code = get_platform_code(manifest)
  NRT = is_NRT(manifest) # True/False
  L1_filename = get_L1_filename(manifest)

  export_filename = get_export_filename(file,manifest,level)

  logging.debug(f'Checking for previous export of {export_filename}')
  [prev_exp,is_next_version,err_msg] = sql_investigate(export_filename,hashsum,level,NRT,platform_code,err_msg)

  if prev_exp['status'] == 'EXISTS':
    success = 2
  elif prev_exp['status'] == 'NEW' or prev_exp['status'] == 'UPDATE':
    meta = build_metadata_package(
      file, manifest,index, hashsum, 
      OBJ_SPEC_URI[level], level, L0_hashsums, is_next_version)
    
    if upload:
      try:
        upload_status, response = upload_to_cp(
          auth_cookie, file, hashsum, meta, OBJ_SPEC_URI[level])
        logging.debug(f'Upload status: {upload_status}')
        if upload_status:
          success = 1
          db_status = sql_commit(
            export_filename, hashsum,filename,level,L1_filename)
          logging.debug(f'{export_filename}: SQL commit {db_status}')
          CP_pid = response.decode('utf-8')
      except Exception as e:
        err_msg += (f'Failed to upload: {export_filename}, \nException: {e}')
    else: success = 2

  return success, hashsum, err_msg, CP_pid