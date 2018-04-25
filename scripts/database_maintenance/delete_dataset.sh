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
dataset_id=$4

if [ -z $dataset_id ]
then
  echo "Missing database ID"
  echo "Run gradle task as: gradle delete_dataset -PdatasetId=<id>"
  exit 1
fi

mysql --user=$db_user --password=$db_password $db_name <<EOF
  DELETE FROM equilibrator_pco2 WHERE measurement_id IN (SELECT id FROM dataset_data WHERE dataset_id = $dataset_id);
  DELETE FROM dataset_data WHERE dataset_id = $dataset_id;
  DELETE FROM calibration_data WHERE dataset_id = $dataset_id;
  DELETE FROM dataset WHERE id = $dataset_id;
EOF
