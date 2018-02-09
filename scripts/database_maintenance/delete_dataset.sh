#!/bin/bash

read -p "Database Name (quince_dev): " db_name
if [ -z "$db_name" ]
	then db_name="quince_dev"
fi


read -p "Database User (quince_dev): " db_user
if [ -z "$db_user" ]
	then db_user="quince_dev"
fi

while [ -z "$dataset_id" ]
do
	read -p "Dataset ID: " dataset_id
done


mysql -u $db_user -p $db_name <<EOF
	DELETE FROM equilibrator_pco2 WHERE measurement_id IN (SELECT id FROM dataset_data WHERE dataset_id = $dataset_id);
	DELETE FROM dataset_data WHERE dataset_id = $dataset_id;
	DELETE FROM calibration_data WHERE dataset_id = $dataset_id;
	DELETE FROM dataset WHERE id = $dataset_id;
EOF
