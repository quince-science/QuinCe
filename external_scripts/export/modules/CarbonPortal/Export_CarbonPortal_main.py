"""
Carbon Portal main module 
Carbon Portal triage

Maren K. Karlsen 2020.10.29

Carbon Portal submission process
https://github.com/ICOS-Carbon-Portal/meta#data-object-registration-and-upload-instructions

1. post metadata package describing the data-object (JSON object)
2. put dataobject

"""

import os.path
import toml
import warnings
import logging

from modules.CarbonPortal.CarbonPortalException import CarbonPortalException
from modules.Common.data_processing import get_file_from_zip, get_hashsum, get_platform_type, is_NRT, \
    get_platform_name, get_start_time, get_end_time
from modules.CarbonPortal.Export_CarbonPortal_http import get_auth_cookie, get_station_uri, get_overlapping_datasets, \
    get_existing_files, upload_file

with open('config_carbon.toml') as f:
    config = toml.load(f)
warnings.simplefilter("ignore", FutureWarning)

OBJ_SPEC_URI = {
    'L0': 'http://meta.icos-cp.eu/resources/cpmeta/otcL0DataObject',
    'SOOP': {
        'L1': 'http://meta.icos-cp.eu/resources/cpmeta/icosOtcL1Product_v2',
        'L2': 'http://meta.icos-cp.eu/resources/cpmeta/icosOtcL2Product'
    },
    'FOS': {
        'L1': 'http://meta.icos-cp.eu/resources/cpmeta/icosOtcFosL1Product',
        'L2': 'http://meta.icos-cp.eu/resources/cpmeta/icosOtcFosL2Product'
    }
}

SOOP_PLATFORMS = ['31', '32', '3B']
FOS_PLATFORMS = ['41']


def get_data_object_spec(platform_type, level):
    if level == 'L0':
        result = OBJ_SPEC_URI['L0']
    else:
        uri_platform_type = None
        if platform_type in SOOP_PLATFORMS:
            uri_platform_type = 'SOOP'
        elif platform_type in FOS_PLATFORMS:
            uri_platform_type = 'FOS'

        if level == 'L1':
            result = OBJ_SPEC_URI[uri_platform_type]['L1']
        else:
            result = OBJ_SPEC_URI[uri_platform_type]['L2']

    return result


def get_filename(dataset, manifest):
    format_dir = 'ICOS OTC'
    platform_name = get_platform_name(manifest)
    if platform_name == 'NO-SOOP-Nuka__Arctica':
        format_dir = format_dir + ' No Salinity Flags'

    return os.path.join(dataset['name'], 'dataset', format_dir, f'{dataset["name"]}.csv')


def cp_upload(manifest, dataset, dataset_zip, raw_filenames):
    station_uri = get_station_uri(get_platform_name(manifest, False))

    platform_type = get_platform_type(get_platform_name(manifest, True))
    upload_level = 'L1' if is_NRT(manifest) else 'L2'

    existing_datasets = get_overlapping_datasets(station_uri, get_start_time(manifest), get_end_time(manifest))

    data_object_spec = get_data_object_spec(platform_type, upload_level)
    data_filename = get_filename(dataset, manifest)
    deprecated_id = []
    bypass_upload = False

    if len(existing_datasets) > 1:
        raise CarbonPortalException(f'Dataset would deprecate multiple datasets: {list(existing_datasets["dobj"])}')
    elif existing_datasets.empty:
        # Just to be explicit: We will upload the dataset and don't need to deprecate anything
        pass
    else:
        deprecated_dataset = existing_datasets.iloc[0]

        extracted_file = get_file_from_zip(dataset_zip, data_filename)
        export_hashsum = get_hashsum(extracted_file, True)

        if export_hashsum == deprecated_dataset['hashSum']:
            logging.info('Hashsum has already been uploaded. Skipping.')
            bypass_upload = True

        if upload_level == 'L1':
            # We need to deprecate the previous dataset
            if int(deprecated_dataset['dataLevel']) > 1:
                raise CarbonPortalException(f'Cannot deprecate L{deprecated_dataset["dataLevel"]} dataset with L1')
            deprecated_id.append(deprecated_dataset['hashSum'])
        elif upload_level == 'L2':
            if int(deprecated_dataset['dataLevel']) == 1:
                deprecated_id.append(deprecated_dataset['hashSum'])
            else:
                if deprecated_dataset['fileName'] != os.path.basename(data_filename):
                    raise CarbonPortalException(
                        f'Cannot deprecate L2 dataset with different filename ({os.path.basename(data_filename)} -> {deprecated_dataset["fileName"]})')
                elif 'nextVersion' in deprecated_dataset:
                    raise CarbonPortalException(
                        f'Cannot deprecate L2 dataset because it already has a next version ({deprecated_dataset["dobj"]})')
                else:
                    deprecated_id.append(deprecated_dataset['hashSum'])

        else:
            raise CarbonPortalException(f'Unrecognised Data Level {upload_level}')

    if bypass_upload:
        upload_result = True
    else:
        # Now we upload the main (L1 or L2) dataset, deprecating the specified ID if required
        cookie = get_auth_cookie()

        # Upload all L0 files first - we need to link to them when we upload the main file
        link_hashums = []
        l0_basenames = [os.path.basename(file) for file in raw_filenames]
        existing_l0 = get_existing_files(station_uri, l0_basenames, OBJ_SPEC_URI['L0'])

        l0_index = -1
        for l0_file in raw_filenames:
            l0_index += 1
            basename = os.path.basename(l0_file)
            file_content = get_file_from_zip(dataset_zip, l0_file)
            hashsum = get_hashsum(file_content, True)

            upload_l0 = True
            previous_l0 = []
            
            existing_hashsums = existing_l0.loc[existing_l0['fileName'] == basename]['hashSum'].values
            if hashsum in existing_hashsums:
                upload_l0 = False
            else:
                if len(existing_hashsums) > 0:
                    previous_l0.append(existing_hashsums[0])

            if upload_l0:
                l0_upload_result = upload_file(cookie, dataset_zip, manifest, l0_index, l0_file, 'L0',
                                               OBJ_SPEC_URI['L0'], previous_l0, None)
                if not l0_upload_result:
                    raise CarbonPortalException('Failed to upload L0 file(s). Aborting')

            link_hashums.append(hashsum)

        # Now upload the main data object. L1 objects don't get linked to L0
        upload_result = upload_file(cookie, dataset_zip, manifest, 0, data_filename,
                                    upload_level, data_object_spec, deprecated_id, link_hashums)

    return upload_result
