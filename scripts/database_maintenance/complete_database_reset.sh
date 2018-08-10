#####################################################
# This scripts drops the current tables in the
# database, and restores a new blank database.
#
# This is done by dropping all tables, and then
# running the upgrade script, whick runs all
# database migration tasks.
#
# The filestore folder is also completely emptied.
#####################################################


db_name="$(scripts/get_setup_property.sh db_database)"
db_user="$(scripts/get_setup_property.sh db_username)"
db_password="$(scripts/get_setup_property.sh db_password)"
filestore_folder="$(scripts/get_setup_property.sh filestore_folder)"

cat << INTRO

 #######################################################
 #  This script drops all tables in the database, and  #
 #  files in the filestore. A new, empty database is   #
 #  then initialized.                                  #
 #######################################################


INTRO

scripts/db_backup.sh -v

# Prompt before dropping tables from database
echo ""
echo ""
read -p " Drop all tables in $db_name? (y/N)" yn
echo ""
echo ""
case $yn in
  [Yy]* ) ;;
  * ) echo " Exit script"; exit;;
esac


tables=$(mysql -u$db_user -p$db_password \
    $db_name -B -N -e 'show tables')

for table in $tables; do
  mysql -u$db_user -p$db_password $db_name -e \
  "SET FOREIGN_KEY_CHECKS=0;drop table $table;
      SET FOREIGN_KEY_CHECKS=1;"
done

# Prompt before deleting files from filestore
read -p " Delete all files under $filestore_folder? (y/N)" yn
echo ""
echo ""
case $yn in
  [Yy]* ) ;;
  * ) echo " Exit script"; exit;;
esac

rm $filestore_folder/*/*
rmdir $filestore_folder/*

scripts/upgrade.sh
