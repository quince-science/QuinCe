#!/bin/bash

##############################################
# This script simply runs the flyway upgrade
# gradle plugin. Might be extended to run
# more migration-tasks later.
##############################################

./gradlew flywayMigrate
