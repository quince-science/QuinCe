
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

from modules.Local.API_calls import get_export_list, report_abandon_export, report_complete_export, post_slack_msg, slack_export_report
from modules.Local.data_processing import process_dataset, get_platform_code, get_platform_name, get_export_destination, construct_datafilename
from modules.CarbonPortal.Export_CarbonPortal_http import get_auth_cookie
from modules.CarbonPortal.Export_CarbonPortal_main import export_file_to_cp  
from modules.CMEMS.Export_CMEMS import build_dataproduct, upload_to_copernicus

with open('config_copernicus.toml') as f: config_copernicus = toml.load(f)
with open('platforms.toml') as f: platforms = toml.load(f)

if not os.path.isdir('log'): os.mkdir('log')
#logging.basicConfig(filename='log/console.log',format='%(asctime)s {%(filename)s:%(lineno)d} %(levelname)s -%(message)s', level=logging.DEBUG)
logging.basicConfig(stream=sys.stdout,format='%(asctime)s {%(filename)s:%(lineno)d} %(levelname)s - %(message)s', level=logging.DEBUG)

upload = False # for debugging purposes, when False no data is exported.
ERROR = 1 # for differentiating between slack reports and slack errors, slack msg defaults to report

def main():
  logging.info('\n \n***** Starting QuinCe NRT export ***** \n Obtaining IDs of datasets ready for export from QuinCe')
  try:
    export_list = get_export_list() 
    if not export_list:
      logging.info('Terminating script, no datasets available for export.')
    else: 
      cp_cookie = get_auth_cookie()
      for dataset in export_list: 
        [dataset_zip,
        manifest, 
        data_filenames, 
        raw_filenames] = process_dataset(dataset)

        platform_code = get_platform_code(manifest)
        platform_name = get_platform_name(platform_code)
        export_destination = get_export_destination(platform_code)

        key = '/'
        if '26NA' in platform_code: key = ' No Salinity Flags' + key

        successful_upload_CP = -1
        successful_upload_CMEMS = -1

        for destination in export_destination:
          if 'ICOS' in destination: 
            successful_upload_CP = 0; cp_err_msg = '';

            #--- Processing L0 files
            L0_hashsums = []
            for index, raw_filename in enumerate(raw_filenames):
              successful_upload_CP, L0_hashsum, cp_err_msg = export_file_to_cp(manifest, raw_filename, dataset_zip, index, cp_cookie,'L0',upload,cp_err_msg)
              if L0_hashsum:
                L0_hashsums += [L0_hashsum]
            
            #--- Processing L1 files            
            index = -1 # used in export of L0, not needed for L1
            try:
              data_filename = construct_datafilename(dataset,'CP',key)
              successful_upload_CP, L1_hashsum, cp_err_msg = export_file_to_cp(
                manifest, data_filename, dataset_zip, index, cp_cookie, 'L1', upload, cp_err_msg,L0_hashsums)
            except Exception as e:
              logging.error('Carbon Portal export failed. \n', exc_info=True)
              successful_upload_CP = 0


          if 'CMEMS' in destination: 
            successful_upload_CMEMS = 0; cmems_err_msg = '';
            build_dataproduct(dataset_zip,dataset,key)
            try: 
              if upload:
                  successful_upload_CMEMS, cmems_err_msg = upload_to_copernicus('nrt_server',dataset,platforms)
            except Exception as e:
              logging.error('Exception occurred: ', exc_info=True)
              successful_upload_CMEMS = 0
        
        successful_upload = False
        if (successful_upload_CP & successful_upload_CMEMS): successful_upload = True

        slack_export_report('Carbon Portal',platform_name,dataset,successful_upload_CP,cp_err_msg)
        slack_export_report('CMEMS',platform_name,dataset,successful_upload_CMEMS,cmems_err_msg)

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

