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

# Create a clean config file that avoids the quirks of using "~"
# Sed on mac is broken, so avoid "in place -i" substituion in favor of rewriting the file
sed "s|\~|${HOME}|g" "$config_file" > "${config_file}".tmp

# Load the properties from the clean file
while read -r NAME VALUE; do
    declare $(echo ${NAME} | sed 's/\./_/g')="${VALUE}"
done < <(sed -e '/^$/d' -e '/^#/d' "${config_file}".tmp | sed 's|=| |')


: ${data_dir:?Please provide a value for data.dir in $config_file }

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

# This is tricky but necessary: we need to replace ./codedefenders with /codedefenders inside
#echo "WARN Please manually update the $(find ${data_dir} -iname security.policy) file ... "
echo "Update security.policy file"
mv "${data_dir}"/security.policy "${data_dir}"/security.policy.tmp
sed -e 's|\./codedefenders/|/codedefenders/|' "${data_dir}"/security.policy.tmp > "${data_dir}"/security.policy
rm "${data_dir}"/security.policy.tmp

echo "* Done"



# Clean up
rm "${config_file}".tmp

exit 0
