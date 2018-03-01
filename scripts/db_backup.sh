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

user=$(scripts/get_setup_property.sh username)
if [ $? -gt 0 ]
then
  exit 1
fi
pw=$(scripts/get_setup_property.sh password)
host=$(scripts/get_setup_property.sh host)
port=$(scripts/get_setup_property.sh port)
db=$(scripts/get_setup_property.sh database)
folder=$(scripts/get_setup_property.sh prod_backup_folder)
current_tag=$(git describe --tags)
mkdir -p $folder
# Quit with error if backup already exists
backup_file="$folder/$current_tag.sql.gz"
if [ -e $backup_file ]
then
  >&2 printf "ERROR: Backup file $backup_file already exists\n"
  exit 1
fi
mysqldump --port=$port --user=$user --password="$pw" $db | gzip - \
  >$folder/$current_tag.sql.gz
retval=$?

if [ $verbose -eq 1 ]
then
  if [ $retval -eq 0 ]
  then
    echo "Database $db backed up to $folder/$current_tag.sql.gz"
  else
    >&2 printf "Database backup script failed\n"
  fi
fi

exit $retval
