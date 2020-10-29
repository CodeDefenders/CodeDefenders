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


: ${tomcat_url:?Please provide a value for tomcat.url in $config_file }
: ${tomcat_path:?Please provide a value for tomcat.path in $config_file }
: ${tomcat_username:?Please provide a value for tomcat.username in $config_file }
: ${tomcat_password:?Please provide a value for tomcat.password  in $config_file }

# Check preconditions on software
echo "* Check preconditions on software"

# Do we have Maven and Ant installed ?
echo "* Check Maven"
mvn -version > /dev/null


echo "* Create folder structure under $data_dir"

# Create the home folder and checks this worked

if [ -e ${data_dir} ]; then
    read -p "* Data DIR ${data_dir} exists. Do you want to wipe it out? Continue (y/n)? " choice
    case "$choice" in
        y|Y ) rm -rfv  ${data_dir};;
        n|N ) echo "no";;
        * ) echo "Invalid. Abort"; exit 1;;
    esac
fi

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
