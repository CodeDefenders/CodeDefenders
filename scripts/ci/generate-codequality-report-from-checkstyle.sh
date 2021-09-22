#!/bin/bash

function mapSeverity() {
    severity="$1"

    if [ "$severity" == "error" ]; then
        echo "critical"
    elif [ "$severity" == "warning" ]; then
        echo "major"
    elif [ "$severity" == "info" ]; then
        echo "minor"
    else
        echo "info"
    fi
}

function err() {
    printf "%s\n" "$*" >&2
}

file=""
first=1
output="checkstyle-report.json"
path="$(pwd)"
echo "["
tail -n +3 $1 | while read p; do
    if echo "$p" | grep -q "<file name.*"; then
        file=$(expr "$p" : '<file name=\"\(.*\)\".*' | sed 's@'"$path"'@@g')
        err Processing checkstyle results for "$file"
    fi
    if echo "$p" | grep -q "<error.*"; then
        line="$(expr "$p" : '.*line=\"\([0-9]*\)\".*')"
        message="$(expr "$p" : '.*message=\"\(.*\)\" source.*' | sed -e 's/&apos;/`/g' -e 's/&lt;/</g' -e 's/&gt;/>/g' -e 's/&quot;/\\\"/g' -e 's/&amp;/\&/g')"
        severityCheckstyle="$(expr "$p" : '.*severity=\"\(.*\)\" message.*')"
        severity=$(mapSeverity $severityCheckstyle)
        checksum=$(echo "$file $line $message" | sha1sum | awk '{print $1}')
        if [ "$first" == 1 ]; then
            echo "{ \"description\": \"$message\", \"severity\": \"$severity\", \"fingerprint\": \"$checksum\", \"location\": { \"path\": \"$file\", \"lines\": { \"begin\": \"$line\" } } }"
            first=0
        else
            echo ",{ \"description\": \"$message\", \"severity\": \"$severity\", \"fingerprint\": \"$checksum\", \"location\": { \"path\": \"$file\", \"lines\": { \"begin\": \"$line\" } } }"
        fi
    fi
done
echo "]"
