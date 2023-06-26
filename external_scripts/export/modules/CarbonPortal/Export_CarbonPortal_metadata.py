'''
Carbon Portal module
Metadata package construction

Maren K. Karlsen 2020.10.29
'''
import json
import os
import logging
import datetime

from modules.Common.data_processing import get_platform_name, get_platform, get_export_filename

def build_metadata_package(file,manifest,index,hashsum,
  obj_spec,level,L0_hashsums,is_next_version,partial_upload):
  '''  Builds metadata-package, step 1 of 2 Carbon Portal upload process.
  https://github.com/ICOS-Carbon-Portal/meta#registering-the-metadata-package
  returns metadata json object
  '''

  export_filename = get_export_filename(file,manifest,level)
  platform_name = get_platform_name(manifest, True)
  platform = get_platform(platform_name)

  logging.debug('Constructing metadata-package')
  creation_date = datetime.datetime.utcnow().isoformat()+'Z'

  meta= {
    'submitterId': platform['submitter_id'],
    'hashSum': hashsum,
    'specificInfo': {'station': platform['cp_url'],},
    'objectSpecification': obj_spec
    }

  if 'L1' in level or 'L2' in level:  # L1 and L2 specific metadata
    meta['fileName'] = export_filename
    meta['specificInfo']['nRows'] = manifest['manifest']['exportFiles']['ICOS OTC']['records']

    comments = manifest['manifest']['metadata']['comments'].strip()
    if len(comments) > 0:
      comments += '\n'
    comments += manifest['manifest']['metadata']['quince_information']

    meta['specificInfo']['production'] = (
      {'creator': 'http://meta.icos-cp.eu/resources/organizations/OTC',
      'contributors': [],
      'creationDate': creation_date,
      'comment': comments})

    # We only link L2 datasets to the raw files. L1 don't get linked
    # because they get updated so frequently
    if 'L2' in level:
      meta['specificInfo']['production']['sources'] = L0_hashsums
    if is_next_version is not None:
      meta['isNextVersionOf'] = is_next_version

  if 'L0' in level:  # L0 specific metadata
    meta['specificInfo']['acquisitionInterval'] = ({
      'start':manifest['manifest']['raw'][index]['startDate'],
      'stop': manifest['manifest']['raw'][index]['endDate']})
    meta['fileName'] = os.path.split(file)[-1]

  meta['references'] = {
    'duplicateFilenameAllowed': True,
    'partialUpload': partial_upload
  }

  meta_JSON = json.dumps(meta) # converting from dictionary to json-object
  logging.debug(f'metadata-package: {type(meta_JSON)}\n \
    {json.dumps(json.loads(meta_JSON), indent = 4)}')

  return meta_JSON



