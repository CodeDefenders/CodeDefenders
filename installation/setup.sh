#!/bin/bash
#
# Copyright (C) 2016-2019 Code Defenders contributors
#
# This file is part of Code Defenders.
#
# Code Defenders is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or (at
# your option) any later version.
#
# Code Defenders is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
#


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

# Create a clean config file that avoids the quirks of using "~"
# Sed on mac is broken, so avoid "in place -i" substituion in favor of rewriting the file
sed "s|\~|${HOME}|g" "$config_file" > "${config_file}".tmp

# Load the properties from the clean file
while read -r NAME VALUE; do
    declare $(echo ${NAME} | sed 's/\./_/g')="${VALUE}"
done < <(sed -e '/^$/d' -e '/^#/d' "${config_file}".tmp | sed 's|=| |')


# FAIL if any of the required configurations is missing

: ${data_dir:?Please provide a value for data.dir in $config_file }

# Not sure this is actually require
: ${ant_home:?Please provide a value for ant.home in $config_file }

: ${db_url:?Please provide a value for db.url in $config_file }
: ${db_username:?Please provide a value for db.username in $config_file }
# Password is optional
#: ${db_password:?Please provide a value for db.password in $config_file }

: ${tomcat_url:?Please provide a value for tomcat.url in $config_file }
: ${tomcat_path:?Please provide a value for tomcat.path in $config_file }
: ${tomcat_username:?Please provide a value for tomcat.username in $config_file }
: ${tomcat_password:?Please provide a value for tomcat.password  in $config_file }

# Check preconditions on software
echo "* Check preconditions on software"

# Can we connect to mysql with given user name and pwd ?
echo "* Check mysql (credentials)"

# THIS IS MOSTLY ILLUSTRATIVE. IT
# extract the protocol
proto="$(echo $db_url | grep :// | sed -e's,^\(.*://\).*,\1,g')"
# remove the protocol
url="$(echo ${db_url/$proto/})"
# extract the user (if any)
user="$(echo $url | grep @ | cut -d@ -f1)"
# extract the host
host="$(echo ${url/$user@/} | cut -d/ -f1)"
# by request - try to extract the port
port="$(echo $host | sed -e 's,^.*:,:,g' -e 's,.*:\([0-9]*\).*,\1,g' -e 's,[^0-9],,g')"

if [[ $host = *":"* ]]; then
  DB_HOST="-h ${host%:$port}"
else
  DB_HOST="-h ${host}"
fi

if [ ! -z "$port" ]; then
  DB_PORT="-P $port"
fi

# extract the path (if any)
path="$(echo $url | grep / | cut -d/ -f2-)"

# Remove query string if ANY. Sed does not support ? operation for optional elements
if [[ $path = *"?"* ]]; then
    DB_NAME="$(echo $path | sed 's,^\(.*\)?.*,\1,')"
else
    DB_NAME="$path"
fi

#DEBUG
#echo "url: $url"
#echo "  proto: $proto"
#echo "  user: $user"
#echo "  host: $host"
#echo "  port: $port"
#echo "  path: $path"
#echo "  DB_NAME: $DB_NAME"
#echo "  DB_HOST: $DB_HOST"
#echo "  DB_PORT: $DB_PORT"

#https://stackoverflow.com/questions/3601515/how-to-check-if-a-variable-is-set-in-bash
if [ -z "${db_password}" ]; then
  DB_PASSWORD="" # Unset db_password
else
  DB_PASSWORD="-p${db_password}"
fi

mysql -u${db_username} ${DB_PASSWORD} ${DB_HOST} ${DB_PORT} -e "SELECT USER(),CURRENT_USER()" > /dev/null 2> /dev/null

echo "* Check mysql (database existence)"

result=$(mysql -u${db_username} ${DB_PASSWORD} ${DB_HOST} ${DB_PORT} -s -N -e "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME='${DB_NAME}'" 2>/dev/null); if [ -z "$result" ]; then echo "Database ${DB_NAME} does not exists. Please create it before running this script."; exit 1; fi

# Can we connect the db? Note that this might fail because the user lacks the proper grants/permission (i.e., INDEX creation)
read -p "* Setting up ${DB_NAME} as code-defenders DB. \
This will delete any previous db named ${DB_NAME} from ${DB_HOST}. \
Continue (y/n)? " choice
case "$choice" in
y|Y ) mysql -u${db_username} -p${db_password} ${DB_HOST} ${DB_PORT} ${DB_NAME} < ../src/main/resources/db/codedefenders.sql;;
n|N ) echo "no";;
* ) echo "invalid. Abort"; exit 1;;
esac


# Do we have Maven and Ant installed ?
echo "* Check Maven"
mvn -version > /dev/null

echo "* Check Ant"
${ant_home}/bin/ant -version > /dev/null

echo "* Create folder structure under $data_dir"

# Create the home folder and checks this worked
# Better safe than sorry.
mkdir -vp "${data_dir}"

if [ ! -d "${data_dir}" ]; then
    echo "ERROR: Cannot create folder ${data_dir}"
    exit 1
fi

for FOLDER in "lib" "sources" "mutants" "tests" "ai"; do
  mkdir -vp "${data_dir}"/${FOLDER}
done

# Currently, this downloads more dependencies than necessary.
# The issue is that jacoco agent comes with a broken manifest otherwise.

echo "* Download dependencies and copy resources"
mvn -f installation-pom.xml clean validate package -Dconfig.properties="${config_file}".tmp > /dev/null

# Do we need to check/set group permissions?
# This might be necessary if tomcat runs with a specifc user
#chgrp -R defender defender/
#chmod -R 770 defender/

echo "* Done"

# Clean up
rm "${config_file}".tmp

exit 0
