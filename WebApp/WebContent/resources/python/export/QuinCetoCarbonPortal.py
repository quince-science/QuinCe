
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
from py_func.meta_handling import process_dataset #, process_L0_data, process_L1_data
from py_func.carbon import get_auth_cookie, upload_L0, upload_L1
from py_func.copernicus import build_netCDF, upload_to_copernicus



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

def main():
  try:
    logging.debug('Obtaining IDs of datasets ready for export from QuinCe')
    # Get list of datasets ready to be submitted from QuinCe

    export_list = get_export_list(config_quince)

    if not export_list:
      logging.info('Terminating script, no datasets to be exported.')
      sys.exit()


    for dataset in export_list: 
      [dataset_zip,
      manifest, 
      data_filenames, 
      raw_filenames] = process_dataset(dataset,config_quince)

      platform_code = manifest['manifest']['metadata']['platformCode']
      export = platform[platform_code]['export'] 
    

      #if 'ICOS' in export: 
      #  cp_cookie = get_auth_cookie(config_carbon)
      #  upload_L0(manifest, platform, config_carbon, raw_filenames, platform_code, dataset_zip, cp_cookie)
          
      #--- Processing L1 files
      for index, data_filename in enumerate(data_filenames):

        # if 'ICOS' in data_filename and 'ICOS' in export: 
        #   #upload file to Carbon Portal
        #   upload_L1(manifest, platform, config_carbon, data_filename, platform_code, dataset_zip,index, cp_cookie)

        if 'Copernicus' in data_filename and 'CMEMS' in export:  
          logging.info('Executing Copernicus routine')
          build_netCDF(dataset_zip,dataset['name'],data_filename)

      # UPLOAD TO COPERNICUS
        try: 
         successful_upload_CMEMS = upload_to_copernicus(config_copernicus,'nrt_server',dataset)
        except Exception as e:
         logging.error('Exception occurred: ', exc_info=True)
         logging.INFO('FTP connection failed')

      if successful_upload_CMEMS:
        report_complete_export(config_quince,dataset['id'])
      else:
        report_abandon_export(config_quince,dataset['id'])

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

    report_abandon_export(config_quince,dataset['id'])


if __name__ == '__main__':
  main()

