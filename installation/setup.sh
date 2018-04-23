#!/bin/bash

set -eE  # same as: `set -o errexit -o errtrace`
trap 'echo "Fail!"' ERR

# Read configuration input from provided config.file
if [ "$#" -lt 1 ]; then
    echo "You must provide a config file. Abort"
    exit 1
else
    config_file=$1
if [ "${config_file:0:1}" = "/" ]; then
# absolute path
echo "Absolute path"
    else
# relative path. Make it absolute
echo "Relative path"
config_file=$(pwd)/$config_file
    fi
fi

if [ ! -f ${config_file} ]; then
    echo "Config file $config_file is missing. Abort"
    exit 1
fi

echo "* Loading configuration from ${config_file}"

while read -r NAME VALUE; do
#    echo "* Found ${NAME} "
    declare $(echo ${NAME} | sed 's/\./_/g')="${VALUE}"
done < <(sed -e '/^$/d' -e '/^#/d' ${config_file} | sed 's|=| |')


# FAIL if any of the required configurations is missing

: ${data_dir:?Please provide a value for data.dir in $config_file }
# Not sure this is actually require
: ${ant_home:?Please provide a value for ant.home in $config_file }

: ${db_url:?Please provide a value for db.url in $config_file }
: ${db_username:?Please provide a value for db.username in $config_file }
: ${db_password:?Please provide a value for db.password in $config_file }

: ${tomcat_url:?Please provide a value for tomcat.url in $config_file }
: ${tomcat_path:?Please provide a value for tomcat.path in $config_file }
: ${tomcat_username:?Please provide a value for tomcat.username in $config_file }
: ${tomcat_password:?Please provide a value for tomcat.password  in $config_file }

# Check preconditions on sowftware
echo "* Check preconditions on software"

# Can we connect to mysql with given user name and pwd ?
echo "* Check mysql (credentials)"

DB_NAME=$(echo "$db_url" | sed 's|^.*jdbc:mysql://localhost:[0-9][0-9]*/\(.*\)?.*|\1|')

mysql -u${db_username} -p${db_password} -e "SELECT USER(),CURRENT_USER()" > /dev/null

# Can we connect the db? Note that this might fail because the use lacks the proper grants/permission (i.e., INDEX creation)
read -p "* Setting up '${DB_NAME}' as code-defenders DB.\
This will delete any previous db named '${DB_NAME}'.\
Continue (y/n)?" choice
case "$choice" in
y|Y ) mysql -u${db_username} -p${db_password} ${DB_NAME} < ../src/main/resources/db/codedefenders.sql;;
n|N ) echo "no";;
* ) echo "invalid. Abort"; exit 1;;
esac


# Do we have mvn adn ant installed ?
echo "* Check maven"
mvn -version > /dev/null

echo "* Check ant"
${ant_home}/bin/ant -version > /dev/null

echo "* Create folder structure under $data_dir"

mkdir -vp ${data_dir}/lib
mkdir -vp ${data_dir}/sources
mkdir -vp ${data_dir}/mutants
mkdir -vp ${data_dir}/tests
mkdir -vp ${data_dir}/ai

# ATM Thid downloads more deps than necessary. The issue might be jacoco agent which comes with a broken manifest otherwise.

echo "* Download dependencies and copy resources"
set -x
mvn -f installation-pom.xml clean validate package -Dconfig.properties=$config_file > /dev/null

# Do we need to check/set group permissions?
# This might be necessary if tomcat runs with a specifc user
#chgrp -R defender defender/
#chmod -R 770 defender/

echo "Done"

exit 0
