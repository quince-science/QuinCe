"""
NB THIS IS NOT PART OF THE MAIN EXPORT SCRIPT - IT JUST USES SOME OF
THE LIBRARIES

Formats export files from QuinCe for submission to SOCAT

Files exported from QuinCe in the SOCAT format need some changes before they
can be sent to the SOCAT upload dashboard:

- Add header containing simple metadata
  (EXPO Code, Vessel Name, PIs, Vessel Type)
  This information will be read from the input filename and platforms.toml
- Rename QC columns

The script will take an QuinCe export ZIP file
Output files will be named <expocode>.tsv

Usage:

python socat_formatter.py <zip_file>

Steve Jones
"""
import argparse
import json

import toml
import re
import zipfile
import os.path


def main(infile):

    manifest, file_content = extract_zip(infile)
    metadata = build_metadata(manifest)

    # Write header
    out_file = f'{metadata["expocode"]}.tsv'
    with open(out_file, 'w') as out:
        out.write(f'Expocode: {metadata["expocode"]}\n')
        out.write(f'Vessel Name: {metadata["vessel_name"]}\n')
        out.write(f'PIs: {metadata["pis"]}\n')
        out.write(f'Org: {metadata["org"]}\n')
        out.write(f'Vessel type: {metadata["vessel_type"]}\n')
        out.write('Suggested QC: 2\n')

    with open(out_file, 'ab') as out:
        out.write(file_content)


def extract_zip(infile):
    """
    Extract the required parts of the QuinCe export ZIP
    """

    manifest = None
    content = None

    if not zipfile.is_zipfile(infile):
        raise zipfile.BadZipFile('Supplied file is not a ZIP file')

    basename = os.path.splitext(os.path.basename(infile))[0]

    with zipfile.ZipFile(infile) as in_zip:
        manifest_path = zipfile.Path(in_zip, f'{basename}/manifest.json')
        if not manifest_path.exists():
            raise KeyError('ZIP file is missing manifest.json')

        manifest = json.loads(manifest_path.read_text())
        dataset_name = manifest['manifest']['metadata']['name']

        dataset_filename = f'{basename}/dataset/SOCAT/{dataset_name}.tsv'
        dataset_path = zipfile.Path(in_zip, dataset_filename)
        if not dataset_path.exists():
            raise KeyError('ZIP file does not contain SOCAT export')
        content = in_zip.read(dataset_filename)

    return manifest, content


def build_metadata(manifest):
    with open('platforms.toml') as f:
        platforms = toml.load(f)

    metadata = {}

    dataset_name = manifest['manifest']['metadata']['name']

    m = re.match('(.*)(\\d\\d\\d\\d\\d\\d\\d\\d)',
                 dataset_name)

    platform = m[1]
    date = m[2]
    metadata['expocode'] = f'{platform}{date}'

    try:
        platform_info = platforms[platform]
        metadata['vessel_name'] = platform_info['name']
        metadata['vessel_type'] = \
            get_platform_type_name(platform_info['category_code'])

        metadata['pis'] = platform_info['author_list']
        metadata['org'] = platform_info['affiliation']
    except KeyError:
        raise Exception(f'Platform {platform} not found')

    return metadata


def get_platform_type_name(code):
    if code == '31':
        return 'Ship'
    elif code == '32':
        return 'Ship '
    elif code == '3B':
        return 'Saildrone'
    else:
        raise Exception(f'Unrecognised platform code {code}')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Prepare a SOCAT export from QuinCe for submission')
    parser.add_argument('zipfile', type=str, help='the QuinCe export file')
    args = parser.parse_args()
    main(args.zipfile)
    exit()
