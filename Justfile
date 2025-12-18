# Load environment from .env file
set dotenv-load

# Lookup config values from env and $CODEDEFENDERS_CONFIG file.
# Works similarly to env config in Code Defenders, but also
# accepts env variables without the "CODEDEFENDERS_" prefix.
# Returns with exit code 1 if no value is set.
lookup_config := """
function lookup_config {
    snake_case="$(echo "$1" | tr '[.a-z]' '[_A-Z]')"
    for varname in "$snake_case" "CODEDEFENDERS_$snake_case"; do
        if test -n "${!varname:-}"; then echo "${!varname}"; return; fi
    done
    for varname in "CONFIG" "CODEDEFENDERS_CONFIG"; do
        if test -n "${!varname:-}"; then
            value="$(grep "$1" "${!varname:-}" | cut -d'=' -f2)"
            if test -n "$value"; then echo "$value"; return; fi
        fi
    done
    >&2 echo "missing $1"; return 1
}
"""

# List available recipes
default:
    just --list

# Print software paths, versions and variables
versions:
    #!/usr/bin/env bash
    function header { echo; echo "{{YELLOW}}{{BOLD}}$@{{NORMAL}}"; }
    function show_cmd { echo "{{style("command")}}$@{{NORMAL}}" && eval "$@"; }
    function show_var { echo "${1}=\"{{BLUE}}${!1:-}{{NORMAL}}\" {{WHITE}}${2:-}{{NORMAL}}"; }

    header Java
    which java
    show_var JAVA_HOME
    show_cmd java --version

    header Maven
    which mvn
    show_var MAVEN_HOME
    show_var MAVEN_OPTS "(configures CLI options passed to Java)"
    show_var MAVEN_ARGS "(configures CLI options passed to Maven)"
    show_cmd mvn --version

    header docker
    which docker
    show_cmd docker --version

    if test -n "$STANDALONE_TOMCAT_EXECUTABLE"; then
        header "Tomcat (for standalone)"
        echo $(realpath "$STANDALONE_TOMCAT_EXECUTABLE")
        show_var JAVA_OPTS "(configures CLI options passed to Java)"
        show_var CATALINA_OPTS "(configures CLI options passed to Tomcat)"
        show_var STANDALONE_TOMCAT_EXECUTABLE
        show_var STANDALONE_CONTEXT_PATH
        show_var STANDALONE_CODEDEFENDERS_CONFIG
        show_var STANDALONE_HTTP_PORT
        show_var STANDALONE_HTTPS_PORT
        show_var STANDALONE_DEBUG_PORT
        show_var STANDALONE_LOG_LEVEL
        show_cmd "$STANDALONE_TOMCAT_EXECUTABLE" version
    fi

# Clean build directory
clean:
    mvn clean

# Run Maven (with config from .env)
mvn *args:
    mvn {{args}}

# Compile (without running tests)
compile:
    mvn compile -DskipTests

# Run unit tests
unit-test:
    mvn -DskipCheckstyle -DskipFrontend test

# Run database tests
database-test:
    mvn -P it-database-only -DskipCheckstyle -DskipUnitTests -DskipFrontend verify

# Compile and deploy (without running tests)
deploy:
    mvn deploy -DskipTests

# Produce exploded war file (without recompiling or running tests)
explode:
    mvn war:exploded

# Update license headers
update-license:
    mvn license:format

# Run a java class with the project classpath (args cannot include spaces)
java class *args:
    mvn exec:java -Dexec.mainClass="{{class}}" -Dexec.args="{{args}}"

# Run jshell with the project classpath
jshell:
    mvn com.github.johnpoth:jshell-maven-plugin:1.3:run

# Show tree of Java dependencies
dependency-tree:
    mvn dependency:tree

# Show report of Java dependencies
dependency-report:
    mvn dependency:analyze-report
    which xdg-open 2>/dev/null && { find target -name dependency-analysis.html | xargs xdg-open; } || true

# Run docker-compose
[working-directory: "docker"]
docker-compose:
    docker-compose up

# Build docker image
docker-build tag="dev":
    docker build --file ./docker/Dockerfile --tag codedefenders/codedefenders:{{tag}} .

# Generate .zip files for builtin puzzles
[working-directory: "installation/puzzles"]
pack-puzzles:
    ./pack_puzzles.sh

# Drop all tables and views from the database
clean-database:
    #!/usr/bin/env bash
    set -euo pipefail

    {{lookup_config}}
    DB_NAME="$(lookup_config db.name)"
    DB_HOST="$(lookup_config db.host)"
    DB_PORT="$(lookup_config db.port)"
    DB_USERNAME="$(lookup_config db.username)"
    DB_PASSWORD="$(lookup_config db.password)"
    echo "Using database:"
    echo "- Name: {{BLUE}}$DB_NAME{{NORMAL}}"
    echo "- Host: {{BLUE}}$DB_HOST:$DB_PORT{{NORMAL}}"
    echo "- Username: {{BLUE}}$DB_USERNAME{{NORMAL}}"

    function query {
        mysql --host="$DB_HOST" --port="$DB_PORT" --user="$DB_USERNAME" --password="$DB_PASSWORD" --database="$DB_NAME" -s -e "$1"
    }

    tables=($(query "show full tables where table_type <> 'VIEW';" | cut -f1))
    views=($(query "show full tables where table_type = 'VIEW';" | cut -f1))
    if test -z "${tables:-}${views:-}"; then echo "Already empty."; exit; fi

    q="SET FOREIGN_KEY_CHECKS=0;"
    for view in ${views[@]}; do q="$q DROP VIEW IF EXISTS \`$view\`;"; done
    for table in ${tables[@]}; do q="$q DROP TABLE IF EXISTS \`$table\`;"; done
    q="$q SET FOREIGN_KEY_CHECKS=1;"

    echo "{{RED}}About to run the following query:{{NORMAL}}"$'\n'"$q"
    read -p "{{RED}}Continue? [yN]{{NORMAL}} " -n1 answer; echo;
    if [[ "$answer" != "y" ]]; then exit; fi
    query "$q"
    echo "Done."

# Run the mysql REPL on the database
mysql *args:
    #!/usr/bin/env bash
    set -euo pipefail

    {{lookup_config}}
    DB_NAME="$(lookup_config db.name)"
    DB_HOST="$(lookup_config db.host)"
    DB_PORT="$(lookup_config db.port)"
    DB_USERNAME="$(lookup_config db.username)"
    DB_PASSWORD="$(lookup_config db.password)"
    >&2 echo "Using database:"
    >&2 echo "- Name: {{BLUE}}$DB_NAME{{NORMAL}}"
    >&2 echo "- Host: {{BLUE}}$DB_HOST:$DB_PORT{{NORMAL}}"
    >&2 echo "- Username: $DB_USERNAME"

    mysql --host="$DB_HOST" --port="$DB_PORT" --user="$DB_USERNAME" --password="$DB_PASSWORD" --database="$DB_NAME" -s {{args}}

# Remove all files from the data dir (use with sudo if required)
clean-data:
    #!/usr/bin/env bash
    set -euo pipefail

    {{lookup_config}}
    DATA_DIR="$(lookup_config data.dir)"
    echo "Using data dir: {{BLUE}}$DATA_DIR{{NORMAL}}"

    cmd=""
    shopt -s nullglob
    for f in "$DATA_DIR"/*; do
        if test -n "$cmd"; then cmd="$cmd"$'\n'; fi
        cmd="${cmd}rm -rfv '${f}';"
    done
    if test -z "$cmd"; then echo "Already empty."; exit; fi

    echo "{{RED}}About to run the following command:{{NORMAL}}"
    echo "$cmd"
    read -p "{{RED}}Continue? [yN]{{NORMAL}} " -n1 answer; echo;
    if [[ "$answer" != "y" ]]; then exit; fi

    sh -c "$cmd"
    echo "Done."

# Run a standalone tomcat instance on the exploded war file
# This recipe won't re-compile the code or update the context.
# The "compile" and "explode" recipes can be used for this.
# The "explode" recipe will hot-swap assets, tags and JSP files.
[doc('Run a standalone tomcat instance on the exploded war file')]
tomcat-standalone:
    #!/usr/bin/env bash
    set -euo pipefail

    tomcat="$STANDALONE_TOMCAT_EXECUTABLE"
    path="${STANDALONE_CONTEXT_PATH:-}"
    config="$STANDALONE_CODEDEFENDERS_CONFIG"
    http_port="${STANDALONE_HTTP_PORT:-8080}"
    https_port="${STANDALONE_HTTPS_PORT:-8443}"
    debug_port="${STANDALONE_DEBUG_PORT:-8000}"
    log_level="${STANDALONE_LOG_LEVEL:-INFO}"
    tomcat_log_level="${STANDALONE_TOMCAT_LOG_LEVEL:-INFO}"

    # Enable remote debugging if CATALINA_OPTS is unset
    if test -z "${CATALINA_OPTS:-}"; then
        CATALINA_OPTS="-agentlib:jdwp=transport=dt_socket,address=${debug_port},server=y,suspend=n"
    fi
    # Set log level and some basic config if JAVA_OPTS is unset
    if test -z "${JAVA_OPTS:-}"; then
        JAVA_OPTS="-Dorg.slf4j.simpleLogger.defaultLogLevel=${log_level}"
        JAVA_OPTS="$JAVA_OPTS -Dorg.slf4j.simpleLogger.showDateTime=true"
        JAVA_OPTS="$JAVA_OPTS -Dorg.slf4j.simpleLogger.dateTimeFormat=HH:mm:ss"
        JAVA_OPTS="$JAVA_OPTS -Dorg.slf4j.simpleLogger.showShortLogName=true"
    fi

    rm -rf target/tomcat
    mkdir -p target/tomcat/conf/Catalina/localhost
    mkdir -p target/tomcat/logs

    cat << EOF > "target/tomcat/conf/Catalina/localhost/${path}.xml"
    <Context docBase="${PWD}/target/codedefenders" reloadable="true" />
    EOF

    cat << EOF > target/tomcat/conf/web.xml
    <web-app xmlns="http://java.sun.com/xml/ns/javaee"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd"
      version="3.0">
      <servlet>
        <servlet-name>default</servlet-name>
        <servlet-class>org.apache.catalina.servlets.DefaultServlet</servlet-class>
      </servlet>
      <servlet>
        <servlet-name>jsp</servlet-name>
        <servlet-class>org.apache.jasper.servlet.JspServlet</servlet-class>
      </servlet>
      <servlet-mapping>
        <servlet-name>default</servlet-name>
        <url-pattern>/</url-pattern>
      </servlet-mapping>
      <servlet-mapping>
        <servlet-name>jsp</servlet-name>
        <url-pattern>*.jsp</url-pattern>
        <url-pattern>*.jspx</url-pattern>
      </servlet-mapping>
    </web-app>
    EOF

    cat << EOF > target/tomcat/conf/server.xml
    <Server port="8005" shutdown="SHUTDOWN">
      <Listener className="org.apache.catalina.startup.VersionLoggerListener" />
      <Listener className="org.apache.catalina.core.AprLifecycleListener" />
      <Listener className="org.apache.catalina.core.JreMemoryLeakPreventionListener" />
      <Listener className="org.apache.catalina.mbeans.GlobalResourcesLifecycleListener" />
      <Listener className="org.apache.catalina.core.ThreadLocalLeakPreventionListener" />
      <Service name="Catalina">
        <Connector port="${http_port}" protocol="HTTP/1.1"
                   connectionTimeout="20000"
                   redirectPort="${https_port}"
                   maxParameterCount="1000" />
        <Connector port="${https_port}" protocol="org.apache.coyote.http11.Http11NioProtocol"
                 SSLEnabled="true" scheme="https" secure="true">
          <SSLHostConfig>
              <Certificate certificateKeystoreFile="${PWD}/target/tomcat/keystore.jks"
                           certificateKeystorePassword="codedefenders" type="RSA" />
          </SSLHostConfig>
        </Connector>
        <Engine name="Catalina" defaultHost="localhost">
          <Host name="localhost" appBase="webapps"
                unpackWARs="true" autoDeploy="true">
          </Host>
        </Engine>
      </Service>
    </Server>
    EOF

    cat << EOF > target/tomcat/conf/logging.properties
    handlers = java.util.logging.ConsoleHandler
    .handlers = java.util.logging.ConsoleHandler
    java.util.logging.ConsoleHandler.level = $tomcat_log_level
    java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter
    EOF

    # Generated using:
    # keytool -genkey -keyalg RSA -keysize 2048 -validity 100000 -keystore cd.jks && base64 cd.jks
    base64 -d << EOF > target/tomcat/keystore.jks
    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
    EA==
    EOF

    function show_cmd { echo "{{style("command")}}$@{{NORMAL}}" && eval "$@"; }
    function show_export { echo "{{style("command")}}export $1=\"$2\"{{NORMAL}}" && export $1="$2"; }
    show_export CODEDEFENDERS_CONFIG "$config"
    show_export CATALINA_BASE "$(realpath target/tomcat)"
    show_export CATALINA_OPTS "${CATALINA_OPTS:-}"
    show_export JAVA_OPTS "${JAVA_OPTS:-}"
    show_cmd "$tomcat" run
