import urllib
import http.cookiejar
import json
import sys
import os
import traceback
import logging
import hashlib
import datetime
from py_func.meta_handling import get_hashsum, get_file_from_zip

'''
Carbon Portal submission process
1. post metadata package describing the data-object (JSON object)
   - post potential supporting document
2. post dataobject
'''

 #auth_cookie = None #resets authentication cookie everytime the carbon.py is imported to the main script, e.g. start of main-script?


def carbon_portal_routine(file,manifest,config):  # NOT IN USE

  #retireving authentication cookit
  auth_cookie = get_auth_cookie(config)



  # FOR EACH L0 DATASET
  # CREATE METADATA, UPLOAD METADATA, UPLOAD DATA
  # CREATE LIST OF UPLOADED DATASETS

  # FOR EACH L1 DATASET
  # LINK TO L0 DATASETS
  # CREATE METADATA, UPLOAD METADATA, UPLOAD DATA


  #creating metadata-package
  metadata = build_metadata_package(file,manifest,station)

  #posting metadata-package
  url = 'https://meta.icos-cp.eu/upload'
  logging.debug(f'Uploading metadata-package')
  
  resp = post_object(url,metadata,'application/json',auth_cookie)
  
  logging.debug(f'Upload response: {resp}')

  '''
  <data object id> is either base64url- or hex-encoded representation of the first 18 bytes of a SHA-256 hashsum of the data object's contents. 
  The complete 32-byte representations are also accepted. 
  You will have obtained the data object id by the time you have completed the first step of the 2-step upload procedure. 
  '''
  #posting data-object
  data = 'file'.encode('utf-8') #Binary contents of file. #open file, read bytes.
  obj_spec_uri = '' #Ask Oleg, same as meta: objectSpecification?
  nRows = ''
  url = 'https://data.icos-cp.eu/tryingest?specUri=<' + obj_spec_uri + '>&nRows=<' + nRows + '>'
  content_type = ''
  logging.debug(f'Uploading data-object')
  
  resp = post_object(url,data,content_type,auth_cookie)
  
  logging.debug(f'Upload response: {resp}')


def upload_L0(manifest, platform, config, filenames, platform_code,dataset_zip, auth_cookie):

  for index, filename in enumerate(filenames):
    logging.debug('L0 file:', filename)

    file = get_file_from_zip(dataset_zip,filename)
    hashsum = get_hashsum(file)  

    #create metadata package
    L0_meta = build_metadata_package(file, manifest, platform[platform_code], index, hashsum)
    logging.debug(f'L0 meta type: {type(L0_meta)}')
    logging.debug(f'L0_meta {L0_meta}')

    #upload metadata package
    #posting metadata-object
    url = 'https://meta.icos-cp.eu/upload'
    content_type = 'application/json'
    logging.debug(f'Uploading data-object')

    resp = post_object(url,L0_meta,auth_cookie,content_type)
  
    logging.debug(f'Upload response: {resp}')


    #upload data-file
    #posting data-object
    data = file.encode('utf-8') #Binary contents of file. #open file, read bytes.
    #obj_spec_uri = '' #Ask Oleg, same as meta: objectSpecification?
    data_object_id = hashsum
    nRows = manifest['manifest']['metadata']['records']
    url = 'https://data.icos-cp.eu/tryingest?specUri=<' + obj_spec_uri + '>&nRows=<' + nRows + '>'
    #url = 'https://data.icos-cp.eu/objects/<' + data_object_id + '>'
    content_type = 'text/csv'
    logging.debug(f'Uploading data-object')
  
    resp = post_object(url,data,auth_cookie,content_type)
  
    logging.debug(f'Upload response: {resp}')


def upload_L1(manifest, platform, config, filename, platform_code,dataset_zip,index, cookie):

  file = get_file_from_zip(dataset_zip,filename)


  #create metadata package
  L1_meta = build_metadata_package(file, manifest, platform[platform_code],index)


  #upload metadata package

  #upload data-file



def build_metadata_package(file,manifest,platform,index,hashsum):
  '''
  Builds metadata-package, step 1 of 2 Carbon Portal upload process.
  Description: https://github.com/ICOS-Carbon-Portal/meta#registering-the-metadata-package

  "submitterId": "OTC",
  "hashSum":  from file,
  "fileName": from file "L0test.csv",
  "specificInfo": {
  "station": from lookup table "https://meta.icos-cp.eu/ontologies/cpmeta/OS",
  "acquisitionInterval": {
    "start": from file "2008-09-01T00:00:00.000Z",
    "stop": from file "2008-12-31T23:59:59.999Z"
  },
  "production": {
    "creator": "http://meta.icos-cp.eu/resources/people/Lynn_Hazan",
    "contributors": [],
      "hostOrganization": "http://meta.icos-cp.eu/resources/organizations/ATC",
      "comment": "free text",
      "creationDate": "2017-12-01T12:00:00.000Z"
    }
    "nRows": from file (number of data_rows; total_rows-header_rows),
  },
  "objectSpecification": "http://meta.icos-cp.eu/resources/cpmeta/atcCo2NrtDataObject",
  "isNextVersionOf": Optional "MAp1ftC4mItuNXH3xmAe7jZk",
  "preExistingDoi": Optional "10.1594/PANGAEA.865618"

  '''

  logging.debug('Constructing metadata-package')

  creation_date = datetime.datetime.utcnow().isoformat()+'Z'
  #now = datetime.datetime.now()
  #creation_date = now.strftime('%Y-%m-%d')

  #creating dictonary/json structure
  meta= {
  'submitterID': 'http://meta.icos-cp.eu/resources/people/Benjamin_Pfeil', # 'provided by CP', #ask Oleg, # Should probably be a general OTC ID
  'hashsum':hashsum,
  'filename':manifest['manifest']['raw'][index]['filename'],
  'specificInfo':{
  'station': platform['cp_url'], #cp url: manifest['manifest']['metadata']['platformCode'],
  'nRows':manifest['manifest']['metadata']['records'],
  'acquisitionInterval': {
  'start':manifest['manifest']['raw'][index]['startDate'],
  'stop': manifest['manifest']['raw'][index]['endDate']
  },
  'production': {
  'creator': 'http://meta.icos-cp.eu/resources/organizations/OTC',
  'contributors': '[]',
  'creationDate': creation_date
  },
  },
  'objectSpecification': 'URI', #ask Oleg
  'comment': [manifest['manifest']['metadata']['quince_information']]
  }

  #converting from dictionary to json-object
  meta_JSON = json.dumps(meta) 
  parsed = json.loads(meta_JSON);  
  logging.debug(f'metadata-package: {json.dumps(parsed, indent = 4)}')
  logging.debug(f'meta_JSON type: {type(meta_JSON)}')

  return meta_JSON



def get_hashsum(filename):
  '''
  creates a 256 hashsum corresponding to input file.
  returns: hashsum 
  '''
  logging.info(
    'Generating hashsum for datafile {:s}'
    .format(filename))
  with open(filename) as f: content=f.read()
  hashsum = hashlib.sha256(content.encode('utf-8')).hexdigest()
  return hashsum

def post_object(url,data,auth_cookie,content_type):
  '''
  http-posts data-object to url with header denoting content-type and authentication cookie

  '''
  # data = json.dumps(metadata_filename) #transforming dictionary-object to json-object
  # content_type = 'application/json'

  headers = { 'Content-Type': content_type , 'Cookie': auth_cookie,  }
  req = urllib.request.Request(url, data=data.encode('utf-8'), headers=headers, method='POST')

  handlers = [ 
    urllib.request.HTTPHandler(), 
    urllib.request.HTTPSHandler()
    ]
  opener = urllib.request.build_opener(*handlers)

  response = opener.open(req)

  #response = opener.error(req)

  logging.debug('Post response:',str(response.read()))
  return response.read()



def get_auth_cookie(config):   
  '''
  Retrives authentication cookie from Carbon Portal. 
  authentication_Values:  Login credentials
  returns:  authentication cookie string 
  '''
  logging.info('Obtaining authentication cookie')

  auth_url = config['CARBON']['auth_url']
  auth_mail = config['CARBON']['auth_mail']
  auth_pwd = config['CARBON']['auth_pwd']
  auth_values={'mail': auth_mail,'password': auth_pwd}

  # Building cookie system
  cookies = http.cookiejar.LWPCookieJar()
  handlers = [ 
    urllib.request.HTTPHandler(), 
    urllib.request.HTTPSHandler(),
    urllib.request.HTTPCookieProcessor(cookies)
    ]
  opener = urllib.request.build_opener(*handlers) 
    
  #Constructing request
  data = urllib.parse.urlencode(auth_values).encode('utf-8')
  req = urllib.request.Request(auth_url, data)
  response = opener.open(req)
  logging.debug(f'Cookie response: {response}')

  #Retrieving cookie
  for cookie in cookies:
    if cookie.name == 'cpauthToken':
      return cookie.value
    else:
      logging.debug('No cookie obtained')
  return None

  # def cp_init(dataset_name,destination,dataset_zip,config_carbon):
  #   ''' 
  #   Initiates contact with Carbon Portal. Sends login information, hashsum 
  #   and retrieves authentication cookie from Carbon Portal.
  #   '''
  #   auth_cookie = get_new_auth_cookie(config_carbon)
  #   filename = dataset_name + '/dataset/ICOS OTC/' + destination
  #   hashsum = get_hashsum(dataset_zip,filename)
  #   logging.info('Sending hashsum and filename to CP')

  

  #     #check hashsum

  #     #... if hashsum already exists; abort loop over dataset  
  #       # Report to quince that dataset exist, check that raw data matches(?)
    
  #     #check filename
  #     #... if filname already exists; communicate that this is updated 
  #     # version, upload new version with new hashsum
  #       # isNextVersionOf = PID

  #   response = 'temp'
  #   return response, auth_cookie
 
  # def upload_data(filename, hashsum, auth_cookie):   
  #   '''
  #   uploads metadata to Carbon Portal
  #   error 401 :  authentication cookie has expired
  #   filePath: path to file to be uploaded
  #   auth_cookie: authentication cookie to use for communication with Carbon portal
  #   return: http response
  #   '''
  #   #Constructing and running request
  #   data = json.dumps(metadata_filename) #transforming dictionary-object to json-object
  #   headers = { 'Content-Type': 'application/json' , 'Cookie': auth_cookie }
  #   req = urllib.request.Request(metadata_url, data=data, headers=headers)
  #   response = opener.open(req)
  #   return response.read()

  # def send_L0_to_cp(meta_L0,file_L0,hashsum,auth_cookie):
  #   logging.info(
  #     'Sending metadata and L0 dataset {:s} to Carbon Portal'
  #     .format(file_L0))
  #   response = 'temp'      
  #   #response = upload_data(data,hashsum,auth_cookie)

  #   return response

  # def send_L2_to_cp(meta_L2,file_L2,hashsum,auth_cookie):
  #   logging.info(
  #     'Sending metadata and L2 dataset {:s} to Carbon Portal'
  #     .format(file_L2))
  #   response = 'temp'      
  #   #response = upload_data(data,hashsum,auth_cookie)

  #   return response