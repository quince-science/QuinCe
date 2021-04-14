'''
Carbon Portal main module 
Carbon Portal triage

Maren K. Karlsen 2020.10.29

Carbon Portal submission process
https://github.com/ICOS-Carbon-Portal/meta#data-object-registration-and-upload-instructions

1. post metadata package describing the data-object (JSON object)
2. put dataobject

'''

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
import traceback

from modules.Common.data_processing import get_file_from_zip, get_hashsum, get_platform_code, is_NRT, get_L1_filename, get_export_filename
from modules.CarbonPortal.Export_CarbonPortal_metadata import  build_metadata_package
from modules.CarbonPortal.Export_CarbonPortal_SQL import sql_investigate, sql_commit
from modules.CarbonPortal.Export_CarbonPortal_http import upload_to_cp


OBJ_SPEC_URI = {}
OBJ_SPEC_URI['L0'] = 'http://meta.icos-cp.eu/resources/cpmeta/otcL0DataObject'
OBJ_SPEC_URI['L1'] = 'http://meta.icos-cp.eu/resources/cpmeta/icosOtcL1Product_v2' 
OBJ_SPEC_URI['L2'] = 'http://meta.icos-cp.eu/resources/cpmeta/icosOtcL2Product' 

def export_file_to_cp(manifest,filename,dataset_zip,index,auth_cookie,upload,err_msg,L0,L0_hashsums=[]):
  ''' Upload routine for NRT data files
  
  Uploads both metadata and data object

  L0_hashsums is list of L0 hashsums associated with L1 object, only applicable 
  for L1 exports. Currently not in use on request by Oleg.

  '''
  success = 0
  response = ''
  CP_pid = ''
    
  file = get_file_from_zip(dataset_zip,filename)
  
  hashsum = get_hashsum(file)  
  platform_code = get_platform_code(manifest)
  NRT = is_NRT(manifest) # True/False
  L1_filename = get_L1_filename(manifest)
  if L0: level = 'L0'
  else:  
    if NRT: level = 'L1'
    else: level = 'L2'

  logging.debug(f'\n\n --- Processing {level} file: {filename}')

  export_filename = get_export_filename(file,manifest,level)

  logging.debug(f'Checking for previous export of {export_filename}')
  [prev_exp,is_next_version,err_msg] = sql_investigate(
    export_filename,hashsum,level,NRT,platform_code,err_msg)

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
          if (response.decode('utf-8')):
              CP_pid = "https://hdl.handle.net/" + response.decode('utf-8')
          else:
              CP_pid = ''
      except Exception as e:
        tb = traceback.format_exc()
        err_msg += (f'Failed to upload: {export_filename}, \nException: {tb}')
    else: success = 2

  return success, hashsum, err_msg, CP_pid
