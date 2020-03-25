#!/bin/bash

############################################################
#
# This script undoes changes done by setup_hide_changes,
# so that changes to config files are visible to git.
#
############################################################

git update-index --no-assume-unchanged   \
  WebApp/WebContent/META-INF/context.xml \
  WebApp/WebContent/WEB-INF/web.xml      \
  configuration/quince.properties        \
  external_scripts/NRT/config.toml \
  external_scripts/export/config_carbon.toml \
  external_scripts/export/config.toml \
  external_scripts/export/config_copernicus.toml
