
# QuinCe to Carbon Portal
''' 
This script downloads data and metadata from QuinCe and uploads the content 
to the Carbon Portal (CP)
'''
import logging 
import toml
import sys
import os
import traceback

from py_func.quince_comm import get_export_list, report_abandon_export
from py_func.meta_handling import process_dataset, process_L0_data, process_L2_data
from py_func.carbon import cp_init, send_L0_to_cp, send_L2_to_cp
from py_func.copernicus import send_to_copernicus


#logging.basicConfig(filename = 'logfile.log', 
#stream=sys.stdout,level = logging.DEBUG, filemode = 'w')
logging.basicConfig(stream=sys.stdout, level=logging.DEBUG)

config_file_quince = 'config_quince.toml'
config_file_copernicus = 'config_copernicus.toml'
config_file_carbon = 'config_carbon.toml'
config_file_meta = 'config_meta.toml'

with open(config_file_quince) as f: config_quince = toml.load(f)
with open(config_file_copernicus) as f: config_copernicus = toml.load(f)
with open(config_file_carbon) as f: config_carbon = toml.load(f)
with open(config_file_meta) as f: config_meta = toml.load(f)

def main():
  try:
    logging.debug('Making connection with QuinCe:')
    logging.debug('Obtaining IDs of datasets ready for export')
    # Get list of datasets ready to be submitted from QuinCe

    export_list = get_export_list(config_quince)
    upload_status = {}
    
    for datasetNr, dataset in enumerate(export_list): 
      upload_status[datasetNr]={} 

      [dataset_zip,
      manifest, 
      data_filenames, 
      raw_filenames,
      destinations] = process_dataset(dataset,config_quince)

      #--Make connection with Carbon Portal 
      if 'ICOS OTC' in destinations:
        cp_response_init, auth_cookie = cp_init(
          dataset['name'],
          destinations['ICOS OTC'],
          dataset_zip,
          config_carbon)
        
      #--- Processing L0 files
      for index, filename in enumerate(raw_filenames):
        meta_L0, file_L0, hashsum_L0 = process_L0_data(
          filename,
          manifest,
          dataset_zip,
          config_meta,
          index)

        if 'ICOS OTC' in destinations:
          upload_status[datasetNr]['cp_response_L0'] = send_L0_to_cp(
            meta_L0,
            file_L0,
            hashsum_L0,
            auth_cookie)
          
      #--- Processing L2 files
      for index, data_filename in enumerate(data_filenames):
        if 'ICOS OTC' in data_filename:   
          meta_L2, file_L2, hashsum_L2 = process_L2_data(
            data_filename,
            manifest,
            dataset_zip)
          
          upload_status[datasetNr]['cp_response_L2'] = send_L2_to_cp(
            meta_L2,
            file_L2,
            hashsum_L2,
            auth_cookie)

        if 'Copernicus' in data_filename:  
          upload_status[datasetNr]['result_copernicus_upload_L2'] = (
            send_to_copernicus(
            data_filename, 
            dataset_zip,
            dataset['name'],
            destinations['Copernicus'],
            config_copernicus,
            'nrt_server')) #,delete_file=True)

    #report to QuinCe about upload process Abandon/complete
    #report_abandon_export(config_quince,dataset['id'])

  #Create error handling:
  #except cookie expired
  #except authentication failed
  #except connection issues
  #except internal quince error
  #except error creating metadata, missing template, missing information

  except Exception as e: 
    print(e); 
    logging.info('Failed to run. Encountered: %s ',e);
    exc_type, exc_obj, exc_tb = sys.exc_info()

    fname = os.path.split(exc_tb.tb_frame.f_code.co_filename)[1]
    print(exc_type, fname, exc_tb.tb_lineno)
    traceback.print_exc()
  finally:
    print(upload_status)
    report_abandon_export(config_quince,dataset['id'])

if __name__ == '__main__':
  main()

