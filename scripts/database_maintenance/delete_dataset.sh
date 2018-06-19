#!/bin/bash

################################################################################
#
# This script deletes a data set and all related records from the database
#
# The script takes the database url, username and password as parameters
# It will prompt the user for the ID of the data set to be deleted
#
################################################################################

read -r -d '' usage << EOM
Usage:
delete_dataset.sh [-v] [-h] [dataset_id]
-v Verbose output
-h Print help message and exit
dataset_id Database dataset id

Dataset id will be prompted for if not present in arguments
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

dataset_id=${@:$OPTIND:1}

# Print help and exit
if [ $help -eq 1 ]; then
  echo -e "$usage"
  exit
fi

while [ -z "$dataset_id" ]
do
  read -p "Dataset ID: " dataset_id
done

if [ "$dataset_id" -eq "$dataset_id" ] 2>/dev/null
then
  if [ $verbose -eq 1 ]; then
    echo "Delete all data related to dataset ID $dataset_id from database"
  fi
  # Retrieve the instrument id before delering the dataset data
  instrument_id=$(mysql -u $db_user -p"$db_password" $db_name -N -B \
      -e "select instrument_id from dataset where id=$dataset_id limit 1")

  # Delete dataset data
  mysql -u $db_user -p"$db_password" $db_name <<EOF
    DELETE FROM equilibrator_pco2 WHERE measurement_id IN (SELECT id FROM dataset_data WHERE dataset_id = $dataset_id);
    DELETE FROM diagnostic_data WHERE measurement_id IN (SELECT id FROM dataset_data WHERE dataset_id = $dataset_id);
    DELETE FROM dataset_data WHERE dataset_id = $dataset_id;
    DELETE FROM calibration_data WHERE dataset_id = $dataset_id;
    DELETE FROM dataset WHERE id = $dataset_id;
EOF

fi
