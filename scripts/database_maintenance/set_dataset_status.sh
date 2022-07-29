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
 10   Data Extraction
 20   Sensor QC
 30   Data Reduction
 40   Data Reduction QC
 50   User QC
100   Ready for Submission
110   Waiting for Approval
120   Waiting for Automatic Export
130   Automatic Export In Progress
140   Automatic Export Complete
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
