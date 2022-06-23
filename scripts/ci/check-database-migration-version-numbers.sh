#!/bin/sh

# This matches SQL migrations (in src/main/resources/db/migrations/)
# and Java migrations (in src/java/org/codedefenders/persistence/database/migrations)
output="$(find src/main/ -path "*/migrations/V*__*.*" | sed -e 's/.*\(V[0-9._]*\)__.*/\1/' | sort | uniq -d)"
if [ -z "$output" ]; then
    echo "No duplicate versions"
    exit 0
else
    echo "There are multiple database migrations with the same version:"
    echo "$output"
    exit 1
fi
