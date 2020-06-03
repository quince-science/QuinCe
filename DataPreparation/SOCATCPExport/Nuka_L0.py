# Script to link L0 to relevant L2 Nuka Arctica
# Maren Karlsen 20200602

# Given directory of all L0s and database of all Nuka L2 files.

# For each L2 
# fetch start and end date from db
# convert to day of year/julian day
# iterate through all L0
#   if julian day of L0 falls with in [start:end] of L2, append to list
# submit relation to L0 links db

import os
import sqlite3
import datetime

DB = 'SOCATexport.db'
L0dir = './26NA Nuka Arctica/L0'

def main():
  c = create_connection()
  c.execute('SELECT expocode,filename,startDate,endDate from export where platform =? and level = ?',['26NA','L2'])
  L2list = c.fetchall()
  for L2 in L2list:
    [expocode,filename,startDate,endDate] = L2
    startDate = datetime.datetime.strptime(startDate,'%Y-%m-%dT%H:%M:%S.%fZ')
    endDate = datetime.datetime.strptime(endDate,'%Y-%m-%dT%H:%M:%S.%fZ')
    year = startDate.year
    startDay = datetime.datetime.strftime(startDate,'%j')
    endDay = datetime.datetime.strftime(endDate,'%j')
    L0=[]

    for root,dirs,files in os.walk(L0dir):
      for file in files:
        if file.endswith('dat.txt'):
          [year0,day0,ext] = file.split('-')
          if str(year) in year0:
            if startDay <= day0 <= endDay:
              L0 += [file]
    print(expocode,L0)
    L0_string = ';'.join(L0)
    c.execute('INSERT INTO L0Links (expocode,L2,L0) values (?,?,?)',[expocode,filename,L0_string])
    #c.execute('UPDATE L0Links SET L0 = ? where expocode = ?',[L0_string,expocode])
  


def create_connection():
  ''' creates connection and database if not already created '''
  conn = sqlite3.connect(DB, isolation_level=None)
  c = conn.cursor()
  c.execute(''' CREATE TABLE IF NOT EXISTS L0Links (
              expocode TEXT PRIMARY KEY,
              L2 TEXT UNIQUE,
              L0 TEXT
              )''')
  return c

if __name__ == '__main__':
  main()

def fetch_L2_dates(platform):
  """ updates start and enddate in database based on L2 content"""
  c.execute('SELECT expocode,filepath from export where platform =? and level = ?',[platform,'L2'])
  L2list = c.fetchall()
  for L2 in L2list:
    [expocode,filepath] = L2
    with open(filepath,'r',encoding="utf8",errors='ignore') as file:
        content = file.readlines()
        startDate = content[1].split(',')[0]
        endDate = content[-1].split(',')[0] 
    c.execute('UPDATE export SET startDate = ?, endDate = ? where filepath = ?',[startDate,endDate,filepath])
