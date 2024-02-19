# File Combiner
This is a pre-processing script for NRT data files from certain stations that produce large numbers of small files. This can happen if, for example, a station regular transmits raw data strings and these are stored as one file per transmission. The resulting volume of files is too unwieldy for QuinCe and related data stores (e.g. the ICOS Carbon Portal), so this script will combine multiple files to create many fewer files covering large periods.

The logistics of performing this task does not easily fit within the existing NRT processing scripts, so it has been moved into its own independent script. The output of this script will then be fed to the main NRT scripts.

### Notes
At the time of writing there is only one data source using this script, so the generic principles are extrapolations to possible behaviours.

## General Principle
This script will run periodically (usually once per day), controlled by cron jobs or similar. Each time the script is run, it will check the file source for new files since the last run. Any new files will be combined into a single file. The last retrieved file will be stored as the starting point for the next run.

## File Formats
Each file will contain one or more records, with one per line. Each line is analogous to a single line in a more traditional CSV file, so combining these records is conceptually trivial.

In some cases the incoming files will contain records from multiple sensors, so the lines will not be consistent. In this case the different line formats will be identified and recorded in separate files. These can then be loaded as separate file formats into QuinCe. The different line formats are detectable by a specific string on the line - an identifier in the first column is the option seen so far.

## File Sources
The script will support a number of file sources, written as modules in the same manner as for the main NRT scripts. (Some of the logic will be identical even if the code is not shared directly.)

## File Logic
The logic for detecting new files is not as sophisticated as the standard NRT logic. It will only check for files that have been created or modified since the last run, and concatenate those into a single output file. There will be no checks to see if a file has been processed before (i.e. modified), so re-uploaded files will cause problems.

Files are processed in order of ascending Modified Date, which is assumed to be sufficient to ensure that the output will be preserved in ascending date order.

## Output
The output from the script will be the combined files placed in a specified directory. This directory will then be used as a file source for the main NRT script.

## Troubleshooting
### Google Drive: File list is always empty
API access must be performed through a Service Worker account (whose key is stored in the `credentials.json` file). Make sure the folder has been shared with this account. 
