#!/bin/bash

############################################################
#
# This script replaces placeholders in various configuration
# files with values from the files quince.setup.default and
# quince.setup, the second overriding the first if exists.
#
# Usage:
# scripts/setup_replace_strings.sh [-v] [setuptype]
# -v = verbose, say whats going on
# setuptype = if this is set, script will look for a file
# quince.setup.[setuptype] (eg quince.setup.prod).
#
############################################################

# Reset the files we'll be editing
./scripts/setup_reverse_strings.sh

verbose=0
setuptype=''
while getopts "v" opt; do
  case "$opt" in
  v)  verbose=1
    ;;
  esac
done
setuptype=''
inputstring=${@:$OPTIND:1}
if [ -n "$inputstring" ]
then
  setuptype=".$inputstring"
fi

# First get only default file
setup=$(cat quince.setup.default)


setupfile="quince.setup$setuptype"
if [ -e $setupfile ]
then
  # using awk to keep properties in quince.setup.default not in quince.setup
  setup=$(awk -F= '!a[$1]++' $setupfile quince.setup.default)
fi

# If %quince_version% don't exist, append quince version from current git tag
git_tag=$(git describe --tag --abbrev=0)
if [ -z $(echo "$setup" | grep -e '^%quince_version%') ]
then
  setup="$setup
  %quince_version%=$git_tag"
fi


if [ $verbose -eq 1 ]
then
  printf "Using setup file $setupfile\n"
  echo "$setup"
fi

# Remove empty and commented lines
setup=$(echo "$setup"|sed -e '/^#/d' -e '/^\s*$/d')

# Files to do search replace in:
files=\
"WebApp/WebContent/WEB-INF/web.xml
configuration/quince.properties
WebApp/WebContent/META-INF/context.xml"

while read -r l;
do
  key=$(echo "$l" | sed -e 's/^ *\([^ ]*\) *\=.*$/\1/')
  value=$(echo "$l" | sed -e 's/^.*\= *\(.*\)$/\1/' -e 's/[\/&]/\\&/g')

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

    sed -i.bak "s/$key/$value/" "$f"
    rm $f.bak
  done <<< "$files"
done <<< "$setup"

rm NB-quince_is_not_setup 2>/dev/null

# Hide the changed files
./scripts/setup_hide_changes.sh

exit 0
