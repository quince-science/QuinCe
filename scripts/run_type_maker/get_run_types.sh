#!/bin/bash
mysql -u $1 --password=$2 $3 -A -e"SELECT * FROM run_type WHERE file_definition_id = ${4};"
