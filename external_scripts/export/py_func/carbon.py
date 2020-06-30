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
import datetime
from zipfile import ZipFile
import io

#from py_func.meta_handling import get_hashsum, get_file_from_zip
'''Carbon Portal submission process
https://github.com/ICOS-Carbon-Portal/meta#data-object-registration-and-upload-instructions

1. post metadata package describing the data-object (JSON object)
2. put dataobject
'''
OBJ_SPEC_URI = {}
OBJ_SPEC_URI['L0'] = 'http://meta.icos-cp.eu/resources/cpmeta/otcL0DataObject'
OBJ_SPEC_URI['L1'] = 'http://meta.icos-cp.eu/resources/cpmeta/icosOtcL1Product_v2' 
CP_DB = 'database_carbon_portal.db'

META_URL = 'https://meta.icos-cp.eu/upload'
OBJECT_BASE_URL = 'https://data.icos-cp.eu/objects/'
META_CONTENT_TYPE = 'application/json'
OBJECT_CONTENT_TYPE = 'text/csv'

def export_file_to_cp(
  manifest, platform, config, filename, platform_code,dataset_zip,index, 
  auth_cookie,level,upload,err_msg,L0_hashsums=[]):
  ''' Upload routine for NRT data files

  Uploads both metadata and data object
  OBJ_SPEC_URI is Carbon Portal URI for identifying data object

  L0_hashsums is list of L0 hashsums associated with L1 object, only applicable 
  for L1 exports. Currently not in use on request by Oleg.

  '''
  CP_success = 0;
  logging.debug(f'Processing {level} file: {filename}')
    
  file = get_file_from_zip(dataset_zip,filename)
  hashsum = get_hashsum(file)  

  start_date = datetime.datetime.strptime(
    manifest['manifest']['metadata']['startdate'],
    '%Y-%m-%dT%H:%M:%S.%fZ').strftime('%Y%m%d')
  platform_code = manifest['manifest']['metadata']['platformCode']
  NRT = manifest['manifest']['metadata']['nrt'] # True/False
  L1_filename = platform_code + '_NRT_' + start_date + '.csv'
  # Setting export name
  if 'L1' in level:
    export_filename = L1_filename
  else: 
    export_filename = os.path.split(file)[-1]

  logging.debug(f'Checking for previous export of {export_filename}')
  prev_exp = sql_investigate(export_filename,hashsum,level,NRT,platform_code)
  logging.debug(prev_exp['info'])
  is_next_version = None

  if prev_exp['status'] == 'ERROR': 
    err_msg +=(f'Checking database failed.{prev_exp["ERROR"]}')
  if prev_exp['status'] == 'UPDATE': 
    is_next_version = prev_exp['old_hashsum'] #old hashsum

  if prev_exp['status'] == 'EXISTS':
    CP_success = 2
  elif prev_exp['status'] == 'NEW' or prev_exp['status'] == 'UPDATE':
    meta = build_metadata_package(
      file, manifest, platform[platform_code],index, hashsum, 
      OBJ_SPEC_URI[level], level, L0_hashsums, is_next_version, export_filename)
    
    if upload:
      try:
        upload_status = upload_to_cp(
          auth_cookie, file, hashsum, meta, OBJ_SPEC_URI[level])
        logging.debug(f'Upload status: {upload_status}')
        if upload_status:
          CP_success = 1
          db_status = sql_commit(
            export_filename, hashsum,filename,level,L1_filename)
          logging.debug(f'{export_filename}: SQL commit {db_status}')
      except Exception as e:
        err_msg += (f'Failed to upload: {export_filename}, \nException: {e}')
    else: CP_success = 1

  return CP_success, hashsum, err_msg

def upload_to_cp(auth_cookie, file, hashsum, meta, OBJ_SPEC_URI):
  '''Uploads metadata and data object to Carbon Portal  '''
  success = True

  logging.debug(f'POSTING {file} metadata-object to {META_URL}')
  resp = push_object(
    META_URL,meta.encode('utf-8'),auth_cookie,META_CONTENT_TYPE,'POST')
  logging.info(f'{file} metadata upload response: {resp}')
  if 'IngestionFailure' in str(resp): 
    success = False
    logging.error(f'failed to upload metadata: {resp}')
  else:
    object_url = OBJECT_BASE_URL + hashsum
    logging.debug(f'PUTTING data-object: {file} to {object_url}')
    with open(file) as f: 
      data = f.read().encode('utf-8')
    resp = push_object(object_url,data,auth_cookie,OBJECT_CONTENT_TYPE,'PUT')
    logging.info(f'{file} Upload response: {resp}')
    if 'IngestionFailure' in str(resp): 
      logging.error(f'failed to upload datafile: {resp}')
      success = False

  return success


def build_metadata_package(file,manifest,platform,index,hashsum,
  obj_spec,level,L0_hashsums,is_next_version,export_filename):
  '''  Builds metadata-package, step 1 of 2 Carbon Portal upload process.
  https://github.com/ICOS-Carbon-Portal/meta#registering-the-metadata-package
  returns metadata json object
  '''
  logging.debug('Constructing metadata-package')
  creation_date = datetime.datetime.utcnow().isoformat()+'Z'

  meta= {
    'submitterId': platform['submitter_id'],
    'hashSum':hashsum,
    'specificInfo':{'station': platform['cp_url'],},
    'objectSpecification': obj_spec
    }

  if 'L1' in level:  # L1 specific metadata
    meta['fileName'] = export_filename
    meta['specificInfo']['nRows'] = manifest['manifest']['metadata']['records']
    meta['specificInfo']['production'] = (
      {'creator': 'http://meta.icos-cp.eu/resources/organizations/OTC',
      'contributors': [],
      'creationDate': creation_date,
      'comment': ''.join(
        [manifest['manifest']['metadata']['quince_information']])})
    # meta['specificInfo']['production']['sources'] = L0_hashsums
    if is_next_version is not None:
      meta['isNextVersionOf'] = is_next_version

  if 'L0' in level:  # L0 specific metadata
    meta['specificInfo']['acquisitionInterval'] = ({
      'start':manifest['manifest']['raw'][index]['startDate'],
      'stop': manifest['manifest']['raw'][index]['endDate']})
    meta['fileName'] = os.path.split(file)[-1]
  
  meta_JSON = json.dumps(meta) # converting from dictionary to json-object
  logging.debug(f'metadata-package: {type(meta_JSON)}\n \
    {json.dumps(json.loads(meta_JSON), indent = 4)}')

  return meta_JSON


def get_hashsum(filename):
  ''' returns a 256 hashsum corresponding to input file. '''
  logging.debug(f'Generating hashsum for datafile {filename}')
  with open(filename) as f: content = f.read()

  return hashlib.sha256(content.encode('utf-8')).hexdigest()


def push_object(url,data,auth_cookie,content_type,method):
  '''  http-posts/puts data-object to url with content-type and auth_cookie  '''

  headers = {'Content-Type':content_type,'Cookie':'cpauthToken=' + auth_cookie,}
  req = urllib.request.Request(url, data=data, headers=headers, method=method)
  try:
    response = urllib.request.urlopen(req)
    logging.debug(f'Post response: {response.read()}')
  except Exception as e:
    logging.error(e.code)
    logging.error(e.read())
    raise Exception(f'{e.code} {method} failed,\n {data} not sent, {e.read()}')
  return response.read()

def get_auth_cookie(config):   
  '''   Returns authentication cookie from Carbon Portal.   '''
  logging.debug('Obtaining authentication cookie')
  auth_cookie = None

  auth_url = config['CARBON']['auth_url']
  auth_mail = config['CARBON']['auth_mail']
  auth_pwd = config['CARBON']['auth_pwd']
  auth_values={'mail': auth_mail,'password': auth_pwd}

  cookies = http.cookiejar.LWPCookieJar()
  handlers = [ 
    urllib.request.HTTPHandler(), 
    urllib.request.HTTPSHandler(),
    urllib.request.HTTPCookieProcessor(cookies)
    ]
  opener = urllib.request.build_opener(*handlers) 
    
  data = urllib.parse.urlencode(auth_values).encode('utf-8')
  req = urllib.request.Request(auth_url, data)
  response = opener.open(req)

  for cookie in cookies:
    if cookie.name == 'cpauthToken':
      logging.debug(f'Cookie: {cookie.value}')
      auth_cookie = cookie.value
    else:
      logging.debug('No cookie obtained')

  return auth_cookie

def sql_investigate(export_filename, hashsum,level,NRT,platform):
  '''  Checks the sql database for identical filenames and hashsums
  returns 'exists', 'new', old_hashsum if 'update' and 'error' if failure. 
  '''
  c = create_connection(CP_DB)
  status = {}
  try:

    if NRT and level == 'L1':
      c.execute("SELECT hashsum FROM cp_export WHERE export_filename LIKE ? ORDER BY export_date desc",
        [platform + '%']) # % is wildcard
    else:
      c.execute("SELECT hashsum FROM cp_export WHERE export_filename=? ORDER BY export_date desc",
        [export_filename])
    
    filename_exists = c.fetchone()
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
    logging.error(f'Checking database failed:  {export_filename} ', exc_info=True)
    status = {'status':'ERROR','info':e}

  return status

def sql_commit(export_filename,hashsum,filename,level,L1_filename):
  '''  Updates existing entries or inserts new entries after export.  '''
  logging.debug(f'Adding/updating {export_filename} to SQL database')
  status = 'FAILED'

  today = datetime.datetime.now().strftime('%Y-%m-%d')
  c = create_connection(CP_DB)
  c.execute("SELECT * FROM cp_export WHERE export_filename=? ",[export_filename])

  try:
    filename_exists = c.fetchone() 
    if filename_exists:
      logging.debug(f'Update to {export_filename}')
      c.execute("UPDATE cp_export SET \
        hashsum=?,export_date=? WHERE export_filename = ?",\
        (hashsum, today, export_filename))
      logging.debug(f'{export_filename} SQL database update: Success')
      status = 'SUCCESS'
    else:
      c.execute("INSERT INTO cp_export \
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
  c = conn.cursor()
  c.execute(''' CREATE TABLE IF NOT EXISTS cp_export (
              export_filename TEXT PRIMARY KEY,
              hashsum TEXT NOT NULL UNIQUE,
              filename TEXT NOT NULL UNIQUE,
              level TEXT,
              L1_filename TEXT,
              export_date TEXT 
              )''')
  return c

def get_file_from_zip(zip_folder,filename):
  ''' opens zip folder and returns file '''
  with ZipFile(io.BytesIO(zip_folder),'r') as zip: 
    file = zip.extract(filename, path='tmp')
  return file