#!/bin/sh

output="$(find src/main/resources/db -name "V*" | sed -e 's/.*\(V[0-9._]*\)__.*/\1/' | sort | uniq -d)"
if [ -z "$output" ]; then
    echo "No duplicate versions"
    exit 0
else
    echo "There are multiple database migrations with the same version:"
    echo "$output"
    exit 1
fi
