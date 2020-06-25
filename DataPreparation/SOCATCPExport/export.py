#!/usr/bin/env python.
# Script to export L2 files to ICOS Carbon Portal
# Exports files listed in db-export table. ready for export has uploaded value set to NULL
# Fetches autentication cookie, assembles metata string from db tables and uploads metadata and file to CP


# Maren K Karlsen 20200518

import os
import sys
import datetime
import sqlite3
import logging
import toml
import json
import urllib
import http.cookiejar


DB = 'SOCATexport.db'
OBJ_SPEC_URI = {}
OBJ_SPEC_URI['L0'] = 'http://meta.icos-cp.eu/resources/cpmeta/otcL0DataObject'
OBJ_SPEC_URI['L2'] = 'http://meta.icos-cp.eu/resources/cpmeta/icosOtcL2Product' 

META_URL = 'https://meta.icos-cp.eu/upload'
OBJECT_BASE_URL = 'https://data.icos-cp.eu/objects/'
META_CONTENT_TYPE = 'application/json'
OBJECT_CONTENT_TYPE = 'text/csv'

platform_lookup_file = 'platforms.toml'
with open(platform_lookup_file) as f: platform = toml.load(f)
config_file_carbon = 'config_carbon.toml'
with open(config_file_carbon) as f: config_carbon = toml.load(f)

logging.basicConfig(filename='export_console.log',format='%(asctime)s %(message)s', level=logging.DEBUG)

def main():
  ''' Iterates through files ready for upload, fetches citation and source-files for L2'''

  # Create connection to CP
  auth_cookie = get_auth_cookie(config_carbon)

  # Fetch all non-uploaded entries from db.
  c = create_connection()
  c.execute("SELECT * FROM export WHERE uploaded IS NULL")
  # Iterate through entries
  entries = c.fetchall()
  for entry in entries:
    [filename,expocode,hashsum,platform_code,filepath,level,export_date,uploaded,startDate,endDate,nRows] = entry
    logging.info(f'Processing {level} file: {filename}')
    citation = ''
    L0_hashsums = []
    is_next_version = None
    print(filepath)

    if export_date is not None:
      is_next_version = hashsum

    if level == 'L2':
      #fetch citation
      c.execute("SELECT citation, doi from citation where expocode = ? ",[expocode])
      citation = ', doi:'.join(c.fetchone()).strip('\n')
      if (citation.find(', doi:10.1594/PANGAEA.xxxxxx'))>0:
        citation = citation[0:citation.find(', doi:10.1594/PANGAEA.xxxxxx')]
      
      #fetch linked L0 hashsums
      c.execute("SELECT L0 from L0Links where expocode = ? ",[expocode])
      LinkedL0 = c.fetchall()
      if LinkedL0:
        LinkedL0 = LinkedL0[0][0].split(';')
        for L0_filename in LinkedL0:
          c.execute("SELECT hashsum from export where filename = ?",[L0_filename])
          L0_hashsum = c.fetchone()
          if L0_hashsum:
            L0_hashsums += L0_hashsum

    # Create metadata
    meta = build_metadata_package(filename, nRows, startDate, endDate, platform[platform_code], hashsum, citation,level,is_next_version,L0_hashsums)

    # Upload file
    try:
      uploaded = upload_to_cp(auth_cookie, filepath, hashsum, meta, OBJ_SPEC_URI[level])
      logging.info(f'Uploaded: {uploaded}')
     
      # update db
      if uploaded:
        c.execute("UPDATE export SET uploaded = ?  WHERE hashsum = ?",['True',hashsum])
      else:
        c.execute("UPDATE export SET uploaded = ?  WHERE hashsum = ?",['FailedUpload',hashsum])
    except:
        c.execute("UPDATE export SET uploaded = ?  WHERE hashsum = ?",['FailedException',hashsum])


def create_connection():
  ''' creates connection and database if not already created '''
  conn = sqlite3.connect(DB, isolation_level=None)
  c = conn.cursor()
  c.execute(''' CREATE TABLE IF NOT EXISTS export (
              filename TEXT PRIMARY KEY,
              expocode TEXT, 
              hashsum TEXT NOT NULL UNIQUE,
              platform TEXT NOT NULL,
              filepath TEXT,
              level TEXT,
              export_date TEXT,
              uploaded TEXT,
              startDate TEXT,
              endDate TEXT,
              nRows TEXT
              )''')
  return c

def build_metadata_package(filename, nRows, startDate, endDate, platform, hashsum, citation,
  level,is_next_version,L0_hashsums):
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
    'objectSpecification': OBJ_SPEC_URI[level]
    }

  if 'L2' in level:  # L1 specific metadata
    meta['fileName'] = filename.split('/')[-1]
    meta['specificInfo']['nRows'] = int(nRows)
    meta['specificInfo']['production'] = (
      {'creator': 'http://meta.icos-cp.eu/resources/organizations/OTC',
      'contributors': [],
      'creationDate': creation_date,
      'comment': citation})
    meta['specificInfo']['production']['sources'] = L0_hashsums
    if is_next_version is not None:
      meta['isNextVersionOf'] = is_next_version

  if 'L0' in level:  # L0 specific metadata
    meta['specificInfo']['acquisitionInterval'] = ({
      'start':datetime.datetime.strptime(startDate,'%Y-%m-%d %H:%M:%S').isoformat()+'Z',
      'stop': datetime.datetime.strptime(endDate,'%Y-%m-%d %H:%M:%S').isoformat()+'Z'})
    meta['fileName'] = filename

  
  meta_JSON = json.dumps(meta) # converting from dictionary to json-object
  logging.debug(f'metadata-package: {type(meta_JSON)}\n \
    {json.dumps(json.loads(meta_JSON), indent = 4)}')
  print(meta_JSON)

  return meta_JSON

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
    with open(file,'r',encoding="utf8",errors='ignore') as f: 
      data = f.read().encode('utf-8')
    resp = push_object(object_url,data,auth_cookie,OBJECT_CONTENT_TYPE,'PUT')
    logging.info(f'{file} Upload response: {resp}')
    if 'IngestionFailure' in str(resp): 
      logging.error(f'failed to upload datafile: {resp}')
      success = False

  return success

def push_object(url,data,auth_cookie,content_type,method):
  '''  http-posts/puts data-object to url with content-type and auth_cookie  '''

  headers = {'Content-Type':content_type,'Cookie':'cpauthToken=' + auth_cookie,}
  req = urllib.request.Request(url, data=data, headers=headers, method=method)
  logging.debug(headers)
  logging.debug(req)
  try:
    response = urllib.request.urlopen(req)
    logging.debug(f'Post response: {response.read()}')
  except Exception as e:
    logging.error(e.code)
    logging.error(e.read())
    raise Exception(f'{e.code} {method} failed,\n {data[0:50]}... not sent, {e.read()}')
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

def get_hashsum(filename):
  ''' returns a 256 hashsum corresponding to input file. '''
  logging.debug(f'Generating hashsum for datafile {filename}')
  with open(filename,'r',encoding="utf8",errors='ignore') as f: content = f.read()

  return hashlib.sha256(content.encode('utf-8')).hexdigest()

if __name__ == '__main__':
  main()
