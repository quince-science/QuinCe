#!/bin/bash

######################################################################
#
# This script deploys the current branch on the production server
#
######################################################################

# setup using quince.setup
scripts/setup_reverse_strings.sh
scripts/setup_replace_strings.sh
if [ $? -gt 0 ]
then
  >&2 printf "ERROR: Failed setting up quince.\n"
  exit 1
fi

# Reset to the currently set
branch=$(scripts/get_setup_property.sh git_deploy_branch)
echo ""
echo "     QuinCe deployment script"
echo " --------------------------------"
echo ""
echo " Get ready to deploy QuinCe to the production server,"
echo " please revisit your settings in quince.settings"
echo " The current deploy branch is:"
echo ""
echo " ****** $branch ******"
echo ""
echo " The branch will be completely reset to the remote origin."
echo ""
echo ""

read -p " Continue with branch $branch? (y/N)" yn
echo ""
echo ""
case $yn in
  [Yy]* ) ;;
  * ) echo "Exit deployment"; exit;;
esac

git submodule init && git submodule update && git fetch && git reset --hard $branch
if [ $? -gt 0 ]
then
  >&2 printf "ERROR: git reset failed. Aborting\n"
  exit 1
fi
echo ""
echo ""

# This is reset by git reset, and must be run again:
scripts/setup_replace_strings.sh
if [ $? -gt 0 ]
then
  >&2 printf "ERROR: Failed setting up quince after branch reset.\n"
  exit 1
fi

# Make a database backup
scripts/db_backup.sh -v
if [ $? -gt 0 ]
then
  exit 1
fi
echo ""
echo ""

read -p " Run database upgrades? (y/N)" yn
echo ""
echo ""
case $yn in
  [Yy]* ) ;;
  * ) echo "Exit deployment"; exit;;
esac
scripts/upgrade.sh -v
if [ $? -gt 0 ]
then
  >&2 printf "ERROR: Failed upgrading.\n"
  exit 1
fi
echo ""
echo ""

echo "Build output .war - file"
./gradlew war
if [ $? -gt 0 ]
then
  >&2 printf "ERROR: Build application war file failed.\n"
  exit 1
fi
echo ""
echo ""

echo "Run project unit tests"
./gradlew test
if [ $? -gt 0 ]
then
  >&2 printf "ERROR: Application unit testing failed.\n"
  exit 1
fi
echo ""
echo ""

echo "Backup currently deployed .war file"
# backup previous war file:
deploy_folder="$(scripts/get_setup_property.sh prod_deploy_folder)"
backup_folder="$(scripts/get_setup_property.sh prod_backup_folder)"
tag=$(git tag|tail -1)
timestamp=$(date +%Y%m%d%H%M%S)
mkdir -p $deploy_folder

cp $deploy_folder/ROOT.war $backup_folder/$tag.$timestamp.ROOT.war
if [ $? -gt 0 ]
then
  >&2 printf "ERROR: Backup of .war file failed.\n"
  exit 1
fi
echo "Current .war - file backed up to $backup_folder/$tag.$timestamp.ROOT.war"
echo ""
echo ""

read -p " Complete deployment of new .war file? (y/N)" yn
echo ""
echo ""
case $yn in
  [Yy]* ) ;;
  * ) echo "Exit deployment"; exit;;
esac
cp build/libs/QuinCe.war $deploy_folder/ROOT.war
if [ $? -gt 0 ]
then
  >&2 printf "ERROR: Failed copy quince.war to $deploy_folder/ROOT.war\n"
  exit 1
fi
url="$(scripts/get_setup_property.sh 'app.urlstub')"
wget $url
echo "QuinCe was successfully deployed to $deploy_folder/ROOT.war."
echo "The new version will be available as soon as tomcat reloads"
echo "Please verify the new release at $url"
