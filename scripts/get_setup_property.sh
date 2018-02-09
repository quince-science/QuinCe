#!/bin/bash

###############################################################
#
# This scripts gets a property from the quince setup
#
# Usage: ./get_setup_property.sh <propertyname>
#
# ...where propertyname can be one of username, password,
# database, filestore.
#
###############################################################

if [ -f NB-quince_is_not_setup ]
then
  >&2 printf "NB! QuinCe is not setup.\n"
  >&2 cat NB-quince_is_not_setup
  exit 1
fi

propertyvalue=''

if [ $1 = 'username' ]
then
  propertyvalue=$(grep -m 1 -o 'username=[\x27"][^\x27"]*[\x27"]' WebApp/WebContent/META-INF/context.xml|sed -e 's/^username=[\x27"]\(.*\)[\x27"]/\1/')

elif [ $1 = 'password' ]
then
  propertyvalue=$(grep -m 1 -o 'password=[\x27"][^\x27"]*[\x27"]' WebApp/WebContent/META-INF/context.xml|sed -e 's/^password=[\x27"]\(.*\)[\x27"]/\1/')

elif [ $1 = 'database' ]
then
  propertyvalue=$(grep -m 1 -o 'jdbc\:mysql\:\/\/localhost:3306\/[a-zA-Z0-9_]\+' WebApp/WebContent/META-INF/context.xml|sed -e 's:.*/\([^/]*\)$:\1:')
elif [ $1 = 'filestore' ]
then
  propertyvalue=$(sed -n 's/^filestore *= *\(.*\)$/\1/p' configuration/quince.properties)
fi

echo $propertyvalue
