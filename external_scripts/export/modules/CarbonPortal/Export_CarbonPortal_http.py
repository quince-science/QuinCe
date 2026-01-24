"""
Carbon Portal module
Contains functions related to http-calls

Maren K. Karlsen 2020.10.29
"""

import urllib
import http.cookiejar
import logging
import toml
from icoscp.sparql.runsparql import RunSparql
from modules.CarbonPortal.CarbonPortalException import CarbonPortalException
from modules.CarbonPortal.Export_CarbonPortal_metadata import build_metadata_package
from modules.Common.data_processing import get_hashsum, get_file_from_zip, b64_to_b64_url
import pandas as pd

from modules.Common.Messaging import post_msg

# from py_func.meta_handling import get_hashsum, get_file_from_zip
'''Carbon Portal submission process
https://github.com/ICOS-Carbon-Portal/meta#data-object-registration-and-upload-instructions

1. post metadata package describing the data-object (JSON object)
2. put dataobject

OBJ_SPEC_URI is Carbon Portal URI for identifying data object

'''
OBJ_SPEC_URI = {
    'L0': 'http://meta.icos-cp.eu/resources/cpmeta/otcL0DataObject',
    'L1': 'http://meta.icos-cp.eu/resources/cpmeta/icosOtcL1Product_v2'
}

META_URL = 'https://meta.icos-cp.eu/upload'
OBJECT_BASE_URL = 'https://data.icos-cp.eu/objects/'
META_CONTENT_TYPE = 'application/json'
OBJECT_CONTENT_TYPE = 'text/csv'

with open('config_carbon.toml') as f:
    config = toml.load(f)


def upload_to_cp(auth_cookie, file, hashsum, meta):
    """
    Uploads metadata and data object to Carbon Portal
    """
    success = True

    logging.debug(f'\nPOSTING {file} metadata-object to {META_URL}')
    resp = str(push_object(
        META_URL, meta.encode('utf-8'), auth_cookie, META_CONTENT_TYPE, 'POST'))
    logging.info(f'{file} metadata upload response: {resp}')
    if 'IngestionFailure' in resp or 'error' in resp.lower():
        success = False
        logging.error(f'failed to upload metadata: {resp}')
    else:
        object_url = OBJECT_BASE_URL + hashsum
        logging.debug(f'PUTTING data-object: {file} to {object_url}')
        with open(file) as fin:
            data = fin.read().encode('utf-8')
        resp = str(push_object(object_url, data, auth_cookie, OBJECT_CONTENT_TYPE, 'PUT'))
        logging.info(f'{file} Upload response: {resp}')
        if 'IngestionFailure' in resp or 'already has new version' in resp or 'error' in resp.lower():
            logging.error(f'failed to upload datafile: {resp}')
            success = False

    return success, resp


def push_object(url, data, auth_cookie, content_type, method):
    """  http-posts/puts data-object to url with content-type and auth_cookie """

    headers = {'Content-Type': content_type, 'Cookie': 'cpauthToken=' + auth_cookie, }
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        response = urllib.request.urlopen(req).read()
        logging.debug(f'Post response: {response}')
    except urllib.error.HTTPError as e:
        code = e.code
        msg = e.read()
        post_msg(f'\n\nHTTP error:  {code} {method} failed,\n {data} not sent. \
      \n\n Error message: {msg}\n', status=1)
        logging.error(f'HTTP error:  {code} {method} failed,\n {data} not sent.\
      \n\n Error message: {msg}\n')
        response = f'HTTP error:  {code}, {msg}'
    except Exception as e:
        msg = e.message if hasattr(e, 'message') else e
        post_msg(f'{method} failed. Error message: {msg}\n', status=1)
        logging.exception(f'{method} failed,\n {data} not sent, {msg}')
        response = f'Error: {method} failed: {msg}'

    return response


def get_auth_cookie():
    """ Returns authentication cookie from Carbon Portal. """
    logging.debug('Obtaining authentication cookie')
    auth_cookie = None

    auth_url = config['CARBON']['auth_url']
    auth_mail = config['CARBON']['auth_mail']
    auth_pwd = config['CARBON']['auth_pwd']
    auth_values = {'mail': auth_mail, 'password': auth_pwd}

    cookies = http.cookiejar.LWPCookieJar()
    handlers = [
        urllib.request.HTTPHandler(),
        urllib.request.HTTPSHandler(),
        urllib.request.HTTPCookieProcessor(cookies)
    ]
    opener = urllib.request.build_opener(*handlers)

    data = urllib.parse.urlencode(auth_values).encode('utf-8')
    req = urllib.request.Request(auth_url, data)
    opener.open(req)

    for cookie in cookies:
        if cookie.name == 'cpauthToken':
            logging.debug(f'Cookie: {cookie.value}')
            auth_cookie = cookie.value
        else:
            logging.debug('No cookie obtained')

    return auth_cookie


def run_sparql(query):
    return RunSparql(sparql_query=query, output_format='pandas').run()


def get_station_uri(name):
    station_query = f"""
prefix xsd: <http://www.w3.org/2001/XMLSchema#>
prefix cpmeta: <http://meta.icos-cp.eu/ontologies/cpmeta/>
select ?uri
from <http://meta.icos-cp.eu/resources/icos/>
where {{
    ?uri a cpmeta:OS ; cpmeta:hasStationId "{name}"^^xsd:string .
}}
"""

    query_result = run_sparql(station_query)
    if query_result.empty:
        raise CarbonPortalException(f'Station {name} not found')

    return query_result['uri'][0]


def get_overlapping_datasets(station_uri, start_date, end_date):
    # This query gets all datasets for the station that overlap
    # the specified time period
    query = f"""
prefix cpmeta: <http://meta.icos-cp.eu/ontologies/cpmeta/>
prefix prov: <http://www.w3.org/ns/prov#>
prefix xsd: <http://www.w3.org/2001/XMLSchema#>
select ?dobj ?fileName ?hashSum ?dataLevel ?timeStart ?timeEnd ?next_version 
where {{
  ?dobj cpmeta:hasObjectSpec ?spec .
  ?dobj cpmeta:wasAcquiredBy/prov:wasAssociatedWith <{station_uri}> .
  ?dobj cpmeta:hasSizeInBytes ?size .
  ?dobj cpmeta:hasName ?fileName .
  ?dobj cpmeta:hasSha256sum ?hashSum .
  ?dobj cpmeta:wasSubmittedBy/prov:endedAtTime ?submTime .
  ?dobj cpmeta:wasAcquiredBy [prov:startedAtTime ?timeStart ; prov:endedAtTime ?timeEnd ] .
  FILTER(?timeEnd >= "{start_date.strftime('%Y-%m-%dT%H:%M:%SZ')}"^^xsd:dateTime && ?timeStart <= "{end_date.strftime('%Y-%m-%dT%H:%M:%SZ')}"^^xsd:dateTime)
  ?spec cpmeta:hasDataLevel ?dataLevel ; cpmeta:hasAssociatedProject <http://meta.icos-cp.eu/resources/projects/icos> .
  FILTER(?dataLevel in ("1"^^xsd:integer, "2"^^xsd:integer))
  FILTER NOT EXISTS {{[] cpmeta:isNextVersionOf ?dobj}}
  OPTIONAL{{?next_version cpmeta:isNextVersionOf ?dobj}}
}}
order by DESC(?submTime)
"""

    query_result = run_sparql(query)

    # Now we filter it to just get the most recent datasets
    filtered = pd.DataFrame(data=None, columns=query_result.columns)

    for (index, candidate) in query_result.iterrows():
        if filtered.empty:
            filtered.loc[len(filtered)] = candidate
        else:
            for (i2, compare) in filtered.iterrows():
                if not overlaps(candidate['timeStart'], candidate['timeEnd'], compare['timeStart'], compare['timeEnd']):
                    filtered.loc[len(filtered)] = candidate

    return filtered


def overlaps(start_time_1, end_time_1, start_time_2, end_time_2):
    return max(start_time_1, start_time_2) < min(end_time_1, end_time_2)


def get_existing_files(station_uri, filenames, data_object_spec):
    in_clause = ','.join([f'"{file}"^^xsd:string' for file in filenames])
    query = f"""
prefix cpmeta: <http://meta.icos-cp.eu/ontologies/cpmeta/>
prefix prov: <http://www.w3.org/ns/prov#>
prefix xsd: <http://www.w3.org/2001/XMLSchema#>
select ?dobj ?hasNextVersion ?fileName ?hashSum ?submTime
where {{
  VALUES ?spec {{<{data_object_spec}>}}
  ?dobj cpmeta:hasObjectSpec ?spec .
  ?dobj cpmeta:hasSha256sum ?hashSum .
  BIND(EXISTS{{[] cpmeta:isNextVersionOf ?dobj}} AS ?hasNextVersion)
  VALUES ?station {{<{station_uri}>}}
  ?dobj cpmeta:wasAcquiredBy/prov:wasAssociatedWith ?station .
  ?dobj cpmeta:hasName ?fileName .
  FILTER(?fileName in ({in_clause}))
  ?dobj cpmeta:wasSubmittedBy/prov:endedAtTime ?submTime .
}}
order by desc(?submTime)
"""

    return run_sparql(query)


def get_info(base64_hashsum):
    uri = f'https://meta.icos-cp.eu/objects/{b64_to_b64_url(base64_hashsum)[:24]}'

    query = f"""
prefix cpmeta: <http://meta.icos-cp.eu/ontologies/cpmeta/>
prefix prov: <http://www.w3.org/ns/prov#>
prefix xsd: <http://www.w3.org/2001/XMLSchema#>
select ?hasNextVersion ?timeStart ?timeEnd ?dataLevel
where {{
    VALUES ?dobj {{<{uri}>}}
    ?dobj cpmeta:hasStartTime | (cpmeta:wasAcquiredBy / prov:startedAtTime) ?timeStart .
    ?dobj cpmeta:hasEndTime | (cpmeta:wasAcquiredBy / prov:endedAtTime) ?timeEnd .
    ?dobj cpmeta:hasObjectSpec ?spec .
    ?spec cpmeta:hasDataLevel ?dataLevel .
    BIND(EXISTS{{[] cpmeta:isNextVersionOf ?dobj}} AS ?hasNextVersion)
}}
"""

    result = run_sparql(query)
    result = result.astype({
        'hasNextVersion': 'bool',
        'timeStart': 'datetime64[ns, UTC]',
        'timeEnd': 'datetime64[ns, UTC]',
        'dataLevel': 'int'})
    return result.iloc[0]

def upload_file(cookie, zip_source, manifest, file_index, filename, level, data_object_spec,
                deprecate_hashsum, links):
    extracted_file = get_file_from_zip(zip_source, filename)
    hashsum = get_hashsum(extracted_file)

    partial_upload = False
    if deprecate_hashsum:
        deprecated_info = get_info(deprecate_hashsum[0])
        if level == 'L1':
            new_start_date = pd.to_datetime(manifest['manifest']['metadata']['startdate'])
            if new_start_date > deprecated_info['timeStart']:
                partial_upload = True
        elif level == 'L2':
            new_start_date = pd.to_datetime(manifest['manifest']['metadata']['startdate'])
            new_end_date = pd.to_datetime(manifest['manifest']['metadata']['enddate'])
            if deprecated_info['dataLevel'] == 1 and \
                new_start_date != deprecated_info['timeStart'] or new_end_date != deprecated_info['timeEnd']:

                partial_upload = True

    metadata = build_metadata_package(extracted_file, manifest, file_index, hashsum, data_object_spec,
                                      level, links, deprecate_hashsum, partial_upload)

    if not config['CARBON']['do_upload']:
        logging.info('Uploads disabled - dry run only')
        upload_result = False
    else:
        upload_result, upload_response = upload_to_cp(cookie, extracted_file, hashsum, metadata)

    return upload_result
