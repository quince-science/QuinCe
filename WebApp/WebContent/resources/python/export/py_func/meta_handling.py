import logging
import hashlib
import datetime
import json
import re
import os
from xml.etree import ElementTree as ET
from zipfile import ZipFile
import io

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
      filesInZip = zip.namelist()

      manifest = zip.read(str(dataset_name)+'/manifest.json')
      manifest = json.loads(manifest.decode('utf-8'))

      raw_filenames = [s for s in filesInZip if '/raw/' in s]
      datafilenames = [s for s in filesInZip if '/dataset/' in s]  
    return manifest, datafilenames, raw_filenames

def get_file_from_zip(zip_folder,file):
    ''' opens zip folder and returns file '''
    with ZipFile(io.BytesIO(zip_folder),'r') as zip: file = zip.extract(file, path='tmp')
    return file

def get_export_destination(datafilenames):
    ''' returns the destinations of the datafiles'''
    destinations = {}
    for destination in datafilenames:
        info = re.split('/',destination)
        destination = info[2]
        filename = info[3]
        destinations[destination] = filename
    return destinations


def get_hashsum(zip_folder,file):
    '''
    creates a 256 hashsum corresponding to input file.
    returns: hashsum 
    '''
    logging.info(
        'Generating hashsum for datafile {:s}'
        .format(file))
    file_z = get_file_from_zip(zip_folder,file)
    with open(file_z) as f: content=f.read()
    hashsum = hashlib.sha256(content.encode('utf-8')).hexdigest()
    return hashsum


def build_metadataL0(manifest,zipfolder,datafile,
    isNextVersionOf = 'n/a',preExistingDoi = 'n/a'):
    '''
    Combines metadata from manifest and from NOAA PMEL template.
    '''
    hashsumL0 = get_hashsum(zipfolder,datafile) 
    submitterID = 'OTC'
    station = manifest['manifest']['metadata']['platformCode'];
    nRows = manifest['manifest']['metadata']['records']
    acquisitionInterval = ([manifest['manifest']['metadata']['startdate'],
        manifest['manifest']['metadata']['enddate']]) 
    creator = ('http://meta.icos-cp.eu/resources/organizations/' 
        + 'CarbonPortal_CreatorSite'); 
    contributors = [];
    hostOrganization = ('http://meta.icos-cp.eu/resources/organizations/' 
        + 'CarbonPortal_OrganizationSite');
    now = datetime.datetime.now()
    creationDate = now.strftime('%Y-%m-%d');
    objectSpecification = 'objectSpecification_website';
    additional_information = (
        [manifest['manifest']['metadata']['quince_information']])

    #creating dictonary/json structure
    L0meta= {
                'submitterID':submitterID, 
                'hashsum':hashsumL0,
                'filename':datafile,
                'specificInfo':{
                    'station': station,
                    'nRows':nRows,
                    'acquisitionInterval': {
                        'start': acquisitionInterval[1],
                        'stop': acquisitionInterval[-1]
                    },
                    #'instrument': instrument,
                    #'samplingHeight': samplingHeight,
                    'production': {
                        'creator': creator,
                        'contributors': contributors,
                        'hostOrganization': hostOrganization,
                        #'comment': comment,
                        'creationDate': creationDate
                    },
                },
                'objectSpecification': objectSpecification,
                'isNextVersionOf': isNextVersionOf,
                'preExistingDoi': preExistingDoi,
                'comment': additional_information
            }

    #converting from dictionary to json-object
    L0metaJSON = json.dumps(L0meta) 
    parsed = json.loads(L0metaJSON);  
    #check to make sure json file is correct
    #print ('L0 metadata: \n'); print (json.dumps(parsed, indent = 4))  

    return L0metaJSON, hashsumL0


def build_metadataL2NRT(
	    manifest,isNextVersionOf = 'n/a',preExistingDoi = 'n/a'):
    '''
    Creates metadata for level 2 datasets. 
    Finds metadatatemplate in folder associated with instrument/expocode 
    and fills in missing values from QuinCe.

    input:
    manifest:  contains platformcode as well as dataset specific 
    information missing from the template
    isNextVersionOf: if dataset has already been uploaded and this is 
    an update.
    preExistingDoi: If the dataset has a doi prior to upload

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
            logging.debug('Using: ',template_filename)
        logging.info('metadata template: {:s}'.format(template_filename))

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
    expo_loc=root.find('expocode')
    
    #update information in xml
    start_date_loc.text = startdate
    end_date_loc.text = enddate
    north_loc.text = str(bounds['north'])
    south_loc.text = str(bounds['south'])
    east_loc.text = str(bounds['east'])
    west_loc.text = str(bounds['west'])
    expo_loc.text=expocode

    metaL2='tmp/' + expocode + '/metaL2.xml'
    tree.write(metaL2)

    return metaL2
