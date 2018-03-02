#!/bin/bash

######################################################################
#
# This script backs up the current database using credentials from
# the quince setup.
#
######################################################################

verbose=0

while getopts "v" opt; do
  case "$opt" in
  v)  verbose=1
    ;;
  esac
done

user=$(scripts/get_setup_property.sh db_username)
if [ $? -gt 0 ]
then
  exit 1
fi
pw=$(scripts/get_setup_property.sh db_password)
host=$(scripts/get_setup_property.sh db_host)
port=$(scripts/get_setup_property.sh db_port)
db=$(scripts/get_setup_property.sh db_database)
folder=$(scripts/get_setup_property.sh prod_backup_folder)
current_tag=$(git describe --tags)
mkdir -p $folder
# Quit with error if backup already exists
backup_file="$folder/$current_tag.sql"
if [ -e "${backup_file}.gz" ]
then
  >&2 printf "ERROR: Backup file ${backup_file}.gz already exists\n"
  exit 1
fi
mysqldump --port=$port --user=$user --password="$pw" $db >$backup_file
retval=$?
if [ $retval -eq 0 ]
then
  gzip $backup_file
  if [ $verbose -eq 1 ]
  then
    echo "Database $db backed up to ${backup_file}.gz"
  fi
else
  >&2 printf "Database backup script failed\n"
fi

exit $retval
