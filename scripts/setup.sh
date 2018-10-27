#!/bin/bash
#
# Copyright (C) 2016-2018 Code Defenders contributors
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


# Exit on error
set -e

if [ $# -eq 1 ]; then
  HOME_FOLDER=$1;
else
	echo "Error missing HOME_FOLDER"
	exit 1
fi

echo "Data folder is: $HOME_FOLDER/defender"

tar xf defender.tar.gz -C ${HOME_FOLDER}
#chgrp -R defender defender/
#chmod -R 770 defender/

# TODO: Check if defender group exists
#chgrp groupA ./folderA
#chmod g+rwx  ./folderA



# unzip defender.zip -d ${HOME_FOLDER}
# Extract the defender.zip there

#mkdir -p ${DATA_FOLDER}/lib
#mkdir -p ${DATA_FOLDER}/sources
#mkdir -p ${DATA_FOLDER}/mutants
#mkdir -p ${DATA_FOLDER}/tests
#mkdir -p ${DATA_FOLDER}/ai

# Copy resources
#find ../src/main/webapp -iname "build.xml" -exec cp -v {} ${DATA_FOLDER} \;

## TODO This should be configured with DATA_FOLDER as well. Now it's hardcoded
#find ../src/main/webapp -iname "security.policy" -exec cp -v {} ${DATA_FOLDER} \;

# Libs as well... Assumes that you have this lib in maven local repo !
#for LIB in \
#  junit-4.12.jar \
#  hamcrest-all-1.3.jar  \
#  org.jacoco.ant-0.7.7.201606060606.jar \
#  org.jacoco.core-0.7.7.201606060606.jar \
#  org.jacoco.agent-0.7.7.201606060606.jar \
#  org.jacoco.report-0.7.7.201606060606.jar
#do
#find ~/.m2 -iname ${LIB} -exec cp -v {} ${DATA_FOLDER}/lib \;
#done

