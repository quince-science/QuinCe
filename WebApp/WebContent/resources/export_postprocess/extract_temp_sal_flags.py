import sys
import pandas as pd
''' Finds error flags denoting bad intake temperature and salinity values, creates a corresponding boolean value in a separate column, needs two
variables as system input arguments, read_from_file and write_to_file'''
try:
    if len(sys.argv)!=3:
        print('< Excpected inputfile and outputfile as system arguments. > \n< Please try again adding \'read_from_file\' \'write_to_file\' at the end of your command. >')
    else:
        inputfile=sys.argv[1]
        outputfile=sys.argv[2]

        df=pd.read_csv(inputfile)

        temperature_err_msg=['intake temperature','sst']
        salinity_err_msg=['salinity','sss']

        def get_error(row,error_name):
            if pd.isnull(row['QC Message']):
                return False
            return error_name[0]  in row['QC Message'].lower() or error_name[1]  in row['QC Message'].lower()

        df['Temperature error']=df.apply(lambda row: get_error(row,temperature_err_msg),axis=1)
        df['Salinity error']=df.apply(lambda row: get_error(row,salinity_err_msg),axis=1)

        df.to_csv(outputfile)        
except Exception as e:
    print('Failed to extract temperature and salinity flags:,', e)
