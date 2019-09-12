import logging
import hashlib
import datetime
import json
import re
import os
from xml.etree import ElementTree as ET
from zipfile import ZipFile
import io
from py_func.quince_comm import get_export_dataset


def process_dataset(dataset, config_quince):
  '''  Retrieves and unpacks dataset from QuinCe 

  returns Zip, manifest and filenames
  '''
  logging.info(f'Processing dataset {dataset["name"]}, QuinCe-id: {dataset["id"]}')
  
  dataset_zip = get_export_dataset(config_quince,str(dataset['id']))      
  [manifest, data_filenames, raw_filenames] = (
    extract_zip(dataset_zip,dataset['name']))

  return dataset_zip, manifest, data_filenames, raw_filenames

def extract_zip(dataset_zip,dataset_name):
  ''' Extracts files from zip
  
  returns:  manifest, L1 datafilename and L0 datafilenames 
  '''
  logging.info(
    f'Extracting manifest and datafiles from zip. Dataset: {dataset_name}')
   
  with ZipFile(io.BytesIO(dataset_zip),'r') as zip: 
    files_in_zip = zip.namelist()
    logging.info(f'files extracted: {files_in_zip!r}')

    manifest = zip.read(str(dataset_name)+'/manifest.json')
    manifest = json.loads(manifest.decode('utf-8'))

    raw_filenames = [s for s in files_in_zip if '/raw/' in s]
    data_filenames = [s for s in files_in_zip if '/dataset/' in s]  
  return manifest, data_filenames, raw_filenames

