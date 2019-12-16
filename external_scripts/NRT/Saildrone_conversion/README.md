SAILDRONE DATA RETRIEVAL AND CONVERSION
===============================================================================

The script 'saildrone_main.py' will request data from the saildrone API. The
data received are in json format. These are converted to csv files - one ocean
file and one biogeo file - which are merged.

The folder 'data_files' is where data are temporarily stored while script is
ran. The 'archive' folder within the 'data_files' folder stores all files
created by the script in separate folders named by timestamp of creation.

The folder 'saildrone_module' stores all functions used in the
'saildrone_main.py' script.