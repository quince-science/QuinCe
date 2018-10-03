''' Finds error flags denoting bad intake temperature and salinity values, creates a corresponding boolean value in a separate column'''
import pandas as pd
file='gofl20150410-sst_salinity_qc.csv'
df=pd.read_csv(file)

temperature_err_msg=['intake temperature','sst']
salinity_err_msg=['salinity','sss']

def get_error(row,error_name):
	if pd.isnull(row['QC Message']):
		return False
	return error_name[0]  in row['QC Message'].lower() or error_name[1]  in row['QC Message'].lower()

df['Temperature error']=df.apply(lambda row: get_error(row,temperature_err_msg),axis=1)

df['Salinity error']=df.apply(lambda row: get_error(row,salinity_err_msg),axis=1)

df.to_csv('out.csv')
