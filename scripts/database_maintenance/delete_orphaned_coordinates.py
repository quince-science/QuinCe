import argparse
import pandas as pd
from quince_setup import QuinceSetup
import sqlalchemy
from sqlalchemy.orm import Session
import sys
from tqdm import tqdm

# Load all orphaned coordinate records with dataset and instrument info
def get_orphaned_coordinates(conn):
    sql = '''
        SELECT i.id AS instrument_id, i.platform_name AS platform_name,
        i.name AS instrument_name, d.id AS dataset_id, d.name AS dataset_name,
        COUNT(c.id) AS count FROM coordinates c
        INNER JOIN dataset d ON c.dataset_id = d.id
        INNER JOIN instrument i ON d.instrument_id = i.id
        WHERE c.id NOT IN (SELECT DISTINCT coordinate_id FROM sensor_values)
        AND c.id NOT IN (SELECT DISTINCT coordinate_id FROM measurements)
        GROUP BY d.id
        ORDER BY platform_name, instrument_name, dataset_name
    '''

    return pd.read_sql(sql, conn)

# Delete orphaned coordinates
def delete_orphaned_coordinates(conn):
    stmt = sqlalchemy.text('''
        DELETE FROM coordinates
        WHERE id NOT IN (SELECT DISTINCT coordinate_id FROM sensor_values)
        AND id NOT IN (SELECT DISTINCT coordinate_id FROM measurements)
    ''')

    with Session(conn) as session:
        session.execute(stmt)
        session.commit()

def main(conn):
    print('Finding orphaned coordinates...')
    counts = get_orphaned_coordinates(conn)
    
    if (len(counts.index) == 0):
        print('No orphaned coordinates found')
        sys.exit()
    
    print(f'Found {counts['count'].sum()} orphaned coordinates in {len(counts.index)} datasets')
    print(counts)

    do_delete = input('Delete orphaned coordinates? ')
    if do_delete.lower() == 'y':
        delete_orphaned_coordinates(conn)


# Command line processor
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Remove unused NULL sensor values from the database")
    parser.add_argument("quince_setup_file", help="Location of quince.setup file")

    args = parser.parse_args()

    quince_setup = QuinceSetup(args.quince_setup_file)
    host, port, db, user, password = quince_setup.get_db_info()

    try:
        engine = sqlalchemy.create_engine(
            f'mariadb+mariadbconnector://{user}:{password}@{host}:{port}/{db}'
        )

        main(engine)
    finally:
        engine.dispose()

