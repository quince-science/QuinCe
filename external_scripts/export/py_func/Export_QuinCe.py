import logging 
import urllib
import base64
import toml
import json
import sys
import os
import re
import io
import hashlib
import datetime
from slacker import Slacker

from xml.etree import ElementTree as ET
from zipfile import ZipFile

with open('config.toml') as f: CONFIG = toml.load(f)


def post_slack_msg(message,status=0):
    if status == 1: workspace = 'err_workspace'
    else: workspace = 'rep_workspace' 
    
    slack = Slacker(CONFIG['slack']['api_token'])
    slack.chat.post_message('#'+CONFIG['slack'][workspace],f'{message}')


def quince_req(call, dataset_id=-1):
    ''' sends request to QuinCe'''
    quince_url = CONFIG['QuinCe']['url']
    user = CONFIG['QuinCe']['user']
    password = CONFIG['QuinCe']['password']

    data = urllib.parse.urlencode({'id':str(dataset_id)})
    data = data.encode('ascii')

    if dataset_id is -1:
        request = urllib.request.Request(quince_url + '/api/export/' + call)
    else:
        request = urllib.request.Request(quince_url + '/api/export/' + call, 
            data=data)

    auth_string = '%s:%s' % (user, password)
    base64_auth_string = base64.standard_b64encode(auth_string.encode('utf-8'))
    request.add_header('Authorization', 'Basic %s' % base64_auth_string
        .decode('utf-8'))

    try:
        conn = urllib.request.urlopen(request)
        quince_response = conn.read()
        conn.close()

    except Exception as e:
      raise Exception(f'Failed to connect to QuinCe. Encountered: {e}');
      exc_type, exc_obj, exc_tb = sys.exc_info()

      fname = os.path.split(exc_tb.tb_frame.f_code.co_filename)[1]
      logging.debug(f'type: {exc_type}')
      logging.debug(f'file name: {fname}')
      logging.debug(f'line number: {exc_tb.tb_lineno}')

      sys.exit('Failed to connect to QuinCe')

    return quince_response

def get_export_list():
    ''' Retrieves list of datasets ready for export from QuinCe. 
    returns: array containing name, instrument and id for each dataset 
    ready to be downloaded.
    '''
    logging.debug('Retrieving exportList from QuinCe')
    logging.debug('Fetching from ' + CONFIG['QuinCe']['url'])
    export_list = quince_req('exportList').decode('utf8')
    export_list_count = export_list.count('id')
    logging.info(f'{export_list_count} dataset(s) ready for export')

    return json.loads(export_list)

def get_export_dataset(dataset_id):
    ''' Retrieves .zip file from QuinCe 

    zip-file contains metadata, raw data and datasets 
    associated with dataset-id retrieved using the 
    'get_exportList' function.
    dataset_id is the internal QuinCe-id associated with our desired dataset.
    returns .zipfile.
    '''
    logging.debug(f'Exporting dataset with id : {dataset_id}, from QuinCe')
    export_dataset = quince_req('exportDataset', dataset_id)

    return export_dataset

def report_complete_export(dataset_id):
    ''' Reports to successful export '''
    logging.info(f'Export complete for dataset with QuinCe id: {dataset_id}')
    complete_export = quince_req('completeExport', dataset_id)

def report_abandon_export(dataset_id):
    ''' Reports unsuccessful export '''
    logging.info(f'Abandoning export of dataset with QuinCe id: {dataset_id}')
    abandon_export = quince_req('abandonExport', dataset_id)

def report_touch_export(dataset_id):
    ''' Reports still processing to QuinCe  '''    
    touch_export = quince_req('touchExport', dataset_id)


def process_dataset(dataset):
  '''  Retrieves and unpacks dataset from QuinCe 

  returns zip, manifest and filenames
  '''
  logging.info(f'Processing dataset {dataset["name"]}, QuinCe-id: {dataset["id"]}')
  
  dataset_zip = get_export_dataset(str(dataset['id']))      
  [manifest, data_filenames, raw_filenames] = (
    extract_zip(dataset_zip,dataset['name']))

  return dataset_zip, manifest, data_filenames, raw_filenames

def extract_zip(dataset_zip,dataset_name):
  ''' Extracts files from zip
  
  returns:  manifest, L1 datafilename and L0 datafilenames 
  '''
  logging.debug(
    f'Extracting manifest and datafiles from zip. Dataset: {dataset_name}')
   
  with ZipFile(io.BytesIO(dataset_zip),'r') as zip: 
    files_in_zip = zip.namelist()
    #logging.info(f'Files extracted: {files_in_zip!r}')
    files_print = "\t\n".join(file for file in files_in_zip)
    logging.info(f'Files extracted: {files_print}')

    manifest = zip.read(str(dataset_name)+'/manifest.json')
    manifest = json.loads(manifest.decode('utf-8'))

    raw_filenames = [s for s in files_in_zip if '/raw/' in s]
    data_filenames = [s for s in files_in_zip if '/dataset/' in s]  
  return manifest, data_filenames, raw_filenames