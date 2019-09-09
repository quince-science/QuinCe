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
OBJ_SPEC_URI['L1'] = 'http://meta.icos-cp.eu/resources/cpmeta/icosOtcL1Product' 
CP_DB = 'database_carbon_portal.db'

META_URL = 'https://meta.icos-cp.eu/upload'
OBJECT_BASE_URL = 'https://data.icos-cp.eu/objects/'
META_CONTENT_TYPE = 'application/json'
OBJECT_CONTENT_TYPE = 'text/csv'

def export_file_to_cp(
  manifest, platform, config, filename, platform_code,dataset_zip,index, 
  auth_cookie,level,L0_hashsums=[]):
  ''' Upload routie for NRT data files

  Uploads both metadata and data object
  OBJ_SPEC_URI is Carbon Portal URI for identifying data object

  L0_hashsums is list of L0 hashsums associated with L1 object, only applicable 
  for L1 exports. Might get deprecated.

  '''
  logging.info(f'Processing {level} file: {filename}')
  
  file = get_file_from_zip(dataset_zip,filename)
  hashsum = get_hashsum(file)  

  start_date = datetime.datetime.strptime(
    manifest['manifest']['metadata']['startdate'],
    '%Y-%m-%dT%H:%M:%S.%fZ').strftime('%Y%m%d')
  NRT_set = (manifest['manifest']['metadata']['platformCode']
   + '_NRT_' + start_date + '.csv')
  
  if 'L1' in level:
    export_filename = NRT_set
  else:
    export_filename = os.path.split(file)[-1]

  logging.info(f'Checking for previous export of {export_filename}')
  #sql_investigate returns old hashsum if new version(same name, diff hashsum).
  prev_exp = sql_investigate(export_filename,hashsum,filename,level,NRT_set)

  if 'EXISTS' in prev_exp: return hashsum # terminates function. 
  elif 'NEW' not in prev_exp: is_next_version = prev_exp #old hashsum
  else: is_next_version = None

  meta = build_metadata_package(
    file, manifest, platform[platform_code],index, hashsum, 
    OBJ_SPEC_URI[level], level, L0_hashsums, is_next_version, export_filename)

  try:
    upload_to_cp(auth_cookie, file, hashsum, meta, OBJ_SPEC_URI[level])
  except Exception as e:
    logging.error(f'Failed to upload: {filename}, \n Exception: {e}')
    return ''

  db_status = sql_commit(export_filename, hashsum,filename,level,NRT_set)
  logging.debug(f'{export_filename}: SQL commit {db_status}')

  return hashsum

def upload_to_cp(auth_cookie, file, hashsum, meta, OBJ_SPEC_URI):
  '''Uploads metadata and data object to Carbon Portal  '''
  logging.debug(f'cookie: cpauthToken={auth_cookie}')

  #posting metadata-json
  logging.info(f'POSTING {file} metadata-object to {META_URL}')
  resp = push_object(
    META_URL,meta.encode('utf-8'),auth_cookie,META_CONTENT_TYPE,'POST')
  logging.debug(f'{file} metadata upload response: {resp}')

  #putting data-object
  #tryingest_url = ('https://data.icos-cp.eu/tryingest?specUri=' 
  #  + urllib.parse.quote(OBJ_SPEC_URI, safe='') + '&nRows=' 
  #  + str(manifest['manifest']['metadata']['records']))
  object_url = OBJECT_BASE_URL + hashsum
  logging.info(f'PUTTING data-object: {file} to {object_url}')
  
  with open(file) as f: 
    data = f.read().encode('utf-8')
    resp = push_object(object_url,data,auth_cookie,OBJECT_CONTENT_TYPE,'PUT')
    logging.debug(f'{file} Upload response: {resp}')


def build_metadata_package(file,manifest,platform,index,hashsum,
  obj_spec,level,L0_hashsums,is_next_version,export_filename):
  '''  Builds metadata-package, step 1 of 2 Carbon Portal upload process.

  https://github.com/ICOS-Carbon-Portal/meta#registering-the-metadata-package
  returns metadata json object
  '''
  logging.debug('Constructing metadata-package')
  creation_date = datetime.datetime.utcnow().isoformat()+'Z'

  # generic metadata
  meta= {
    'submitterId': 'OTC',
    'hashSum':hashsum,
    'specificInfo':{'station': platform['cp_url'],},
    'objectSpecification': obj_spec
    }

  # L1 specific metadata
  if 'L1' in level:
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

  # L0 specific metadata
  if 'L0' in level:
    meta['specificInfo']['acquisitionInterval'] = ({
      'start':manifest['manifest']['raw'][index]['startDate'],
      'stop': manifest['manifest']['raw'][index]['endDate']})
    meta['fileName'] = os.path.split(file)[-1]


  # converting from dictionary to json-object
  meta_JSON = json.dumps(meta) 

  logging.debug(
    f'metadata-package: {type(meta_JSON)}\n \
    {json.dumps(json.loads(meta_JSON), indent = 4)}')

  return meta_JSON


def get_hashsum(filename):
  ''' returns a 256 hashsum corresponding to input file. '''
  logging.info(f'Generating hashsum for datafile {filename}')
  with open(filename) as f: content = f.read()
  hashsum = hashlib.sha256(content.encode('utf-8')).hexdigest()
  return hashsum

def push_object(url,data,auth_cookie,content_type,method):
  '''  http-posts/puts data-object to url with content-type and auth_cookie  '''

  headers = {'Content-Type':content_type,'Cookie':'cpauthToken=' + auth_cookie,}
  req = urllib.request.Request(url, data=data, headers=headers, method=method)
  try:
    response = urllib.request.urlopen(req)
    logging.debug(f'Post response: {response.read()}')
    return response.read()
  except Exception as e:
    logging.debug(e.code)
    logging.debug(e.read())
    return None

def get_auth_cookie(config):   
  '''   Returns authentication cookie from Carbon Portal.   '''
  logging.info('Obtaining authentication cookie')

  auth_url = config['CARBON']['auth_url']
  auth_mail = config['CARBON']['auth_mail']
  auth_pwd = config['CARBON']['auth_pwd']
  auth_values={'mail': auth_mail,'password': auth_pwd}

  logging.debug('Building cookie system')
  cookies = http.cookiejar.LWPCookieJar()
  handlers = [ 
    urllib.request.HTTPHandler(), 
    urllib.request.HTTPSHandler(),
    urllib.request.HTTPCookieProcessor(cookies)
    ]
  opener = urllib.request.build_opener(*handlers) 
    
  logging.debug('Constructing request')
  data = urllib.parse.urlencode(auth_values).encode('utf-8')
  req = urllib.request.Request(auth_url, data)
  response = opener.open(req)
  logging.debug(f'Cookie response: {response}')

  logging.debug('Retrieving cookie')
  for cookie in cookies:
    if cookie.name == 'cpauthToken':
      return cookie.value
    else:
      logging.debug('No cookie obtained')
  return None

def sql_investigate(export_filename, hashsum,filename,level,NRT_set):
  '''  Checks the sql database for identical filenames and hashsums
  returns 'exists', 'new', old_hashsum if 'update' and 'error' if failure. 
  '''
  c = create_connection(CP_DB)

  c.execute("SELECT hashsum FROM cp_export WHERE export_filename=? ",
    [export_filename])
  filename_exists = c.fetchone() 
  # fetches hashsum if filename exists
  try:
    if filename_exists: 
      if filename_exists[0] == hashsum:
        logging.debug(f'{filename}: already exported')
        return 'EXISTS'
      else:
        return filename_exists[0] #old_hashsum
    else:
      logging.debug(f'{filename}: new entry.')
      return 'NEW'
  except Exception as e:
    logging.error(f'Checking database failed:  {filename} ', exc_info=True)
    return 'ERROR'


def sql_commit(export_filename,hashsum,filename,level,NRT_set):
  '''  Updates existing entries or inserts new entries after export.  '''
  logging.debug(f'Adding {export_filename} to SQL database')

  today = datetime.datetime.now().strftime('%Y-%m-%d')
  c = create_connection(CP_DB)

  try:
    filename_exists = c.fetchone() 
    if filename_exists:
      if not(filename_exists[0] == hashsum):
        logging.info(f'Update to {filename}')
        c.execute("UPDATE cp_export SET \
          hashsum=?,export_date=? WHERE filename = ?",\
          (hashsum, today, filename))
        #conn.commit()
        logging.debug(f'{filename} SQL database update: Success')
        return 'SUCCESS'
    else:
      c.execute("INSERT INTO cp_export \
        (export_filename,hashsum,filename,level,NRT_set,export_date) \
        VALUES (?,?,?,?,?,?)",
         (export_filename, hashsum, filename, level, NRT_set, today))
      #conn.commit()
      logging.debug(f'{filename} SQL database commit: Success')
      return 'SUCCESS'
  except Exception as e:
    logging.error(f'Adding/Updating database failed: {filename}', exc_info=True)
    return 'ERROR'

def create_connection(CP_DB):
  ''' creates connection and database if not already created '''
  conn = sqlite3.connect(CP_DB, isolation_level=None)
  c = conn.cursor()
  c.execute(''' CREATE TABLE IF NOT EXISTS cp_export (
              export_filename TEXT PRIMARY KEY,
              hashsum TEXT NOT NULL UNIQUE,
              filename TEXT NOT NULL UNIQUE,
              level TEXT,
              NRT_set TEXT,
              export_date TEXT 
              )''')
  return c

def get_file_from_zip(zip_folder,filename):
  ''' opens zip folder and returns file '''
  with ZipFile(io.BytesIO(zip_folder),'r') as zip: 
    file = zip.extract(filename, path='tmp')
  return file