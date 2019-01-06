#!/bin/bash
# This script assumes that the CUT, Tests and Mutants are not yet in the file system and in the database, which should be true at installation time

# Each parameter of the script takes a list of values separated by spaces. Names should be self explanatory. In case a required element is missing the code will skip that element. For example, if the CUT is missing, tests and mutants will not be installed.

# The script assumes Convention over Configuration, so please try to respect the structure of the folders
HERE=$(pwd)
cd ..

set -x # Debug

mvn clean compile exec:java -Dexec.args="--cuts $(pwd)/installation/cuts/Branch/Branch.java --mutants $(pwd)/installation/mutants/Branch/0/Branch.java --tests  $(pwd)/installation/tests/Branch/0/TestBranch.java $(pwd)/installation/tests/Branch/1/TestBranch.java --puzzleChapters $(pwd)/installation/puzzleChapters/PuzzleChapter01.properties --puzzles $(pwd)/installation/puzzles/Branch/0/Puzzle.properties $(pwd)/installation/puzzles/Branch/1/Puzzle.properties --configurations $(pwd)/config.properties"

cd $HERE
