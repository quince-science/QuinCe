'''
Automated export main script

- Retrieves NRT, L2 and raw data from QuinCe.
- Extracts relevant metadata from manifest and toml file platforms.toml
- Export each datafile to destinations listed in manifest.
- Carbon Portal:
    - Exports raw data files and NRT/L2 data file
    -  Creates metadata json object for each file
    -  Exports the metadata and data using http-requests.
    -  Carbon Portal PID is returned in the event of a successful ingestion.
         This PID is used to create CMEMS citation string.
- CMEMS:
    - Exports NRT data as day-length netCDF files.
    -  Creates netCDFs from retrieved csv-file
    -  Uploads netCDF file to 'production' FTP-server
    -  Uploads index file, detailing current content of FTP-server
    -  Uploads ready for ingestion 'delivery notice', DNT.
    -  Retrieves and investigates return DNT to evaluatate successful ingestion
- Export success/failure, and exceptions, are reported to Slack channel 'reports' and 'errors'

Maren K. Karlsen 2020.10.29
'''

import logging
import toml
import sys
import os

from modules.Common.QuinCe import get_export_list, report_abandon_export, report_complete_export
from modules.Common.Slack import post_slack_msg, slack_export_report
from modules.Common.data_processing import process_dataset, get_platform_name, get_export_destination, is_NRT
from modules.CarbonPortal.Export_CarbonPortal_main import cp_upload

with open('config_copernicus.toml') as f:
    config_copernicus = toml.load(f)
with open('platforms.toml') as f: platforms = toml.load(f)

if not os.path.isdir('log'): os.mkdir('log')
logging.basicConfig(filename='log/console.log',
                    format='%(asctime)s {%(filename)s:%(lineno)d} %(levelname)s - %(message)s', level=logging.DEBUG)
# logging.basicConfig(stream=sys.stdout,format='%(asctime)s {%(filename)s:%(lineno)d} %(levelname)s - %(message)s',
# level=logging.DEBUG) #Logs to console, for debugging only.

# UPLOAD flag removed - now set in each destination's config file
SLACK_ERROR_MSG = True  # for differentiating between slack reports and slack errors, slack msg defaults to report


def main():
    logging.info(
        '\n \n***** Starting QuinCe NRT export ***** \n Obtaining IDs of datasets ready for export from QuinCe')
    export_list = None
    dataset = None
    try:
        export_list = get_export_list()
        if not export_list:
            logging.info('Terminating script, no datasets available for export.')
        else:
            for dataset in export_list:
                # dataset = {'name':'26NA20190327','id':0}  # For testing purposes. Overrides QuinCe;
                # Sets dataset variable manually, replacing QuinCe response. For every dataset in export_list.
                [dataset_zip,
                 manifest,
                 data_filenames,
                 raw_filenames] = process_dataset(dataset)

                platform_name = get_platform_name(manifest, True)
                export_destination = get_export_destination(platform_name, is_NRT(manifest))

                key = '/'
                if platform_name == 'NO-SOOP-Nuka__Arctica':
                    key = ' No Salinity Flags' + key

                successful_upload_CP = 0
                successful_upload_CMEMS = 0

                for destination in export_destination:
                    if 'ICOS' in destination:
                        upload_result = icos_upload(raw_filenames, manifest, dataset_zip, dataset)
                        if upload_result is not None:
                            successful_upload_CP = 1

                    if 'CMEMS' in destination:
                        successful_upload_CMEMS = 0
                        cmems_err_msg = ''

                        # --- Creating netCDFs
                        raise NotImplementedError('CMEMS required CP_pid, but it is not available from new code')
                        #build_dataproduct(dataset_zip, dataset, key, CP_pid)
                        #try:
                        #    if config_copernicus['do_upload']:
                        #        successful_upload_CMEMS, cmems_err_msg = upload_to_copernicus('nrt_server', dataset,
                        #                                                                      platforms)
                        #except Exception:
                        #    logging.error('Exception occurred: ', exc_info=True)
                        #    successful_upload_CMEMS = 0

                        #slack_export_report('CMEMS', platform_name, dataset, successful_upload_CMEMS, cmems_err_msg)
                    else:
                        successful_upload_CMEMS = True  # No export => no failure to report to QuinCe

                successful_upload = bool(successful_upload_CP) & bool(successful_upload_CMEMS)
                if successful_upload:
                    report_complete_export(dataset['id'])
                else:
                    report_abandon_export(dataset['id'])

    except Exception as e:
        exc_type, exc_obj, exc_tb = sys.exc_info()
        fname = os.path.split(exc_tb.tb_frame.f_code.co_filename)[1]
        except_msg = (f'Failed to run. Encountered: {e} \n \
      type: {exc_type} \n file name: {fname} \n line number: {exc_tb.tb_lineno}')

        post_slack_msg(except_msg, SLACK_ERROR_MSG)
        logging.exception('')
        try:
            if dataset is not None:
                report_abandon_export(dataset['id'])
        except Exception as e:
            post_slack_msg(f'Failed to abandon QuinCe export {e}', SLACK_ERROR_MSG)


def icos_upload(raw_filenames, manifest, dataset_zip, dataset):
    upload_result = cp_upload(manifest, dataset, dataset_zip, raw_filenames)
    slack_export_report('Carbon Portal', get_platform_name(manifest), dataset, upload_result, None)
    return upload_result


if __name__ == '__main__':
    main()
