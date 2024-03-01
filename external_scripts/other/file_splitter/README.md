# File Combiner
This is a pre-processing script for NRT data files from certain stations that produce files containing data from multiple sources on different lines. These files must be split so that QuinCe can correctly distinguish the lines, because it expects files to have the same format on every line.

The logistics of performing this task does not easily fit within the existing NRT processing scripts, so it has been moved into its own independent script. The output of this script will then be fed to the main NRT scripts.

### Notes
At the time of writing there is only one data source using this script, so the generic principles are extrapolations to possible behaviours.

## General Principle
This script will run periodically (usually once per day), controlled by cron jobs or similar. Each time the script is run, it will check the file source for new files since the last run. Any new files will be downloaded, and their contents split into separate files. The last retrieved file will be stored as the starting point for the next run.

The different line formats are detected by a specific string on the line - an identifier in the first column is the option seen so far.

## File Sources
The script will support a number of file sources, written as modules in the same manner as for the main NRT scripts. (Some of the logic will be identical even if the code is not shared directly.)

## File Logic
The logic for detecting new files is not as sophisticated as the standard NRT logic. It will only check for files that have been created or modified since the last run. There will be no checks to see if a file has been processed before (i.e. modified), so re-uploaded files will cause problems.

## Output
The output from the script will be the separated files placed in a specified directory. This directory will then be used as a file source for the main NRT script.

The files will be named as `<identifier>_<original_filename>`. For the single station accommodated so far, a file `20231016_1697493914.txt` will generate the files `Pro-CV_20231016_1697493914.txt` and `Seabird_20231016_1697493914.txt`, with the prefixes defined in the configuration file.

## Troubleshooting
### Google Drive: File list is always empty
API access must be performed through a Service Worker account (whose key is stored in the `credentials.json` file). Make sure the folder has been shared with this account. 
