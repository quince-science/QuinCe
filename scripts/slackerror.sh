#!/bin/bash

############################################################
#
# This script restores the full production environment
# to the test server:
# - Run db_restore_from_prod.sh
# - git fetch && git checkout master
# - gradle clean build test
# - Alert to slack on errors
# - Start apache with new codebase
#
############################################################
OPTIND=1
input_file=""

while getopts "v:f:" opt; do
  case "$opt" in
  f)  input_file=$OPTARG
    ;;
  esac
done

# Escape
json_escape () {
  # Use php json_encode to escape text for json output
  printf '%s' "$1" | php -r 'echo json_encode(file_get_contents("php://stdin"));'
}


url=$(./scripts/get_setup_property.sh slack_app_url)

# Either data from input
message=$(json_escape "$1")

# or read from file
if [ -n $input_file ]
then
  message="$(cat $input_file)"
  message=$(json_escape "$message")
fi

json="{\"text\":$message}"

curl -s -X POST -H 'Content-type: application/json' --data "$json" $url
