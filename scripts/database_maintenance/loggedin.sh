#!/bin/bash
db_name="$(scripts/get_setup_property.sh db_database)"
db_user="$(scripts/get_setup_property.sh db_username)"
db_password="$(scripts/get_setup_property.sh db_password)"

mysql --user="$db_user" --password="$db_password" "$db_name" -e "SET time_zone='+0:00';SELECT id,firstname,surname,FROM_UNIXTIME(last_login/1000) FROM user ORDER BY last_login DESC limit 10"
