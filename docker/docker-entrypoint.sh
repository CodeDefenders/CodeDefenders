#!/bin/bash

gen_tomcat_users() {
    FILE="$1"
    ADMIN_USERNAME="$2"
    ADMIN_ROLE="$3"

    echo '<?xml version="1.0" encoding="UTF-8"?>' > "${FILE}"
    echo '<tomcat-users xmlns="http://tomcat.apache.org/xml"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://tomcat.apache.org/xml tomcat-users.xsd"
              version="1.0">' >> "${FILE}"
    if [ -n "${ADMIN_USERNAME}" ]
    then
        echo "<role rolename=\"${ADMIN_ROLE}\"/>" >> "${FILE}"
        echo "<user username=\"${ADMIN_USERNAME}\" roles=\"${ADMIN_ROLE}\"/>" >> "${FILE}"
    fi
    echo '</tomcat-users>' >> "${FILE}"
}

config_tomcat_listening_port() {
    sed -i -E -e "$(printf 's|^(.*<Connector port=")[0-9]*(" protocol="HTTP/1.1".*)$|\\1%d\\2|' "$1")" /etc/tomcat10/server.xml
}

main() {
    gen_tomcat_users "/etc/tomcat10/tomcat-users.xml" "${CODEDEFENDERS_ADMIN_USERNAME}" "${CODEDEFENDERS_AUTH_ADMIN_ROLE:-"codedefenders-admin"}"
    config_tomcat_listening_port "${CODEDEFENDERS_TOMCAT_LISTENING_PORT:-8080}"
}

main

JAVA_OPTS="-Djava.awt.headless=true -Dorg.apache.catalina.startup.EXIT_ON_INIT_FAILURE=true" exec "$CATALINA_HOME/bin/catalina.sh" run
