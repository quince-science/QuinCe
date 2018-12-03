#!/usr/bin/env python3
''' 
This script finds error flags denoting bad intake temperature and 
salinity values, it creates a corresponding boolean value in a 
separate column.
This script requires two variables as system input arguments;
 - read_file
 - write_file

 -Maren 16.11.18
 '''

import sys
import pandas as pd

try:
    if len(sys.argv) is not 3 :
        print('Excpected inputfile and outputfile as system'
             'arguments. \nPlease try again adding: '
             '<read_file write_file> '
             'at the end of your command.')
    else:
        read_file = sys.argv[1]
        write_file = sys.argv[2]

        df = pd.read_csv(read_file) #dataframe

        temperature_err_msg = ['intake temperature','sst']
        salinity_err_msg = ['salinity','sss']

        def get_error(row,error_name):
            if pd.isnull(row['QC Message']):
                return False
            else:
                return (error_name[0] in row['QC Message'].lower()
                    or error_name[1] in row['QC Message'].lower())

        df['Temperature error'] = df.apply(lambda row: 
        	get_error(row,temperature_err_msg),axis=1)
        
        df['Salinity error'] = df.apply(lambda row: 
        	get_error(row,salinity_err_msg),axis=1)

        df.to_csv(write_file)     

except Exception as e:
    print('Failed to extract temperature and salinity flags:,', e)