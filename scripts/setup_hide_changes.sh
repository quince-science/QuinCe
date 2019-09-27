#!/bin/bash

############################################################
#
# This script hides changes to config files from git.
# Affected files are:
# - quince.properties
# - context.xml
# - web.xml
#
############################################################

git update-index --assume-unchanged      \
  WebApp/WebContent/META-INF/context.xml \
  WebApp/WebContent/WEB-INF/web.xml      \
  configuration/quince.properties        \
  external_scripts/NRT/config.toml \
  external_scripts/export/config_carbon.toml \
  external_scripts/export/config_quince.toml \
  external_scripts/export/config_copernicus.toml

