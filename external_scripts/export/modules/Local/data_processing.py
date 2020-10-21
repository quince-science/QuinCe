import logging 
import base64
import json
import sys
import os
import re
import io

from zipfile import ZipFile

from modules.Local.API_calls import get_export_dataset


def process_dataset(dataset):
  '''  Retrieves and unpacks dataset from QuinCe 
  returns zip, manifest and list of filenames
  '''
  logging.info(f'Processing dataset {dataset["name"]}, QuinCe-id: {dataset["id"]}')
  
  dataset_zip = get_export_dataset(str(dataset['id']))      
  [data_filenames, raw_filenames] = (extract_filelist(dataset_zip))
  manifest = (extract_manifest(dataset_zip,dataset['name']))

  return dataset_zip, manifest, data_filenames, raw_filenames

def extract_filelist(dataset_zip):
  ''' Extracts filelist from zip  
  returns:  L1 datafilename and L0 datafilenames 
  '''
  logging.debug(
    f'Extracting list of datafiles from zip.')
   
  with ZipFile(io.BytesIO(dataset_zip),'r') as zip: 
    files_in_zip = zip.namelist()
    files_print = "\t\n".join(file for file in files_in_zip)
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