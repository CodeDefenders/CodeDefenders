FROM docker.io/library/maven:3.9-eclipse-temurin-17 AS build-webapp

COPY ./local-repo ./local-repo/
COPY pom.xml .
COPY package.json .
COPY package-lock.json .
COPY rollup.config.js .
COPY ./.git ./.git/
COPY ./src ./src/
RUN mvn --no-transfer-progress package -DskipTests -DskipCheckstyle


FROM docker.io/library/debian:12-slim AS debian-java-layer

# Install Java
RUN apt-get update && \
    DEBIAN_FRONTEND='noninteractive' apt-get install -y --no-install-recommends openjdk-17-jre-headless && \
    rm -rf /var/lib/apt/lists/* && \
    rm -rf /var/cache/apt/* && \
    rm -rf /tmp/* && \
    rm -f /var/log/alternatives.log /var/log/apt/history.log /var/log/apt/term.log /var/log/dpkg.log


FROM debian-java-layer AS debian-java-ant-layer

# Install ANT
RUN apt-get update && \
    DEBIAN_FRONTEND='noninteractive' apt-get install -y --no-install-recommends ant ant-optional && \
    rm -rf /var/lib/apt/lists/* && \
    rm -rf /var/cache/apt/* && \
    rm -rf /tmp/* && \
    rm -f /var/log/alternatives.log /var/log/apt/history.log /var/log/apt/term.log /var/log/dpkg.log


FROM debian-java-ant-layer AS debian-java-ant-tomcat-layer

# Install TOMCAT and CURL (for the health check)
RUN apt-get update && \
    DEBIAN_FRONTEND='noninteractive' apt-get install -y --no-install-recommends tomcat10 libtcnative-1 curl &&  \
    rm -rf /var/lib/apt/lists/* && \
    rm -rf /tmp/* && \
    rm -f /var/log/alternatives.log /var/log/apt/history.log /var/log/apt/term.log /var/log/dpkg.log && \
    rm -f /etc/machine-id

ENV CATALINA_HOME="/usr/share/tomcat10"
ENV CATALINA_BASE="/var/lib/tomcat10"
ENV CATALINA_TMPDIR="/tmp"
WORKDIR "${CATALINA_BASE}"
ENV CODEDEFENDERS_DATA_DIR="/srv/codedefenders"
ENV CODEDEFENDERS_ANT_HOME="/usr/share/ant"

EXPOSE 8080


FROM debian-java-ant-layer AS integrationtest

# Install MAVEN
RUN apt-get update && \
    DEBIAN_FRONTEND='noninteractive' apt-get install -y --no-install-recommends maven && \
    rm -rf /var/lib/apt/lists/*


FROM debian-java-ant-tomcat-layer AS systemtest

# Install maven tomcat10-admin separately so the image layers before are shared with the production image
RUN apt-get update && \
    DEBIAN_FRONTEND='noninteractive' apt-get install -y tomcat10-admin maven && \
    rm -rf /var/lib/apt/lists/* && \
    rm -rf /tmp/* && \
    rm -f /var/log/alternatives.log /var/log/apt/history.log /var/log/apt/term.log /var/log/dpkg.log

RUN printf '<?xml version="1.0" encoding="UTF-8"?>\n<tomcat-users xmlns="http://tomcat.apache.org/xml"\n    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\n    xsi:schemaLocation="http://tomcat.apache.org/xml/tomcat-users.xsd"\n    version="1.0">\n<role rolename="manager-script"/>\n<role rolename="manager-gui"/>\n<role rolename="codedefenders-admin"/>\n<user username="%s" password="%s" roles="manager-script, manager-gui, codedefenders-admin"/>\n</tomcat-users>\n' "admin" "admin" > /etc/tomcat10/tomcat-users.xml

ENTRYPOINT ["/bin/sh", "-c", "JAVA_OPTS=\"-Djava.awt.headless=true -Dorg.apache.catalina.startup.EXIT_ON_INIT_FAILURE=true\" exec \"$CATALINA_HOME/bin/catalina.sh\" run"]


FROM debian-java-ant-tomcat-layer AS production

# opencontainers annotations are set in the scripts/ci/build-container-images script

COPY ./docker/docker-entrypoint.sh /usr/local/bin/

COPY --from=build-webapp target/codedefenders /var/lib/tomcat10/webapps/ROOT/

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
