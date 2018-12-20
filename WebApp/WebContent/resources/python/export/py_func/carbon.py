import urllib
import http.cookiejar
import json
import logging

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
   