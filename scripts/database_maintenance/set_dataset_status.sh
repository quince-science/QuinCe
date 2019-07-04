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

read -r -d '' statuses << EOM
Choose status:
-1   ERROR
 0   Waiting
 1   Data extraction
 2   Data reduction
 3   Automatic QC
 4   Ready for QC
 5   Ready for submission
 6   Waiting for approval
 7   Waiting for automatic export
 8   Automatic export in progress
 9   Automatic export complete
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
new_status=${@:$OPTIND + 1:2}

# Print help and exit
if [ $help -eq 1 ]; then
  echo -e "$usage"
  exit
fi

if [ -z "$dataset_id" ]
then
  mysql -u$db_user -p"$db_password" $db_name << EOF
    SELECT id, name FROM dataset;
EOF
echo ""
fi

while [ -z "$dataset_id" ]
do
  read -p "Dataset ID: " dataset_id
done

if [ -z "$new_status" ]
then
  echo -e "$statuses"
fi

while [ -z "$new_status" ]
do
  read -p "Status: " new_status
done

if [[ "$dataset_id" -eq "$dataset_id" && "$new_status" -eq "$new_status" ]] 2>/dev/null
then
  if [ $verbose -eq 1 ]
  then
    echo "Setting status of dataset ID $dataset_id to $new_status"
  fi

  # Set the status
  mysql -u $db_user -p"$db_password" $db_name <<EOF
    UPDATE dataset SET status = $new_status WHERE id = $dataset_id;
EOF

fi
