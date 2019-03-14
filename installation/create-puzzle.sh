#!/bin/bash

# The script assumes Convention over Configuration, so please try to respect the structure of the folders
mvn -f ../pom.xml clean compile exec:java -Dexec.args="--bundle-directory ./puzzles --configurations ../config.properties"
