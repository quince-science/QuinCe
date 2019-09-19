
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

from py_func.quince_comm import get_export_list, report_abandon_export, report_complete_export
from py_func.meta_handling import process_dataset 
from py_func.carbon import get_auth_cookie, export_file_to_cp 
from py_func.copernicus import build_dataproduct, upload_to_copernicus


config_file_quince = 'config_quince.toml'
config_file_copernicus = 'config_copernicus.toml'
config_file_carbon = 'config_carbon.toml'
config_file_meta = 'config_meta.toml'
platform_lookup_file = 'platforms.toml'

with open(config_file_quince) as f: config_quince = toml.load(f)
with open(config_file_copernicus) as f: config_copernicus = toml.load(f)
with open(config_file_carbon) as f: config_carbon = toml.load(f)
with open(config_file_meta) as f: config_meta = toml.load(f)
with open(platform_lookup_file) as f: platform = toml.load(f)

if not os.path.isdir('log'):
  os.mkdir('log')
#logging.basicConfig(stream=sys.stdout,format='%(asctime)s %(message)s', level=logging.DEBUG)
logging.basicConfig(filename='log/console.log',format='%(asctime)s %(message)s', level=logging.DEBUG)

upload = True # for debugging purposes, when False no data is exported.

def main():
  logging.debug('Obtaining IDs of datasets ready for export from QuinCe')
  export_list = get_export_list(config_quince)

  try:
    if not export_list:
      logging.info('Terminating script, no datasets to be exported.')
    else: 
      for dataset in export_list: 
        [dataset_zip,
        manifest, 
        data_filenames, 
        raw_filenames] = process_dataset(dataset,config_quince)
        logging.debug(manifest)

        platform_code = manifest['manifest']['metadata']['platformCode']
        export_destination = platform[platform_code]['export'] 

        if 'ICOS' in export_destination: 
          cp_cookie = get_auth_cookie(config_carbon)
        
          L0_hashsums = []
          for index, raw_filename in enumerate(raw_filenames):
            L0_hashsum = export_file_to_cp(
              manifest, platform, config_carbon, raw_filename, platform_code, 
              dataset_zip, index, cp_cookie,'L0',upload)
            if L0_hashsum:
              L0_hashsums += [L0_hashsum]
            
        #--- Processing L1 files
        for index, data_filename in enumerate(data_filenames):

          ## EXPORTING L1 TO CARBON PORTAL ##
          if 'ICOS' in data_filename and 'ICOS' in export_destination: 
            try:
              L1_hashsum = export_file_to_cp(
                manifest, platform, config_carbon, data_filename, platform_code, 
                dataset_zip, index, cp_cookie, 'L1', upload, L0_hashsums)
            except Exception as e:
              logging.INFO('Carbon Portal export failed')
              logging.error('Exception occurred: ', exc_info=True)

          if 'Copernicus' in data_filename and 'CMEMS' in export_destination:  
            logging.info('Executing Copernicus routine')
            curr_date  = build_dataproduct(dataset_zip,dataset['name'],data_filename)
            try: 
              if upload:
                  successful_upload_CMEMS = upload_to_copernicus(
                      config_copernicus,'nrt_server',dataset,curr_date)
              else: 
                  successful_upload_CMEMS = False
            except Exception as e:
              logging.error('Exception occurred: ', exc_info=True)
              logging.INFO('FTP connection failed')
        if successful_upload_CMEMS:
          report_complete_export(config_quince,dataset['id'])
        else: 
          report_abandon_export(config_quince,dataset['id'])

  except Exception as e: 
    print(e); 
    logging.info('Failed to run. Encountered: %s ',e);
    exc_type, exc_obj, exc_tb = sys.exc_info()
    traceback.print_exc()

    fname = os.path.split(exc_tb.tb_frame.f_code.co_filename)[1]
    logging.debug(f'type: {exc_type}')
    logging.debug(f'file name: {fname}')
    logging.debug(f'line number: {exc_tb.tb_lineno}')
    
    report_abandon_export(config_quince,dataset['id'])


if __name__ == '__main__':
  main()

