#!/bin/bash

config_tomcat_listening_port() {
    sed -i -E -e "$(printf 's|^(.*<Connector port=")[0-9]*(" protocol="HTTP/1.1".*)$|\\1%d\\2|' "$1")" /etc/tomcat10/server.xml
}

main() {
    config_tomcat_listening_port "${CODEDEFENDERS_TOMCAT_LISTENING_PORT:-8080}"
}

main

JAVA_OPTS="-Djava.awt.headless=true -Dorg.apache.catalina.startup.EXIT_ON_INIT_FAILURE=true" exec "$CATALINA_HOME/bin/catalina.sh" run
