#!/bin/bash

################################################################################
#
# This script deletes a data set and all related records from the database
#
# The script takes the database url, username and password as parameters
# It will prompt the user for the ID of the data set to be deleted
#
################################################################################

db_name="$(scripts/get_setup_property.sh db_database)"
db_user="$(scripts/get_setup_property.sh db_username)"
db_password="$(scripts/get_setup_property.sh db_password)"
filestore_folder="$(scripts/get_setup_property.sh filestore_folder)"

filename=$1

mysql -u $db_user -p"$db_password" $db_name <<EOF
  SELECT i.id, i.platform_name, i.name FROM instrument i
    INNER JOIN file_definition fd ON fd.instrument_id = i.id
    INNER JOIN data_file df ON df.file_definition_id = fd.id
    WHERE df.filename = '${filename}';
EOF
