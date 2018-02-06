#!/bin/bash

#################################################
#
# Make a backup of the current production
# database using credentials from context.xml
#
# Usage:
# ./database_backup.sh [/path/for/backup/folder]
#
# Default backup path is /var/backup/shared/backups/
#################################################

# Need to be in QuinCe root for this to work
cd ~/QuinCe

path=${1-/var/backup/shared/backups/}
tag=${2-}

username=$(grep -m 1 -o 'username=[\x27"][^\x27"]*[\x27"]' WebApp/WebContent/META-INF/context.xml|sed -e 's/^username=[\x27"]\(.*\)[\x27"]/\1/')

password=$(grep -m 1 -o 'password=[\x27"][^\x27"]*[\x27"]' WebApp/WebContent/META-INF/context.xml|sed -e 's/^password=[\x27"]\(.*\)[\x27"]/\1/')

database=$(grep -m 1 -o 'jdbc\:mysql\:\/\/localhost:3306\/[a-zA-Z0-9_]\+' WebApp/WebContent/META-INF/context.xml|sed -e 's/^.*\/\([^\/]\+\)$/\1/')

mysqldump --user=$username --password=$password $database |gzip -> ${path}/quince${tag}.sql.gz


