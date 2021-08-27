#!/bin/bash

############################################################
#
# This script reverses changes made to config files by the
# setup_replace_strings.sh - script. Currently it just
# checks out the original files. Any changes to these files
# will thus be overwritten.
#
############################################################

echo "Run scripts/setup_replace_strings.sh to apply setup "\
     "in either quince.setup.default or quince.setup" >NB-quince_is_not_setup

git checkout WebApp/WebContent/META-INF/context.xml \
  WebApp/WebContent/WEB-INF/web.xml \
  configuration/quince.properties
