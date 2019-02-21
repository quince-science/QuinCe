#!/bin/sh

######################################################
# This hook runs spotlessCheck before commit
######################################################

./gradlew spotlessCheck
./gradlew migrationCheck
