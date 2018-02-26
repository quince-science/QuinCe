#!/bin/bash

############################################################
#
# This script restores the full production environment
# to the test server:
# - Run db_restore_from_prod.sh
# - git fetch && git checkout master
# - gradle clean build test
# - Alert to slack on errors
# - Start apache with new codebase
#
############################################################

# Restore database
ssh_user=${@:$OPTIND:1}

scripts/db_restore_from_prod.sh $ssh_user || \
  exit 1;

branch=$(scripts/get_setup_property.sh git_test_branch)

# reset setup
scripts/setup_reverse_strings.sh
scripts/setup_show_changes.sh

# Checkout latest updates
git fetch
git checkout $branch
git reset --hard
git submodule update --init

# setup
scripts/setup_replace_strings.sh
scripts/setup_hide_changes.sh

####################
# gradle tasks
###################

# Stop tomcat
./gradlew appStop
tmpfile=/tmp/gradle_build_test_output.txt

# Clean, test and build the war file. Failing tests are sent to the QuinCe-QC
# slack #errors channel
./gradlew clean test war > $tmpfile 2>&1 || scripts/slackerror.sh -f $tmpfile

# Start tomcat with nohup to allow cron to exit while tomcat keeps running
nohup ./gradlew appStartWar &
