SAILDRONE DATA RETRIEVAL, CONVERSION, AND EXPORT TO QUINCE
===============================================================================

The script 'saildrone_main.py' (hereafter referred to as the main script)
requests data from saildrones through the saildrone API. The data are received
in json format. For each drone we request an ocean file, a biogeochemistry
file, and an atmospheric file. These files are converted to csv format and
merged together. If the script is ran successfully, the QuinCe FTP will receive
one such merged file per drone. The timestamp of the last record is stored so
that the next request can continue from this timestamp. The plan is to set up
the main script to run daily.

The main script depends on:
- various functions in the saildrone_module
- configurations from config.json
- information from the previous time the script was ran in stored_info.json

The script should be run in a virtual python environment, hence the pipfile.

The folder 'data_files' stores files produced by the main script. These files
serve no other purpose than to allow for tracking and a way to verify that the
process is correct. The json files, csv files and the final exported files gets
stored in directories named by the timestamp when the main script was ran. The
file names always start with the drone id. For the exported file, the file name
is followed by the start and end timestamp for the data it contains.