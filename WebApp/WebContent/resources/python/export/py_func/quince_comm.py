import logging 
import urllib
import base64
import toml
import json

def make_quince_call(config, call, dataset_id=-1):
    quince_url = config["QuinCe"]["url"]
    user = config["QuinCe"]["user"]
    password = config["QuinCe"]["password"]

    data = urllib.parse.urlencode({'id':str(dataset_id)})
    data = data.encode('ascii')

    if dataset_id is -1:
        request = urllib.request.Request(quince_url + 
        "/api/export/" + call)
    else:
        request = urllib.request.Request(quince_url + 
        "/api/export/" + call, data=data)

    auth_string = "%s:%s" % (user, password)
    base64_auth_string = base64.standard_b64encode(auth_string
        .encode("utf-8"))
    request.add_header("Authorization", "Basic %s" % base64_auth_string
        .decode("utf-8"))

    conn = urllib.request.urlopen(request)
    quince_response = conn.read()
    conn.close()

    return quince_response

def get_export_list(config):
    '''
    retrieves list of datasets ready for export from QuinCe. config file 
    is .toml-file containing log-in information.

    returns: array containing name, instrument and id for each dataset 
    ready to be downloaded.
    '''
    logging.info('Retrieving exportList  from QuinCe')

    export_list = make_quince_call(config,'exportList')

    return json.loads(export_list.decode('utf-8'))

def get_export_dataset(config,dataset_id):
    '''
    Retrieves .zip fil from QuinCe containing metadata, raw data and 
    datasets associated with dataset-id retrieved using the 
    'get_exportList' function config is login information. id is 
    QuinCe-id associated with our desired dataset.
    returns .zipfile.
    '''
    logging.info(
        'Exporting dataset with id : {:s}, from QuinCe'
        .format(dataset_id))
    
    export_dataset = make_quince_call(config, 'exportDataset', dataset_id)
    
    return export_dataset

def report_complete_export(config,dataset_id):
    '''
    Reports to QuinCe that export was successful for dataset with 
    id: dataset_id
    '''
    logging.info(
        'Export complete for dataset with QuinCe id: {:s}'
        .format(dataset_id))
   
    complete_export = make_quince_call(config, 'completeExport', dataset_id)
    conn.close()

def report_abandon_export(config,dataset_id):
    '''
    Reports to QuinCe that export was unsuccessful for dataset with 
    id: dataset_id, and that it should be returned to the list of sets 
    ready for export.
    '''
    logging.info(
        'Abandoning export of dataset with QuinCe id: {:d}'
        .format(dataset_id))
     
    abandon_export = make_quince_call(config, 'abandonExport', dataset_id)


def report_touch_export(config,dataset_id):
    '''
    Reports to QuinCe that work is still being to dataset_id and to not 
    returns the dataset to list of sets ready for export. 
    '''    
    touch_export = make_quince_call(config, 'touchExport', dataset_id)
