'''
Local QuinCe module
Contains functions related to data processing, manifest extraction and file operations

Maren K. Karlsen 2020.10.29
'''
import logging 
import base64
import json
import sys
import os
import re
import io
import hashlib
import datetime
import toml
from zipfile import ZipFile

from modules.Common.QuinCe import get_export_dataset

with open('platforms.toml') as f: platform = toml.load(f)


def construct_datafilename(dataset,destination,key):
  if destination == 'CP': directory = 'ICOS OTC'
  elif destination == 'CMEMS': directory = 'Copernicus'
  else: 
    raise Exception('Unknown export destination')

  data_filename = dataset['name'] + '/dataset/' + directory + key + dataset['name'] + '.csv'
  return data_filename


def get_platform(platform_code=0):
  if platform_code == 0: return platform
  return platform[platform_code]


def get_export_destination(platform_code):
  return platform[platform_code]['export']


def get_start_date(manifest):
  return datetime.datetime.strptime(
    manifest['manifest']['metadata']['startdate'],
    '%Y-%m-%dT%H:%M:%S.%fZ').strftime('%Y%m%d')


def get_platform_code(manifest):
  return manifest['manifest']['metadata']['platformCode']


def get_platform_name(platform_code):
  return platform[platform_code]['name']


def is_NRT(manifest):
  return manifest['manifest']['metadata']['nrt'] # True/False

  
def get_L1_filename(manifest):
  platform_code = get_platform_code(manifest)
  start_date = get_start_date(manifest)
  L1_filename = platform_code + '_NRT_' + start_date + '.csv'
  return L1_filename


def get_export_filename(file,manifest,level):
  L1_filename = get_L1_filename(manifest)
  if 'L1' in level:
    export_filename = L1_filename
  else: 
    export_filename = os.path.split(file)[-1]
  return export_filename


def process_dataset(dataset):
  '''  Retrieves and unpacks dataset from QuinCe 
  returns zip, manifest and list of filenames
  '''
  print(dataset['name'])
  logging.info(f'Processing dataset { dataset["name"] }, \
    QuinCe-id: { dataset["id"] }')
  
  dataset_zip = get_export_dataset(str(dataset['id']))   
  #with open('tmp/'+ dataset['name'] + '.zip','rb') as file_data: dataset_zip = file_data.read()

  [data_filenames, raw_filenames] = (extract_filelist(dataset_zip))
  manifest = extract_manifest(dataset_zip,dataset['name'])

  return dataset_zip, manifest, data_filenames, raw_filenames


def extract_filelist(dataset_zip):
  ''' Extracts filelist from zip  
  returns:  L1 datafilename and L0 datafilenames 
  '''
  logging.debug(
    f'Extracting list of datafiles from zip.')
   
  with ZipFile(io.BytesIO(dataset_zip),'r') as zip: 
    files_in_zip = zip.namelist()
    files_print = '\t\n'.join(file for file in files_in_zip)
    logging.info(f'Files extracted: {files_print}')

    raw_filenames = [s for s in files_in_zip if '/raw/' in s]
    data_filenames = [s for s in files_in_zip if '/dataset/' in s]  
  return data_filenames, raw_filenames


def extract_manifest(dataset_zip,dataset_name):
  ''' Extracts manifest from zip
  returns:  manifest 
  '''
  logging.debug(
    f'Extracting manifest from zip. Dataset: {dataset_name}')
   
  with ZipFile(io.BytesIO(dataset_zip),'r') as zip: 
    manifest = zip.read(str(dataset_name)+'/manifest.json')
    manifest = json.loads(manifest.decode('utf-8'))

  return manifest


def get_file_from_zip(zip_folder,filename):
  ''' opens zip folder and returns file '''
  with ZipFile(io.BytesIO(zip_folder),'r') as zip: 
    file = zip.extract(filename, path='tmp')
  return file


def get_hashsum(filename):
  ''' returns a 256 hashsum corresponding to input file. '''
  logging.debug(f'Generating hashsum for datafile {filename}')
  with open(filename) as f: content = f.read()

  return hashlib.sha256(content.encode('utf-8')).hexdigest()