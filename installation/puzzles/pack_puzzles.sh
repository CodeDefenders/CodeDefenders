#!/bin/bash

set -euo pipefail
IFS=$'\n\t'
shopt -s extglob

if [[ "$(basename "$PWD")" != "puzzles" ]]; then
    >&2 echo This script must be executed from within the puzzle directory.
    exit 1
fi

for chapter in !(*.zip|*.sh); do (
    cd "$chapter"

    rm -f "../${chapter}.zip"
    zip -r "../${chapter}" !(*.zip)

    for puzzle in !(*.zip|*.properties); do (
        cd "$puzzle"

        rm -f "../${puzzle}.zip"
        zip -r "../${puzzle}" *
    ) done
) done
