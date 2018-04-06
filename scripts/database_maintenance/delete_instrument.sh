#!/bin/bash

# This script deletes a data set and all related records from the database
# It is designed to be run from a gradle task
#
# The script takes the database url, username and password as parameters
# It will prompt the user for the ID of the data set to be deleted

# Usage:
# delete_dataset.sh db_name db_user db_password dataset_id

db_name=$1
db_user=$2
db_password=$3
instrument_id=$4

if [ -z $instrument_id ]
then
  echo "Missing instrument ID"
  echo "Run gradle task as: gradle delete_instrument -PinstrumentId=<id>"
  exit 1
fi

mysql --user=$db_user --password=$db_password $db_name <<EOF
  DELETE FROM equilibrator_pco2 WHERE measurement_id IN (SELECT id FROM dataset_data WHERE dataset_id IN (SELECT id FROM dataset WHERE instrument_id = $instrument_id));
  DELETE FROM dataset_data WHERE dataset_id IN (SELECT id FROM dataset WHERE instrument_id = $instrument_id);
  DELETE FROM calibration_data WHERE dataset_id IN (SELECT id FROM dataset WHERE instrument_id = $instrument_id);
  DELETE FROM dataset WHERE instrument_id = $instrument_id;

  DELETE FROM calibration WHERE instrument_id = $instrument_id;

  DELETE FROM data_file WHERE file_definition_id IN (SELECT id FROM file_definition WHERE instrument_id = $instrument_id);
  DELETE FROM run_type WHERE file_definition_id IN (SELECT id FROM file_definition WHERE instrument_id = $instrument_id);
  DELETE FROM file_column WHERE file_definition_id IN (SELECT id FROM file_definition WHERE instrument_id = $instrument_id);
  DELETE FROM file_definition WHERE instrument_id = $instrument_id;

  DELETE FROM instrument WHERE id = $instrument_id;
EOF

echo "NOTE: Files HAVE NOT been removed from the file store."
