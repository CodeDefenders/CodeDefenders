#!/bin/bash

function mapSeverity() {
    severity="$1"

    if [ "$severity" == "error" ]; then
        printf "critical"
    elif [ "$severity" == "warning" ]; then
        printf "major"
    elif [ "$severity" == "info" ]; then
        printf "minor"
    else
        printf "info"
    fi
}

function err() {
    printf "%s\n" "$*" >&2
}

file=""
first=1
path="$(pwd)"
printf "[\n"
tail -n +3 $1 | while read p; do
    if printf "$p" | grep -q "<file name.*" -; then
        file="$(realpath --relative-to "${PWD}" "$(expr "$p" : '<file name=\"\(.*\)\".*')")"
        err Processing checkstyle results for "$file"
    fi
    if printf "$p" | grep -q "<error.*" -; then
        line="$(expr "$p" : '.*line=\"\([0-9]*\)\".*')"
        message="$(expr "$p" : '.*message=\"\(.*\)\" source.*' | sed -e 's/&apos;/`/g' -e 's/&lt;/</g' -e 's/&gt;/>/g' -e 's/&quot;/\\\"/g' -e 's/&amp;/\&/g')"
        severityCheckstyle="$(expr "$p" : '.*severity=\"\(.*\)\" message.*')"
        severity=$(mapSeverity "$severityCheckstyle")
        checksum=$(printf "%s %s %s\n" "$file" "$line" "$message" | sha1sum | awk '{print $1}')
        if [ "$first" == 1 ]; then
            printf '{ "description": "%s", "severity": "%s", "fingerprint": "%s", "location": { "path": "%s", "lines": { "begin": %d } } }\n' "$message" "$severity" "$checksum" "$file" "$line"
            first=0
        else
            printf ',{ "description": "%s", "severity": "%s", "fingerprint": "%s", "location": { "path": "%s", "lines": { "begin": %d } } }\n' "$message" "$severity" "$checksum" "$file" "$line"
        fi
    fi
done
printf "]\n"
