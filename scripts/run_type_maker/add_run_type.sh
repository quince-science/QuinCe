#!/bin/bash
mysql -u $1 --password=$2 $3 -A -e"INSERT INTO run_type(file_definition_id, run_name, category_code) VALUES (${4}, '${5}', '${6}');"
