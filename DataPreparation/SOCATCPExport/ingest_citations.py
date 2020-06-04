# ingests citation file written by Rocío on the format <Expocode;CitationString> and writes them to db
# Maren 20200519
import sqlite3

DB = 'SOCATexport.db'
filename = 'SOCAT OTC citations.csv'

def main():
  c = create_connection()
  with open(filename,'r',encoding="utf8",errors='ignore') as file:
      content = file.readlines()

      for line in content:
        [expocode,citation,doi] = line.split(';')
        print(expocode)
        c.execute("INSERT INTO citation \
        (expocode,citation,doi) VALUES (?,?,?)",(expocode,citation,doi))


def create_connection():
  ''' creates connection and database if not already created '''
  conn = sqlite3.connect(DB, isolation_level=None)
  c = conn.cursor()
  c.execute(''' CREATE TABLE IF NOT EXISTS citation (
              expocode TEXT PRIMARY KEY,
              citation TEXT,
              doi TEXT
              )''')
  return c

if __name__ == '__main__':
  main()
