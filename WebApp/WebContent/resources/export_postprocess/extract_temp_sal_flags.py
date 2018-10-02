''' CSV modding for Steve,  adds boolean variable to confirm that salt'''
import pandas as pd
file='gofl20150410-sst_salinity_qc.csv'
df=pd.read_csv(file)

def get_error(row,error_name):    
	if pd.isnull(row['QC Message']):
		return False
	return error_name in row['QC Message']

df['Temperature error']=df.apply(lambda row: get_error(row,'temperature'),axis=1)

df['Salinity error']=df.apply(lambda row: get_error(row,'Salinity'),axis=1)

df.to_csv('out.csv')