import logging
import hashlib
import datetime
import json
import re
import os
from xml.etree import ElementTree as ET
from zipfile import ZipFile
import io
from py_func.quince_comm import (get_export_list, get_export_dataset, 
  report_complete_export, report_abandon_export, report_touch_export)


def process_dataset(dataset, config_quince):
  '''
  Retrieves dataset from QuinCe, 
  unpacks and determines destinations of dataset. 
  '''
  logging.info(
    'Processing dataset  {}, QuinCe-id: {}'
    .format(dataset['name'],dataset['id']))
  
  dataset_zip = get_export_dataset(config_quince,str(dataset['id']))      
  [manifest,
  data_filenames,
  raw_filenames] = extract_zip(dataset_zip,dataset['name'])
  destination_filename = get_export_destination_filename(data_filenames)

  return dataset_zip, manifest, data_filenames, raw_filenames, destination_filename

def extract_zip(dataset_zip,dataset_name):
  '''
  Extracts manifest, raw-datafilenames and L2 datafilename from the 
  zip-folder downloaded from QuinCe.

  input: zip-file from QuinCe and name of dataset
  returns: 
  manifest, L2 datafilename (icos-format) and raw datafilenames 
  '''
  logging.info(
    'Extracting manifest and datafiles from zip. Dataset: {:s}'
    .format(dataset_name))
   
  with ZipFile(io.BytesIO(dataset_zip),'r') as zip: 
    zip.printdir()
    files_in_zip = zip.namelist()

    manifest = zip.read(str(dataset_name)+'/manifest.json')
    manifest = json.loads(manifest.decode('utf-8'))

    raw_filenames = [s for s in files_in_zip if '/raw/' in s]
    data_filenames = [s for s in files_in_zip if '/dataset/' in s]  
  return manifest, data_filenames, raw_filenames

def get_file_from_zip(zip_folder,filename):
  ''' opens zip folder and returns file '''
  with ZipFile(io.BytesIO(zip_folder),'r') as zip: 
    file = zip.extract(filename, path='tmp')
  return file

def get_export_destination_filename(data_filenames):
  ''' returns the filename to be sent to the corresponding destinations of the datafiles'''
  destination_filename = {}
  for destination in data_filenames:
    info = re.split('/',destination)
    destination = info[2]
    data_filename = info[3]
    destination_filename[destination] = data_filename
  return destination_filename


def get_hashsum(zip_folder,filename):
  '''
  creates a 256 hashsum corresponding to input file.
  returns: hashsum 
  '''
  logging.info(
    'Generating hashsum for datafile {:s}'
    .format(filename))
  file_z = get_file_from_zip(zip_folder,filename)
  with open(file_z) as f: content=f.read()
  hashsum = hashlib.sha256(content.encode('utf-8')).hexdigest()
  return hashsum


def build_metadata_L0(manifest,filename,hashsum,config_meta,index,
  is_next_version_of = 'n/a'):
  '''
  Combines metadata from manifest and from NOAA PMEL template.
  '''
  now = datetime.datetime.now()
  creation_date = now.strftime('%Y-%m-%d')
  #creating dictonary/json structure
   
  L0_meta= {
  'submitterID':config_meta['L0']['submitterID'], 
  'hashsum':hashsum,
  'filename':manifest['manifest']['raw'][index]['filename'],
  'specificInfo':{
    'station': manifest['manifest']['metadata']['platformCode'],
    'nRows':manifest['manifest']['metadata']['records'],
    'acquisitionInterval': {
      'start':manifest['manifest']['raw'][index]['startDate'],
      'stop': manifest['manifest']['raw'][index]['endDate']
    },
    #'instrument': instrument,
    #'samplingHeight': samplingHeight,
    'production': {
      'creator': config_meta['L0']['creator'],
      'contributors': config_meta['L0']['contributors'],
      'hostOrganization': config_meta['L0']['hostOrganization'],
      'creationDate': creation_date
    },
  },
  'objectSpecification': config_meta['L0']['objectSpecification'],
  'isNextVersionOf': is_next_version_of,
  'preExistingDoi': 'n/a',
  'comment': [manifest['manifest']['metadata']['quince_information']]
      }

  #converting from dictionary to json-object
  L0_meta_JSON = json.dumps(L0_meta) 
  parsed = json.loads(L0_meta_JSON);  
  #check to make sure json file is correct
  #print ('L0 metadata: \n'); print (json.dumps(parsed, indent = 4))  

  return L0_meta_JSON


def build_metadata_L2NRT(
    manifest,is_next_version_of = 'n/a',pre_existing_doi = 'n/a'):
  '''
  Creates metadata for level 2 datasets. 
  Finds metadatatemplate in folder associated with instrument/expocode 
  and fills in missing values from QuinCe.

  input:
  manifest:  contains platformcode as well as dataset specific 
  information missing from the template
  is_next_version_of: if dataset has already been uploaded and this is 
  an update.
  pre_existing_doi: If the dataset has a doi prior to upload

  returns:
  L2 metadata
  '''

  #get information from manifest
  expocode = manifest['manifest']['metadata']['name']
  platformcode = manifest['manifest']['metadata']['platformCode']
  startdate = manifest['manifest']['metadata']['startdate']
  enddate = manifest['manifest']['metadata']['enddate']
  bounds = manifest['manifest']['metadata']['bounds']

  #get template from folder
  list_of_template_filenames = os.listdir('oap_metadata_templates')
  candidate_filenames = \
  [s for s in list_of_template_filenames if platformcode in s]

  if len(candidate_filenames) == 0:
    logging.warning('oap metadata template is missing')    
  else:
    template_filename = candidate_filenames[0]
    if len(candidate_filenames) > 1:
      logging.warning('Multiple oap metadata templates exist')
      logging.warning('Using: ',template_filename)
    logging.debug('metadata template: {:s}'.format(template_filename))

  #load template
  tree = ET.parse('oap_metadata_templates/' + template_filename)
  root = tree.getroot()
  
  #find locations in xml where information should be updated
  start_date_loc = root.find('startdate')
  end_date_loc = root.find('enddate')
  north_loc = root.find('northbd')
  south_loc = root.find('southbd')
  east_loc = root.find('eastbd')
  west_loc = root.find('westbd')
  expo_loc = root.find('expocode')
  
  #update information in xml
  start_date_loc.text = startdate
  end_date_loc.text = enddate
  north_loc.text = str(bounds['north'])
  south_loc.text = str(bounds['south'])
  east_loc.text = str(bounds['east'])
  west_loc.text = str(bounds['west'])
  expo_loc.text = expocode

  meta_L2='tmp/' + expocode + '/meta_L2.xml'
  tree.write(meta_L2)

  return meta_L2

def process_L0_data(filename,manifest,dataset_zip,config_meta,index):
  ''' 
  Builds metadata and retrieves file for L0 data
  '''
  logging.info(
    'Building L0 metadata-package for L0 file: {:s}'
    .format(filename))
  hashsum = get_hashsum(dataset_zip,filename)
  meta_L0 = build_metadata_L0(manifest,filename,hashsum,config_meta,index)
  file_L0 = get_file_from_zip(dataset_zip,filename)

  
  meta_L0_parsed = json.loads(meta_L0);
  logging.debug(
    'Generated metadata and hashsum for datafile: {filename},\nhashsum: {hashsum}\n metadata:\n{meta_L0}'
    .format(filename=filename,hashsum=hashsum,meta_L0=json.dumps(meta_L0_parsed, indent = 4)))

  return meta_L0, file_L0, hashsum

def process_L2_data(file,manifest,dataset_zip):
  ''' 
  Builds metadata and retrieves file for L0 data
  '''
  logging.info(
    'Generating metadata associated with, \'{:}\''
    .format(file))
  meta_L2 = build_metadata_L2NRT(manifest,dataset_zip,file)
  file_L2 = get_file_from_zip(dataset_zip,file)
  hashsum = get_hashsum(dataset_zip,file)

  return meta_L2, file_L2, hashsum
