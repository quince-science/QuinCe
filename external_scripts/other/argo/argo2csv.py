'''
Extract data from an Argo dataset into a CSV file for use in QuinCe.

If you see a message saying "Skipped n invalid coordinates", these
are values for variables measured at points where the PRES value
of the coordinate is empty.
'''
import argparse
from datetime import datetime, timedelta
import glob
from netCDF4 import Dataset
import numpy.ma as ma
import os
import pandas as pd
import sqlite3
from tqdm import tqdm


# Exception thrown when a coordinate can't be found
class NoCoordinateException(Exception):
    def __init__(self, cycle, profile, direction, sequence):
        super().__init__(f'Cannot find coordinate record for {cycle} {profile} {direction} {sequence}')
        self.coord_info = f'{cycle} {profile} {direction} {sequence}'


# Locate a coordinate in the intermediate database
def get_coord(cur, cycle, profile, direction, sequence):
    cur.execute(f"""SELECT id FROM coordinates WHERE cycle_number = {cycle} AND
        profile_number = {profile} AND direction = '{direction}' AND profile_sequence = {sequence}""")
    id = cur.fetchone()
    if id is None:
        raise NoCoordinateException(cycle, profile, direction, sequence)
    
    return id[0]


#############################
#
# Let's go!

# Command line arguments
parser = argparse.ArgumentParser(
    prog='argo2csv',
    description='Extract data from an Argo deployment dataset into a CSV for import into QuinCe'
)

parser.add_argument('deployment_id', type=str, help='Argo deployment ID. The script will search for a directory with this name.')
parser.add_argument('d_file_vars', type=str, help='Variables to extract from D-files')
parser.add_argument('bd_file_vars', type=str, help='Variables to extract from BD-files')
parser.add_argument('--data_dir', type=str, default='.', help='Directory containing deployment data files. Defaults to the current directory.')
parser.add_argument('--save_db', action='store_true', help='Store the intermediate database to disk for debugging')
parser.add_argument('--display_progress', action='store_true', help='Display progress')

args = parser.parse_args()

d_file_variables = [item for item in args.d_file_vars.split(',')]
bd_file_variables = [item for item in args.bd_file_vars.split(',')]

# Set up in-memory database
db = sqlite3.connect(":memory:")
cur = db.cursor()

# Create the coordinates table
cur.execute("DROP TABLE IF EXISTS coordinates")
cur.execute("""CREATE TABLE coordinates (
    "id" INTEGER PRIMARY KEY,
    "cycle_number" INTEGER NOT NULL,
    "profile_number" INTEGER NOT NULL,
	"direction" TEXT NOT NULL,
    "profile_sequence" INTEGER NOT NULL,
	"pressure" REAL NOT NULL,
	"timestamp" INTEGER,
	"longitude" REAL,
	"latitude" REAL,
	"source_file" TEXT NOT NULL
);""")

profiles_dir = os.path.join(args.data_dir, args.deployment_id, 'profiles')
d_files_pattern = f'D{args.deployment_id}_*.nc'
d_files = glob.glob(os.path.join(profiles_dir, d_files_pattern))

for file in tqdm(d_files, desc='Extracting coordinates', disable=not(args.display_progress)):
    filename = os.path.basename(file)
    nc = Dataset(file, 'r', format='NETCDF4')

    profile_count = nc.dimensions['N_PROF'].size

    reference_time =  datetime.strptime(
        nc.variables['REFERENCE_DATE_TIME'][:].tobytes().decode('utf-8'),
        '%Y%m%d%H%M%S')
    
    # Cycle through profile numbers to get each profile in turn
    for profile in range(profile_count):
        cycle_number = nc.variables['CYCLE_NUMBER'][profile]
        profile_number = profile + 1
        direction = nc.variables['DIRECTION'][profile].tobytes().decode('utf-8')

        # Extract the timestamp
        timestamp = None
        juld = nc.variables['JULD'][profile]
        if not ma.is_masked(juld):
            timestamp = reference_time + timedelta(days=float(juld))

        # Position
        lon = nc.variables['LONGITUDE'][profile]
        if ma.is_masked(lon):
            lon = None

        lat = nc.variables['LATITUDE'][profile]
        if ma.is_masked(lat):
            lat = None

        # Now extract all the pressures and add records to the coordinates table
        profile_sequence = 0
        for pres in nc.variables['PRES'][profile,:]:
            profile_sequence += 1

            if not ma.is_masked(pres):
                cur.execute(f"""
                    INSERT INTO coordinates
                    (cycle_number, profile_number, direction, profile_sequence, pressure, timestamp, longitude, latitude, source_file)
                    VALUES
                    ({cycle_number}, {profile_number}, '{direction}', {profile_sequence}, {pres}, {int(timestamp.timestamp())}, {lon}, {lat}, '{filename}')""")
                db.commit()
    
    nc.close()

cur.execute("CREATE INDEX coord_idx ON coordinates (cycle_number, profile_number, direction, profile_sequence)")

# Create the Data table
cur.execute("DROP TABLE IF EXISTS data_values")
cur.execute("""CREATE TABLE data_values (
    "variable" TEXT NOT NULL,
    "coordinate" INTEGER NOT NULL,
    "value" REAL NOT NULL,
	"qc" INTEGER NOT NULL
);""")
db.commit()

# Add data from D files
skipped_count = 0
for file in tqdm(d_files, desc='Extract D-file data', disable=not(args.display_progress)):
    nc = Dataset(file, 'r', format='NETCDF4')
    profile_count = nc.dimensions['N_PROF'].size

    for var in d_file_variables:
        var_data = nc.variables[var]
        qc_data = nc.variables[f'{var}_QC']

        for profile in range(profile_count):
            cycle_number = nc.variables['CYCLE_NUMBER'][profile]
            profile_number = profile + 1
            direction = nc.variables['DIRECTION'][profile].tobytes().decode('utf-8')

            profile_data = var_data[profile, :]
            profile_qc = qc_data[profile, :]
            
            for i in range(len(profile_data)):
                if not ma.is_masked(profile_data[i]):
                    try:
                        coord_id = get_coord(cur, cycle_number, profile_number, direction, i + 1)
                        cur.execute("""INSERT INTO data_values (variable, coordinate, value, qc)
                            VALUES (?, ?, ?, ?)""", (var, coord_id, float(profile_data[i]), int(profile_qc[i])))
                        db.commit()
                    except NoCoordinateException as e:
                        skipped_count += 1

    nc.close()

print(f'Skipped {skipped_count} invalid coordinates')

# Add data from BD files
bd_files_pattern = f'BD{args.deployment_id}_*.nc'
bd_files = glob.glob(os.path.join(profiles_dir, bd_files_pattern))

skipped_count = 0
for file in tqdm(bd_files, desc='Extract BD-file data', disable=not(args.display_progress)):
    nc = Dataset(file, 'r', format='NETCDF4')
    profile_count = nc.dimensions['N_PROF'].size
    
    for var in bd_file_variables:
        var_data = nc.variables[var]
        qc_data = nc.variables[f'{var}_QC']

        for profile in range(profile_count):
            cycle_number = nc.variables['CYCLE_NUMBER'][profile]
            profile_number = profile + 1
            direction = nc.variables['DIRECTION'][profile].tobytes().decode('utf-8')

            profile_data = var_data[profile, :]
            profile_qc = qc_data[profile, :]
            
            for i in range(len(profile_data)):
                if not ma.is_masked(profile_data[i]):
                    try:
                        coord_id = get_coord(cur, cycle_number, profile_number, direction, i + 1)
                        cur.execute("""INSERT INTO data_values (variable, coordinate, value, qc)
                            VALUES (?, ?, ?, ?)""", (var, coord_id, float(profile_data[i]), int(profile_qc[i])))
                        db.commit()
                    except NoCoordinateException as e:
                        skipped_count += 1

    nc.close()

print(f'Skipped {skipped_count} invalid coordinates')


# Generate output

# Get the coordinates
output = pd.read_sql_query("""SELECT id AS COORD_ID, cycle_number AS CYCLE_NUMBER, profile_number AS NPROF,
    direction AS DIRECTION, profile_sequence AS NLEVEL, pressure AS PRES, timestamp AS TIMESTAMP,
    longitude AS LONGITUDE, latitude AS LATITUDE, source_file AS SOURCE_FILE FROM coordinates
    ORDER BY CYCLE_NUMBER, DIRECTION, NPROF, NLEVEL""", db, index_col='COORD_ID')

# Now we go through the variables
for var in tqdm(d_file_variables + bd_file_variables, desc='Writing output', disable=not(args.display_progress)):
    var_df = pd.read_sql_query(f"SELECT coordinate, value as {var}, qc as {var}_QC FROM data_values WHERE variable = ?", db,
                              params=[var], index_col='coordinate')

    output = output.join(var_df, how='left')

output.to_csv(f'{args.deployment_id}.csv', index=False)

if args.save_db:
    db_file = f'{args.deployment_id}.sqlite'
    if os.path.exists(db_file):
        os.remove(db_file)

    db_disk = sqlite3.connect(db_file)
    db.backup(db_disk)
    db_disk.close()

# Cleanup
cur.close()
db.close()