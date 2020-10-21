
''' 

This script fetches datasets available for export from QuinCe and exports them to the locations listed in the accompanying manifest.


Maren K. Karlsen 
'''
import logging 
import toml
import sys
import os
import traceback
from slacker import Slacker

from modules.Local.API_calls import get_export_list, report_abandon_export, report_complete_export, post_slack_msg
from modules.Local.data_processing import process_dataset
from modules.CarbonPortal.Export_CarbonPortal import get_auth_cookie, export_file_to_cp 
from modules.CMEMS.Export_CMEMS import build_dataproduct, upload_to_copernicus

with open('config_copernicus.toml') as f: config_copernicus = toml.load(f)
with open('config_carbon.toml') as f: config_carbon = toml.load(f)
with open('platforms.toml') as f: platform = toml.load(f)

if not os.path.isdir('log'): os.mkdir('log')
#logging.basicConfig(filename='log/console.log',format='%(asctime)s {%(filename)s:%(lineno)d} %(levelname)s -%(message)s', level=logging.DEBUG)
logging.basicConfig(stream=sys.stdout,format='%(asctime)s {%(filename)s:%(lineno)d} %(levelname)s -%(message)s', level=logging.DEBUG)

upload = False # for debugging purposes, when False no data is exported.
ERROR = 1 # for differentiating between slack reports and slack errors, slack msg defaults to report

def main():
  logging.info('\n \n***** Starting QuinCe NRT export ***** \n Obtaining IDs of datasets ready for export from QuinCe')
  try:
    export_list = get_export_list() 
    if not export_list:
      logging.info('Terminating script, no datasets available for export.')
    else: 
      cp_cookie = get_auth_cookie(config_carbon)
      for dataset in export_list: 
        [dataset_zip,
        manifest, 
        data_filenames, 
        raw_filenames] = process_dataset(dataset)

        platform_code = manifest['manifest']['metadata']['platformCode']              
        export_destination = platform[platform_code]['export'] 
        key = '/'
        if '26NA' in platform_code: key = ' No Salinity Flags' + key
        successful_upload_CP = -1
        successful_upload_CMEMS = -1
        for destination in export_destination:
          if 'ICOS' in destination: 
            #--- Processing L0 files
            successful_upload_CP = 0; cp_err_msg = '';
            L0_hashsums = []
            for index, raw_filename in enumerate(raw_filenames):
              successful_upload_CP, L0_hashsum, cp_err_msg = export_file_to_cp(
                manifest, platform, config_carbon, raw_filename, platform_code, 
                dataset_zip, index, cp_cookie,'L0',upload,cp_err_msg)
              if L0_hashsum:
                L0_hashsums += [L0_hashsum]
            
            #--- Processing L1 files            
            data_filename = (dataset["name"] + '/dataset/' + "ICOS OTC" + key 
              + dataset["name"] + '.csv')
            index = -1 # used in export of L0, not needed for L1
            logging.debug(data_filename in data_filenames)
            
            try:
              successful_upload_CP, L1_hashsum, cp_err_msg = export_file_to_cp(
                manifest, platform, config_carbon, data_filename, platform_code, 
                dataset_zip, index, cp_cookie, 'L1', upload, cp_err_msg,L0_hashsums)
            except Exception as e:
              logging.error('Carbon Portal export failed. \n', exc_info=True)

          if 'CMEMS' in destination: 
            data_filename = (dataset["name"] + '/dataset/' + "Copernicus" + key 
              + dataset["name"] + '.csv')

            successful_upload_CMEMS = 0; cmems_err_msg = '';
            curr_date  = build_dataproduct(dataset_zip,dataset['name'],data_filename,platform)
            try: 
              if upload:
                  successful_upload_CMEMS, cmems_err_msg = upload_to_copernicus(
                      config_copernicus,'nrt_server',dataset,curr_date,platform)
            except Exception as e:
              logging.error('Exception occurred: ', exc_info=True)
              successful_upload_CMEMS = 0
        
        successful_upload = True

        CP_slack_msg = platform[platform_code]['name'] + ' : ' + dataset['name'] + ' - Carbon Portal - '

        if successful_upload_CP == 0: 
          CP_slack_msg += 'Export failed. ' + str(cp_err_msg)
          successful_upload = False
        elif successful_upload_CP == 1: CP_slack_msg += 'Successfully exported.'
        elif successful_upload_CP == 2: CP_slack_msg += 'No new data.'
        
        CMEMS_slack_msg = platform[platform_code]['name'] + ' : ' + dataset['name'] + ' - CMEMS - ' 
        if successful_upload_CMEMS == 0: 
          CMEMS_slack_msg += 'Export failed. ' + cmems_err_msg
          successful_upload = False
        elif successful_upload_CMEMS == 1: CMEMS_slack_msg += 'Successfully exported.'
        elif successful_upload_CMEMS == 2: CMEMS_slack_msg += 'No new data.'

        if CP_slack_msg: post_slack_msg(CP_slack_msg)
        if CMEMS_slack_msg: post_slack_msg(CMEMS_slack_msg)

        if successful_upload:
          report_complete_export(dataset['id'])
        else: 
          report_abandon_export(dataset['id'])

  except Exception as e: 
    exc_type, exc_obj, exc_tb = sys.exc_info()
    fname = os.path.split(exc_tb.tb_frame.f_code.co_filename)[1]
    except_msg = f'Failed to run. Encountered: {e} \n type: {exc_type} \n file name: {fname} \n line number: {exc_tb.tb_lineno}'

    post_slack_msg(except_msg,ERROR)
    logging.error(except_msg)
    try:
      if export_list:
        report_abandon_export(dataset['id'])
    except Exception as e:
      post_slack_msg(f'Failed to abandon QuinCe export {e}',ERROR)

if __name__ == '__main__':
  main()

