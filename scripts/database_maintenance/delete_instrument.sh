#!/bin/bash

################################################################################
#
# This script deletes a data set and all related records from the database
# It is designed to be run from a gradle task
#
# The script takes the database url, username and password as parameters
# It will prompt the user for the ID of the data set to be deleted
#
#
# The script will prompt for dataset id if not set on the command line.
#
################################################################################
read -r -d '' usage << EOM
Usage:
delete_instrument.sh [-v] [-h] [instrument_id]
-v Verbose output
-h Print help message and exit
instrument_id Database instrument id

Instrument id will be prompted for if not present in arguments
EOM

db_name="$(scripts/get_setup_property.sh db_database)"
db_user="$(scripts/get_setup_property.sh db_username)"
db_password="$(scripts/get_setup_property.sh db_password)"
filestore_folder="$(scripts/get_setup_property.sh filestore_folder)"




verbose=0
help=0
while getopts "vh" opt; do
  case "$opt" in
  v)  verbose=1
    ;;
  h)  help=1
    ;;
  esac
done

mysql -u$db_user -p"$db_password" $db_name << EOF
  SELECT id, name FROM instrument;
EOF
echo ""

instrument_id=${@:$OPTIND:1}

# Print help and exit
if [ $help -eq 1 ]; then
  echo -e "$usage"
  exit
fi

while [ -z "$instrument_id" ]
do
  read -p "Instrument ID: " instrument_id
done

# Make sure instrument_id is an integer
if [ "$instrument_id" -eq "$instrument_id" ] 2>/dev/null
then
  if [ $verbose -eq 1 ]; then
    echo "Delete all data related to instrument ID $instrument_id from database"
  fi
  mysql -u$db_user -p"$db_password" $db_name <<EOF
    DELETE FROM data_reduction WHERE measurement_id IN (SELECT id FROM measurements WHERE dataset_id IN (SELECT id FROM dataset WHERE instrument_id = $instrument_id));
    DELETE FROM measurement_values WHERE measurement_id IN (SELECT id FROM measurements WHERE dataset_id IN (SELECT id FROM dataset WHERE instrument_id = $instrument_id));
    DELETE FROM measurements WHERE dataset_id IN (SELECT id FROM dataset WHERE instrument_id = $instrument_id);

    DELETE FROM sensor_values WHERE dataset_id IN (SELECT id FROM dataset WHERE instrument_id = $instrument_id);

    DELETE FROM dataset WHERE instrument_id = $instrument_id;

    DELETE FROM calibration WHERE instrument_id = $instrument_id;

    DELETE FROM data_file WHERE file_definition_id IN (SELECT id FROM file_definition WHERE instrument_id = $instrument_id);
    DELETE FROM run_type WHERE file_definition_id IN (SELECT id FROM file_definition WHERE instrument_id = $instrument_id);
    DELETE FROM file_column WHERE file_definition_id IN (SELECT id FROM file_definition WHERE instrument_id = $instrument_id);
    DELETE FROM file_definition WHERE instrument_id = $instrument_id;

    DELETE FROM instrument_variables WHERE instrument_id = $instrument_id;
    DELETE FROM instrument WHERE id = $instrument_id;
EOF
  # Also delete files from the file store:
  if [ -d "$filestore_folder" ] && [ -d "$filestore_folder/$instrument_id" ]
  then
    if [ $verbose -eq 1 ]; then
      echo "Deleting filestore data"
    fi
    rm "$filestore_folder/$instrument_id/"* 2>/dev/null
    rmdir "$filestore_folder/$instrument_id"
  fi
else
    echo "Folder $filestore_folder/$instrument_id dont exist"
    echo -e "$usage"
    exit 1
fi
