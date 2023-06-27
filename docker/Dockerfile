FROM docker.io/library/maven:3.8-openjdk-11 AS build-webapp

COPY ./local-repo ./local-repo/
COPY pom.xml .
COPY package.json .
COPY package-lock.json .
COPY rollup.config.js .
COPY ./.git ./.git/
COPY ./src ./src/
RUN mvn --no-transfer-progress package -DskipTests -DskipCheckstyle


FROM docker.io/library/maven:3.8-openjdk-11 as bundle-libs

RUN mkdir /installation/
COPY ./installation/installation-pom.xml ./installation/

RUN echo 'data.dir=.' >installation/config.properties && \
    mvn --no-transfer-progress -f installation/installation-pom.xml clean package -Dconfig.properties=config.properties


FROM docker.io/library/debian:11-slim AS debian-java-layer

# Install Java
RUN apt-get update && \
    DEBIAN_FRONTEND='noninteractive' apt-get install -y --no-install-recommends openjdk-11-jre-headless && \
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


FROM debian-java-ant-layer AS debian-java-ant-libs-layer

ENV LANG="C.UTF-8"

RUN mkdir /srv/codedefenders

COPY --from=bundle-libs installation/lib /srv/codedefenders-lib/


FROM debian-java-ant-libs-layer AS debian-java-ant-libs-tomcat-layer

# Install TOMCAT and CURL (for the health check)
RUN apt-get update && \
    DEBIAN_FRONTEND='noninteractive' apt-get install -y --no-install-recommends tomcat9 libtcnative-1 curl &&  \
    rm -rf /var/lib/apt/lists/* && \
    rm -rf /tmp/* && \
    rm -f /var/log/alternatives.log /var/log/apt/history.log /var/log/apt/term.log /var/log/dpkg.log && \
    rm -f /etc/machine-id

ENV CATALINA_HOME="/usr/share/tomcat9"
ENV CATALINA_BASE="/var/lib/tomcat9"
ENV CATALINA_TMPDIR="/tmp"
WORKDIR "${CATALINA_BASE}"
ENV CODEDEFENDERS_DATA_DIR="/srv/codedefenders"
ENV CODEDEFENDERS_LIB_DIR="/srv/codedefenders-lib"
ENV CODEDEFENDERS_ANT_HOME="/usr/share/ant"

EXPOSE 8080


FROM debian-java-ant-libs-tomcat-layer AS production

# opencontainers annotations are set in the scripts/ci/build-container-images script

COPY ./docker/docker-entrypoint.sh /usr/local/bin/

COPY --from=build-webapp target/codedefenders /var/lib/tomcat9/webapps/ROOT/

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
