#!/bin/bash

##############################################
# This script simply runs the flyway upgrade
# gradle plugin. Might be extended to run
# more migration-tasks later.
##############################################

verbose=0

while getopts "v" opt; do
  case "$opt" in
  v)  verbose=1
    ;;
  esac
done

./gradlew flywayMigrate
retval=$?
if [ $verbose -eq 1 ]
then
  if [ $retval -eq 0 ]
  then
    echo "Pending batabase migrations conpleted successfully"
  else
    >&2 printf "Database migration script failed\n"
  fi
fi
exit $retval
