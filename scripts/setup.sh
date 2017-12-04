#!/bin/bash

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
chgrp -R defender defender/
chmod -R 770 defender/

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

