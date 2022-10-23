#!/bin/bash

gen_tomcat_users() {
    FILE="$1"
    ADMIN_USERNAME="$2"
    ADMIN_PASSWORD="$3"

    echo '<?xml version="1.0" encoding="UTF-8"?>' > "$FILE"
    echo '<tomcat-users xmlns="http://tomcat.apache.org/xml"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://tomcat.apache.org/xml tomcat-users.xsd"
              version="1.0">' >> "$FILE"
    if [ -n "$ADMIN_USERNAME" ] && [ -n "$ADMIN_PASSWORD" ]
    then
        echo '<role rolename="manager-gui"/>' >> "$FILE"
        echo "<user username=\"$ADMIN_USERNAME\" password=\"$ADMIN_PASSWORD\" roles=\"manager-gui\"/>" >> "$FILE"
    fi
    echo '</tomcat-users>' >> "$FILE"
}

# TODO: Remove if execution dependency management is handled by CodeDefenders (GitLab #746)
download_dependencies() {
    DATA_DIR="$1"

    mkdir -vp "$DATA_DIR/lib"

    mvn --no-transfer-progress -f /installation-pom.xml clean package -Dconfig.properties=<(echo "data.dir=$DATA_DIR")
}

main() {
    gen_tomcat_users "/etc/tomcat9/tomcat-users.xml" "$CODEDEFENDERS_ADMIN_USERNAME" "$CODEDEFENDERS_ADMIN_PASSWORD"
    download_dependencies "/srv/codedefenders"
}

main

JAVA_OPTS="-Djava.awt.headless=true -Dorg.apache.catalina.startup.EXIT_ON_INIT_FAILURE=true" exec "$CATALINA_HOME/bin/catalina.sh" run
