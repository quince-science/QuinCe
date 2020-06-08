# Ingests csv file on the format 'L0_file \t LinkedL2_file' and creates SQL database
# Maren 20200519
import sqlite3

DB = 'SOCATexport.db'
filename = '48MB PALOMA/L0.tsv'

def main():
  c = create_connection()
  with open(filename,'r',encoding="utf8",errors='ignore') as file:
      content = file.readlines()

      for line in content:
        [L0,L2] = line.split('\t')
        if L2 is not ' ' and L2 is not '\n':
          expocode = L2.split('_')[0]
          print(expocode)
          try:
            c.execute("SELECT L0 FROM L0Links WHERE expocode = ? ",[expocode])
            previous_entry = c.fetchone() 
            if previous_entry:
              if L0 not in str(previous_entry[0]):
                print('adding: '+ L0)
                L0s = str(previous_entry[0]) + ';' +  L0
                c.execute("UPDATE L0Links SET L0=? WHERE expocode = ?",(L0s,expocode))
              else:
                print('already added: ' + L0)
            else:
              c.execute("INSERT INTO L0Links (expocode,L2,L0) VALUES (?,?,?)",(expocode,L2,L0))
          except Exception as e:
            raise Exception(f'Adding/Updating database failed: {filename}', exc_info=True)



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
