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
  NRT/config.toml
