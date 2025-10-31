#!/bin/bash
mysql --user=quince --password=quince quince -e "SET time_zone='+0:00';SELECT id,firstname,surname,FROM_UNIXTIME(last_login/1000) FROM user ORDER BY last_login DESC limit 10"
