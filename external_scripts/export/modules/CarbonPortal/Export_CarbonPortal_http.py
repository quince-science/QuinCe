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
from zipfile import ZipFile
import io
import toml


from modules.Local.data_processing import get_file_from_zip

#from py_func.meta_handling import get_hashsum, get_file_from_zip
'''Carbon Portal submission process
https://github.com/ICOS-Carbon-Portal/meta#data-object-registration-and-upload-instructions

1. post metadata package describing the data-object (JSON object)
2. put dataobject

OBJ_SPEC_URI is Carbon Portal URI for identifying data object

'''
OBJ_SPEC_URI = {}
OBJ_SPEC_URI['L0'] = 'http://meta.icos-cp.eu/resources/cpmeta/otcL0DataObject'
OBJ_SPEC_URI['L1'] = 'http://meta.icos-cp.eu/resources/cpmeta/icosOtcL1Product_v2' 


META_URL = 'https://meta.icos-cp.eu/upload'
OBJECT_BASE_URL = 'https://data.icos-cp.eu/objects/'
META_CONTENT_TYPE = 'application/json'
OBJECT_CONTENT_TYPE = 'text/csv'


with open('config_carbon.toml') as f: config = toml.load(f)

def upload_to_cp(auth_cookie, file, hashsum, meta, OBJ_SPEC_URI):
  '''Uploads metadata and data object to Carbon Portal  '''
  success = True

  logging.debug(f'\nPOSTING {file} metadata-object to {META_URL}')
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

def get_auth_cookie():   
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
