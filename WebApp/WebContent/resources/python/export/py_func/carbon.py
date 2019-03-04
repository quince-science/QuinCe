import urllib
import http.cookiejar
import json
import logging
from py_func.meta_handling import get_hashsum


def cp_init(dataset_name,destination,dataset_zip,config_carbon):
    ''' 
    Initiates contact with Carbon Portal. Sends login information, hashsum and retrieves authentication cookie from Carbon Portal.
    '''
    auth_cookie = get_new_auth_cookie(config_carbon)
    filename = dataset_name + '/dataset/ICOS OTC/' + destination
    hashsum = get_hashsum(dataset_zip,filename)
    logging.info('Sending hashsum and filename to CP')

        #print('check hashsum')
        #print('... if hashsum already exists; abort loop over dataset')  
          # Report to quince that dataset exist, check that raw data matches(?)
        #print('check filename')
        #print('... if filname already exists; ')
          # communicate that this is updated version  
          # isNextVersionOf = PID, preExistingDoi?

    response = 'temp'
    return response, auth_cookie

def get_new_auth_cookie(config):   
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
    request = urllib.request.Request(auth_url, data)
    response = opener.open(request)

    #Retrieving cookie
    for cookie in cookies:
      if cookie.name == 'cpauthToken':
        return cookie.value
    return None

 
def upload_data(filenames, hashsum, auth_cookie):   
    '''
    uploads metadata to Carbon Portal
    error 401 :  authentication cookie has expired
    filePath: path to file to be uploaded
    auth_cookie: authentication cookie to use for communication with Carbon portal
    return: http response
    '''
    #Constructing and running request
    results={}
    for filename in filenames:
        data = json.dumps(metadata_filename) #transforming dictionary-object to json-object
        headers = { 'Content-Type': 'application/json' , 'Cookie': auth_cookie }
        req = urllib.request.Request(metadata_url, data=data, headers=headers)# ,method='POST') # https://docs.python.org/2/library/urllib2.html#urllib2.Request
        response = opener.open(req)
        results[filename]=response.read()
    return results

def send_L0_to_cp(meta_L0,file_L0,hashsum,auth_cookie):
    logging.info(
        'Sending metadata and L0 dataset {:s} to Carbon Portal'
        .format(file_L0))
    response = 'temp'            
    #response = upload_data(data,hashsum,auth_cookie)

    return response

def send_L2_to_cp(meta_L2,file_L2,hashsum,auth_cookie):
    logging.info(
        'Sending metadata and L2 dataset {:s} to Carbon Portal'
        .format(file_L2))
    response = 'temp'            
    #response = upload_data(data,hashsum,auth_cookie)

    return response