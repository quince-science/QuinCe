import logging 
import urllib
import base64
import toml
import json
import sys
import os

def QuinCe_req(config, call, dataset_id=-1):
    ''' sends request to QuinCe'''
    quince_url = config["QuinCe"]["url"]
    user = config["QuinCe"]["user"]
    password = config["QuinCe"]["password"]

    data = urllib.parse.urlencode({'id':str(dataset_id)})
    data = data.encode('ascii')

    if dataset_id is -1:
        request = urllib.request.Request(quince_url + "/api/export/" + call)
    else:
        request = urllib.request.Request(quince_url + "/api/export/" + call, 
            data=data)

    auth_string = "%s:%s" % (user, password)
    base64_auth_string = base64.standard_b64encode(auth_string.encode("utf-8"))
    request.add_header("Authorization", "Basic %s" % base64_auth_string
        .decode("utf-8"))

    try:
        conn = urllib.request.urlopen(request)
        quince_response = conn.read()
        conn.close()
        return quince_response

    except Exception as e:
      logging.error(f'Failed to connect to QuinCe. Encountered: {e}');
      exc_type, exc_obj, exc_tb = sys.exc_info()

      fname = os.path.split(exc_tb.tb_frame.f_code.co_filename)[1]
      logging.debug(f'type: {exc_type}')
      logging.debug(f'file name: {fname}')
      logging.debug(f'line number: {exc_tb.tb_lineno}')

      sys.exit('Failed to connect to QuinCe')


def get_export_list(config):
    ''' Retrieves list of datasets ready for export from QuinCe. 

    config: .toml-file containing log-in information.
    returns: array containing name, instrument and id for each dataset 
    ready to be downloaded.
    '''
    logging.info('Retrieving exportList from QuinCe')
    export_list = QuinCe_req(config,'exportList').decode('utf8').count('id')
    logging.info(f'{export_list} dataset(s) ready for export')

    return json.loads(export_list.decode('utf-8'))

def get_export_dataset(config,dataset_id):
    ''' Retrieves .zip file from QuinCe 

    zip-file contains metadata, raw data and datasets 
    associated with dataset-id retrieved using the 
    'get_exportList' function config is login information. id is 
    QuinCe-id associated with our desired dataset.
    returns .zipfile.
    '''
    logging.info(f'Exporting dataset with id : {dataset_id}, from QuinCe')
    export_dataset = QuinCe_req(config, 'exportDataset', dataset_id)

    return export_dataset

def report_complete_export(config,dataset_id):
    ''' Reports to successful export '''
    logging.info(f'Export complete for dataset with QuinCe id: {dataset_id}')
    complete_export = QuinCe_req(config, 'completeExport', dataset_id)

def report_abandon_export(config,dataset_id):
    ''' Reports unsuccessful export '''
    logging.info(f'Abandoning export of dataset with QuinCe id: {dataset_id}')
    abandon_export = QuinCe_req(config, 'abandonExport', dataset_id)

def report_touch_export(config,dataset_id):
    ''' Reports still processing to QuinCe  '''    
    touch_export = QuinCe_req(config, 'touchExport', dataset_id)
