import argparse
from bisect import bisect_left
import json
import mysql.connector
from quince_setup import QuinceSetup
import sys
from tqdm import tqdm

'''
Script to delete unused NaN values from the database. As of version 13.0.6
this shouldn't be happening any more, but it can still be used to find
and tidy up any strays.
'''

def get_prop(properties, name):
    match = re.search(f'%{name}%=(.*)', properties)
    return match.group(1)


def in_list(a, x):
    'Locate the leftmost value exactly equal to x'
    result = False

    i = bisect_left(a, x)
    if i != len(a) and a[i] == x:
        result = True
    return result


def get_nans(conn):
    print('Locating NaN values...')
    cursor = conn.cursor()
    cursor.execute('SELECT id FROM sensor_values WHERE value IS NULL ORDER BY id')
    null_sensor_values = []
    for (sv_id) in cursor:
        null_sensor_values.append(int(sv_id[0]))
    cursor.close()
    return null_sensor_values


def get_used_sensor_values(conn):
    print("Locating used sensor values")
    used_sensor_values = []
    cursor = conn.cursor()

    # Get the count for the progress bar
    cursor.execute('SELECT COUNT(*) FROM measurements')
    for row in cursor:
        count = int(row[0])

    cursor.execute('SELECT measurement_values FROM measurements')
    for row in tqdm(cursor, total=count):
        mv_json = row[0]
        if mv_json is not None:
            mv = json.loads(mv_json)
            for (sensor_type) in mv:
                for sv in mv[sensor_type]['svids']:
                    used_sensor_values.append(sv)
                for sv in mv[sensor_type]['suppids']:
                    used_sensor_values.append(sv)

    cursor.close()
    return sorted(set(used_sensor_values))

def get_datasets(conn, unused_nans):
    datasets = []

    cursor = conn.cursor()
    cursor.execute(f"""
        SELECT DISTINCT c.dataset_id FROM coordinates c
        INNER JOIN sensor_values sv ON c.id = sv.coordinate_id
        WHERE sv.id IN ({','.join([str(x) for x in unused_nans])}) ORDER BY c.dataset_id
    """)

    for (ds_id) in cursor:
        datasets.append(ds_id[0])

    cursor.close()
    return datasets

def delete_sensor_values(conn, ids):
    print('Deleting sensor values')
    cursor = conn.cursor()

    for id in tqdm(ids):
        cursor.execute(f'DELETE FROM sensor_values WHERE id = {id}')

    cursor.close()


def main(conn):
    nans = get_nans(conn)
    used_sensor_values = get_used_sensor_values(conn)

    unused_nans = []
    for nan in nans:
        if not in_list(used_sensor_values, nan):
            unused_nans.append(nan)

    print(f'Total NaN values: {len(nans)}')
    print(f'Unused NaN values: {len(unused_nans)}')

    if (len(unused_nans) == 0):
        print('No unused NaNs found')
        sys.exit()

    print('Identifying affected datasets...')
    print(get_datasets(conn, unused_nans))

    do_delete = input('Delete unused NaN values? ')
    if do_delete.lower() == 'y':
        delete_sensor_values(conn, unused_nans)

    conn.commit()


# Command line processor
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Remove unused NULL sensor values from the database")
    parser.add_argument("quince_setup_file", help="Location of quince.setup file")

    args = parser.parse_args()

    quince_setup = QuinceSetup(args.quince_setup_file)
    host, port, db, user, password = quince_setup.get_db_info()

    with mysql.connector.connect(user=user, password=password, host=host, database=db) as db_conn:
        main(db_conn)

