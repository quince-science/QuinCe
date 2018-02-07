#!/bin/bash

############################################################
#
# This script replaces placeholders in the files
# - quince.properties
# - context.xml
# - web.xml
# with values from the files quince.setup.default and
# quince.setup, the second overriding the first if exists.
#
# Usage:
# scripts/setup_replace_strings.sh [-v] [setuptype]
# -v = verbose, say whats going on
# setuptype = if this is set, script will look for a file
# quince.setup.[setuptype] (eg quince.setup.prod).
#
############################################################


verbose=0
setuptype=''
while getopts "v" opt; do
  case "$opt" in
  v)  verbose=1
    ;;
  esac
done

setuptype=${@:$OPTIND:1}
setupfile=quince.setup.default

if [ -e quince.setup ]
then
  setupfile=quince.setup
fi

if [ -e "quince.setup.$setuptype" ]
then
  setupfile="quince.setup.$setuptype"
fi

setup=$(cat $setupfile)

if [ $verbose -eq 1 ]
then
  printf "Using setup file $setupfile\n"
  echo "$setup"
fi

# Remove empty and commented lines
setup=$(echo "$setup"|sed -e '/^#/d' -e '/^\s*$/d')

# Files to do search repace in:
files=\
"WebApp/WebContent/WEB-INF/web.xml
configuration/quince.properties
WebApp/WebContent/META-INF/context.xml"

while read -r l;
do
  key=$(echo "$l" | sed -e 's/^ *\([^ ]*\) *\=.*$/\1/')
  value=$(echo "$l" | sed -e 's/^.*\= *\([^ ]*\) *$/\1/' -e 's/[\/&]/\\&/g')

  if [ $verbose -eq 1 ]
  then
    printf "Replacing "$(echo $key|sed 's/%/%%/g')" with $value in\n"
  fi


  while read -r f;
  do
    if [ $verbose -eq 1 ]
    then
      printf "    $f\n"
    fi

    sed -i "s/$key/$value/" "$f"
  done <<< "$files"
done <<< "$setup"

rm NB-quince_is_not_setup 2>/dev/null

