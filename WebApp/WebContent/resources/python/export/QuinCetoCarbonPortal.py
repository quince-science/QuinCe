
# QuinCe to Carbon Portal
''' 
This script downloads data and metadata from QuinCe and uploads the content 
to the Carbon Portal (CP)
'''
import logging 
import toml
import sys
import os

from py_func.quince_comm import get_export_list, get_export_dataset, report_complete_export, report_abandon_export, report_touch_export
from py_func.meta_handling import extract_zip, get_file_from_zip, get_export_destination, get_hashsum, build_metadataL0, build_metadataL2NRT
from py_func.carbon import get_new_auth_cookie, upload_data
from py_func.copernicus import upload_to_copernicus


#logging.basicConfig(filename = 'logfile.log', stream=sys.stdout,level = logging.DEBUG, filemode = 'w')
logging.basicConfig(stream=sys.stdout, level=logging.INFO)

config_file_quince = 'config_quince.toml'
config_file_copernicus = 'config_copernicus.toml'
config_file_carbon = 'config_carbon.toml'

with open(config_file_quince) as f: config_quince = toml.load(f)
with open(config_file_copernicus) as f: config_copernicus = toml.load(f)
with open(config_file_carbon) as f: config_carbon = toml.load(f)

def main():
    try:
        logging.debug('Making connection with QuinCe:')
        logging.debug('Obtaining IDs of datasets ready for export')
        # Get list of datasets ready to be submitted from QuinCe

        export_list = get_export_list(config_quince)
        upload_status = {}

        #logging.info('Making connection with Carbon Portal')
        #auth_cookie = get_new_auth_cookie(config_carbon)
        for datasetNr, dataset in enumerate(export_list):  #for each L0

            dataset_name = dataset['name']       
            dataset_id = dataset['id']

            logging.info(
                'Processing dataset {:s} / {:s}  : {:s},QuinCe-id: {:s}'
                .format(str(datasetNr+1),str(len(export_list)),
                dataset_name,str(dataset_id)))
            
            dataset_zip = get_export_dataset(config_quince,str(dataset_id))            
            [manifest,
            datafilenames,
            raw_filenames] = extract_zip(dataset_zip,dataset_name)
            destinations = get_export_destination(datafilenames)

            #--- Make connection with Carbon Portal to report what we want to upload
            if 'ICOS OTC' in destinations:
                file = dataset_name + '/dataset/ICOS OTC/' + destinations['ICOS OTC']
                hashsum = get_hashsum(dataset_zip,file)
                logging.info('Sending hashsum and filename to CP')
                # WRITE THIS FUNCTION

                    #print('check hashsum')
                    #print('... if hashsum already exists; abort loop over dataset')  
                      # Report to quince that dataset exist, check that raw data matches(?)
                    #print('check filename')
                    #print('... if filname already exists; ')
                      # communicate that this is updated version  
                      # isNextVersionOf = PID, preExistingDoi?


            #--- Generating metadata for raw files and datafiles
            for file in raw_filenames:
                logging.info(
                    'Building L0 metadata-package for raw file: {:s}'
                    .format(file))
                metaL0 = build_metadataL0(manifest,dataset_zip,file)
                fileL0 = get_file_from_zip(dataset_zip,file)

                if 'ICOS OTC' in destinations:
                    logging.info(
                        'Sending metadata and raw dataset {:s} to Carbon Portal'
                        .format(file))
                
                    #response=upload_data(data,hashsum,auth_cookie)


            logging.info(
                'Generating metadata associated with, \'{:}\''
                .format(dataset_name ))
            metaL2 = build_metadataL2NRT(manifest)

            #--- Sending raw data, datafiles and metadata

            if 'ICOS OTC' in destinations:                       
                logging.info(
                    'Sending L2 metadata and datasets {:s} to Carbon Portal.'
                    .format(file))
                #result=upload_data(data_filenames, hashsum, auth_cookie):
                #logging.info('Results of upload: {:s}'.format(result))
                

            if 'Copernicus' in destinations:
                logging.info(
                    'Creating netcdf-files based on {:s} to send to Copernicus'
                    .format(file))
                csv_file = get_file_from_zip(dataset_zip, dataset_name
                    + '/dataset/Copernicus/' + destinations['Copernicus'])
                logging.info(
                    'Sending netcdf-files based on {:s} to send to Copernicus'
                    .format(file))
                result = upload_to_copernicus(
                    csv_file,metaL2,dataset_name,config_copernicus)
                logging.info('Copernicus upload results:')
                logging.info(result)
                upload_status.update(result)
                
        #report to QuinCe about upload process Abandon/complete
        # 
        print( ' *** Reached end of script *** ')

    #Create error handling:
    #except cookie expired
    #except authentication failed
    #except connection issues
    #except internal quince error
    #except error creating metadata, missing template, missing information
    #other exceptions? 
    except Exception as e: 
        print(e); 
        logging.info('Failed to run. Encountered: %s ',e);
        exc_type, exc_obj, exc_tb = sys.exc_info()

        fname = os.path.split(exc_tb.tb_frame.f_code.co_filename)[1]
        print(exc_type, fname, exc_tb.tb_lineno)
    finally:
        report_abandon_export(config_quince,dataset_id)

if __name__ == '__main__':
    main()

