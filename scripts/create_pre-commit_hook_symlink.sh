#!/bin/bash

##########################################################
# This simple script adds the project pre-commit hook
# to the users .git/hooks - folder. The script warns
# about trailing whitespaces and other code style issues
##########################################################

if [ -f .git/hooks/pre-commit ]
then
  echo "File .git/hooks/pre-commit already exists. Exiting."
  exit 1
else
  echo "Creating symlink for pre-commit hook"
  ln -s ../../scripts/pre_commit_checks.sh .git/hooks/pre-commit
fi
