#!/usr/bin/env python.
# Script to transform data files using the SOCAT format to files readable by ICOS Carbon Portal
# Maren K Karlsen 20200518

# Accepts folder or single file
# Assumes consistent SOCAT format, CP export format used by QuinCe 20200518 
# Alterations:
# - metadata removed
# - headers changed to CP vocaboulary 
# - datetime combined to single column
# - Longitude reformated from 0:360 to -180:180
# headers not identified by CP will be ignored by the ingestion engine. They will still be part of the dataset  -   but not as linked or searchable data.

# Header overview  SOCAT - CP 
#   Expocode  -  ignored
#   version SOCAT_DOI  -  ignored
#   QC_Flag yr  -  ignored
#   [mon day hh mm ss]  -  Date/Time
#   longitude [dec.deg.E]  -  Longitude
#   latitude [dec.deg.N]  -  Latitude
#   sample_depth [m]  -  Depth [m]
#   sal    -  P_sal [psu]
#   SST [deg.C]   -  Temp [degC]
#   Tequ [deg.C]  -  Temperature of Equilibration [degC]
#   PPPP [hPa]  -  Atmospheric Pressure [hPa]
#   Pequ [hPa]  -  Equilibrator Pressure (relative) [hPa]
#   WOA_SSS NCEP_SLP [hPa]  -  ignored
#   ETOPO2_depth [m]  -  ignored
#   dist_to_land [km]  -  ignored
#   GVCO2 [umol/mol]  -  ignored
#   xCO2water_equ_dry [umol/mol]  -  CO2 Mole Fraction [umol mol-1]
#   xCO2water_SST_dry [umol/mol]  -  ignored
#   pCO2water_equ_wet [uatm]  -  pCO2 In Water - Equilibrator Temperature [uatm]
#   pCO2water_SST_wet [uatm]  -  pCO2 [uatm]
#   fCO2water_equ_wet [uatm]  -  ignored
#   fCO2water_SST_wet [uatm]  -  ignored
#   fCO2rec [uatm]  -  fCO2 [uatm]
#   fCO2rec_src  - ignored
#   fCO2rec_flag  -  fCO2 [uatm] QC Flag

# Added Columns:
#   pCO2 [uatm] QC Flag - 9
#   P_sal [psu] QC Flag - 9
#   Temp [degC] QC Flag - 9

import os
import pandas as pd
import shutil
import datetime

filenames=[] 
now = datetime.datetime.now().strftime('%Y%m%d')

for root,dirs,files in os.walk('.'):
  if 'L2' in root:
    for file in files:
      if file.endswith('.tsv')
      filenames += [root + '/' + file]

for filename in filenames:
  # Identify start of data
  with open(filename,'r',encoding="utf8",errors='ignore') as file:
    content = file.readlines()

    for line_nr, line in enumerate(content):
      if line.startswith('Expocode\tversion\tSOCAT_DOI'):
        data_start = line_nr

  df = pd.read_csv(filename, skiprows= data_start-1,sep='\t')
  
  #Concatenate Datetime columns
  df['Date/Time'] = pd.to_datetime(
    df['yr'].map(str)+'-'+df['mon'].map(str)+'-'+df['day'].map(str)
    +'T'+df['hh'].map(str)+':'+df['mm'].map(str)+':'+df['ss'].map(str)+'Z'
    ).dt.strftime('%Y-%m-%dT%H:%M:%S.%fZ')

  #Change Longitude framework from 0:360 to -180:180
  df['Longitude'] = df['longitude [dec.deg.E]'].apply(lambda x: x if x < 180 else -(360-x))

  # Drop redundant columns  
  df = df.drop(columns=['yr','mon','day','hh','mm','ss','longitude [dec.deg.E]'])

  # Rename headers
  df = df.rename(columns={
    'latitude [dec.deg.N]':'Latitude',
    'sample_depth [m]':'Depth [m]',
    'sal':'P_sal [psu]',
    'SST [deg.C]':'Temp [degC]',
    'Tequ [deg.C]':'Temperature of Equilibration [degC]',
    'PPPP [hPa]':'Atmospheric Pressure [hPa]',
    'Pequ [hPa]':'Equilibrator Pressure (relative) [hPa]',
    'xCO2water_equ_dry [umol/mol]':'CO2 Mole Fraction [umol mol-1]',
    'pCO2water_equ_wet [uatm]':'pCO2 In Water - Equilibrator Temperature [uatm]',
    'pCO2water_SST_wet [uatm]':'pCO2 [uatm]',
    'fCO2rec [uatm]':'fCO2 [uatm]',
    'fCO2rec_flag':'fCO2 [uatm] QC Flag'})

  df['pCO2 [uatm] QC Flag'] = 9
  df['P_sal [psu] QC Flag'] = 9
  df['Temp [degC] QC Flag'] = 9

  # Reorder columns
  cols = df.columns.tolist()
  cols.insert(0,cols.pop(cols.index('Date/Time')))
  cols.insert(2,cols.pop(cols.index('Latitude')))
  cols.insert(3,cols.pop(cols.index('Longitude')))
  cols.insert(4,cols.pop(cols.index('Depth [m]')))

  df = df[cols]

  # Write content to file
  converted_filename = filename.replace('tsv','csv')
  df.to_csv(converted_filename,index=False,sep=',',float_format='%.3f',na_rep='NaN')

