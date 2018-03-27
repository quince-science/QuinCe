#!/bin/bash

read -p "Database Name (quince_dev): " db_name
if [ -z "$db_name" ]
  then db_name="quince_dev"
fi


read -p "Database User (quince_dev): " db_user
if [ -z "$db_user" ]
  then db_user="quince_dev"
fi

while [ -z "$instrument_id" ]
do
  read -p "Instrument ID: " instrument_id
done


mysql -u $db_user -p $db_name <<EOF
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
