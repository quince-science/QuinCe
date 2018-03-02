#!/bin/bash

###############################################################
#
# This scripts gets a property from the quince setup
#
# Usage: ./get_setup_property.sh <propertyname> [<setuptype>]
#
# ...where propertyname can be one of username, password,
# database, filestore.
#
# Fetches values from: quince.setup.default overridden by quince.setup[.other]
#
###############################################################

if grep -q "%db_username%" "WebApp/WebContent/META-INF/context.xml"
then
  >&2 printf "NB! QuinCe is not setup.\n"
  >&2 cat NB-quince_is_not_setup
  exit 1
fi

setuptype=''
if [ -n "$2" ]
then
  setuptype=".$2"
fi

# First get only default file
setup=$(cat quince.setup.default)

setupfile="quince.setup$setuptype"
if [ -e $setupfile ]
then
  # using awk to keep properties in quince.setup.default not in quince.setup
  setup=$(awk -F= '!a[$1]++' $setupfile quince.setup.default)
fi
propertyvalue=$(sed -n "s/^%$1% *= *\(.*\) *$/\1/p" <<< "$setup" )

echo $propertyvalue
if [ -z "$propertyvalue" ]
then
  exit 1
fi
