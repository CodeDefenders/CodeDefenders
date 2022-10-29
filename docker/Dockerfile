FROM docker.io/library/maven:3.8-openjdk-11 AS build
RUN mkdir /Codedefenders/
WORKDIR /Codedefenders/
COPY ./local-repo ./local-repo/
COPY pom.xml .
# Download dependencies before copying the source code as the dependencies don't change that often
RUN mvn --no-transfer-progress dependency:go-offline
COPY ./.git ./.git/
COPY ./src ./src/
COPY package.json .
COPY package-lock.json .
COPY rollup.config.js .
RUN mvn --no-transfer-progress package -DskipTests -DskipCheckstyle


FROM docker.io/library/debian:11 AS tomcat

ENV LANG="C.UTF-8"

RUN mkdir /srv/codedefenders

# Install TOMCAT, ANT, MAVEN and CURL (for the health check)
RUN apt-get update &&  \
    DEBIAN_FRONTEND='noninteractive' apt-get install -y tomcat9 &&  \
    apt-get install -y --no-install-recommends curl ant ant-optional maven && \
    rm -rf /var/lib/apt/lists/*

COPY ./installation/installation-pom.xml /
COPY ./docker/docker-entrypoint.sh /usr/local/bin/

COPY --from=build /Codedefenders/target/codedefenders /var/lib/tomcat9/webapps/ROOT/

ENV CATALINA_HOME="/usr/share/tomcat9"
ENV CATALINA_BASE="/var/lib/tomcat9"
ENV CATALINA_TMPDIR="/tmp"
WORKDIR "${CATALINA_BASE}"
ENV CODEDEFENDERS_DATA_DIR="/srv/codedefenders"
ENV CODEDEFENDERS_ANT_HOME="/usr/share/ant"

EXPOSE 8080

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
