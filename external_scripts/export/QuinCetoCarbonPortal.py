
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
from slacker import Slacker

from py_func.quince_comm import get_export_list, report_abandon_export, report_complete_export
from py_func.meta_handling import process_dataset 
from py_func.carbon import get_auth_cookie, export_file_to_cp 
from py_func.copernicus import build_dataproduct, upload_to_copernicus


config_file = 'config.toml'
config_file_copernicus = 'config_copernicus.toml'
config_file_carbon = 'config_carbon.toml'
platform_lookup_file = 'platforms.toml'

with open(config_file) as f: basicConfig = toml.load(f)
with open(config_file_copernicus) as f: config_copernicus = toml.load(f)
with open(config_file_carbon) as f: config_carbon = toml.load(f)
with open(platform_lookup_file) as f: platform = toml.load(f)

if not os.path.isdir('log'): os.mkdir('log')
#logging.basicConfig(filename='log/console.log',format='%(asctime)s %(message)s', level=logging.INFO)
logging.basicConfig(filename='log/console.log',format='%(asctime)s %(message)s', level=logging.DEBUG)

slack = Slacker(basicConfig['slack']['api_token'])
upload = True # for debugging purposes, when False no data is exported.

def main():
  logging.info('***** Starting QuinCe NRT export *****')    
  logging.debug('Obtaining IDs of datasets ready for export from QuinCe')
  try:
    export_list = get_export_list(basicConfig)
    if not export_list:
      logging.info('Terminating script, no datasets to be exported.')
    else: 
      cp_cookie = get_auth_cookie(config_carbon)
      for dataset in export_list: 
        [dataset_zip,
        manifest, 
        data_filenames, 
        raw_filenames] = process_dataset(dataset,basicConfig)

        platform_code = manifest['manifest']['metadata']['platformCode']              
        export_destination = platform[platform_code]['export'] 

        #--- Processing L0 files
        if 'ICOS' in export_destination: 
          successful_upload_CP = 0; cp_err_msg = '';
          L0_hashsums = []
          for index, raw_filename in enumerate(raw_filenames):
            successful_upload_CP, L0_hashsum, cp_err_msg = export_file_to_cp(
              manifest, platform, config_carbon, raw_filename, platform_code, 
              dataset_zip, index, cp_cookie,'L0',upload,cp_err_msg)
            if L0_hashsum:
              L0_hashsums += [L0_hashsum]
            
        #--- Processing L1 files
        for index, data_filename in enumerate(data_filenames):
          key = '/'
          if '26NA' in platform_code: key = ' No Salinity Flags' + key
          
          if 'ICOS OTC' + key in data_filename and 'ICOS' in export_destination:
            try:
              successful_upload_CP, L1_hashsum, cp_err_msg = export_file_to_cp(
                manifest, platform, config_carbon, data_filename, platform_code, 
                dataset_zip, index, cp_cookie, 'L1', upload, cp_err_msg,L0_hashsums)
            except Exception as e:
              logging.error('Carbon Portal export failed. \n', exc_info=True)
          if 'Copernicus' + key in data_filename and 'CMEMS' in export_destination: 
            successful_upload_CMEMS = 0; cmems_err_msg = '';
            curr_date  = build_dataproduct(dataset_zip,dataset['name'],data_filename,platform)
            try: 
              if upload:
                  successful_upload_CMEMS, cmems_err_msg = upload_to_copernicus(
                      config_copernicus,'nrt_server',dataset,curr_date,platform)
              else: 
                  successful_upload_CMEMS = 0
            except Exception as e:
              logging.error('Exception occurred: ', exc_info=True)
              successful_upload_CMEMS = 0
          else:
            successful_upload_CMEMS = 0
        
        if successful_upload_CP == 0: CP_slack_msg = 'Carbon Portal: Export failed, ' + str(cp_err_msg)
        elif successful_upload_CP == 1: CP_slack_msg = 'Carbon Portal: Successful export'
        elif successful_upload_CP == 2: CP_slack_msg = 'Carbon Portal: No new data'
        
        if successful_upload_CMEMS == 0: CMEMS_slack_msg = 'CMEMS: Export failed, ' + cmems_err_msg
        elif successful_upload_CMEMS == 1: CMEMS_slack_msg = 'CMEMS: Successful export'
        elif successful_upload_CMEMS == 2: CMEMS_slack_msg = 'CMEMS: No new data'

        slack.chat.post_message('#'+basicConfig['slack']['rep_workspace'],f'{CP_slack_msg}')
        slack.chat.post_message('#'+basicConfig['slack']['rep_workspace'],f'{CMEMS_slack_msg}')
        successful_upload = successful_upload_CP != 0 and successful_upload_CMEMS != 0
        if successful_upload:
          report_complete_export(basicConfig,dataset['id'])
        else: 
          report_abandon_export(basicConfig,dataset['id'])

  except Exception as e: 
    exc_type, exc_obj, exc_tb = sys.exc_info()
    fname = os.path.split(exc_tb.tb_frame.f_code.co_filename)[1]
    except_msg = f'Failed to run. Encountered: {e} \n type: {exc_type} \n file name: {fname} \n line number: {exc_tb.tb_lineno}'

    slack.chat.post_message('#'+basicConfig['slack']['err_workspace'],except_msg)
    logging.error(except_msg)
    try:
      if export_list:
        report_abandon_export(basicConfig,dataset['id'])
    except Exception as e:
      slack.chat.post_message('#'+basicConfig['slack']['err_workspace'],f'Failed to abandon QuinCe export {e}')

if __name__ == '__main__':
  main()

