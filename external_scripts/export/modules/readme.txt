Directory for storing modules containing functions used by AutomatedExportMain.py.

Common contains all functions directed towards QuinCe, Slack and file processing functions common for all export-destinations.

CarbonPortal contains all functions directly related to exporting files from QuinCe to the ICOS Carbon Portal. The functions are divided into a main module and task specific submodules handling metadata, SQL-queries and HTTP-requestes.

CMEMS contains all functions directly related to exporting files from QuinCe to CMEMS, Copernicus Marine Environmental Services. The functions are divided into a main module and task specific submodules handling metadata, SQL-queries, FTP-requests and netCDF creation.

__init__.py is a required object for making these functions callable from another directory.

