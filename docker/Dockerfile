FROM maven:3.8-openjdk-11 AS build
RUN mkdir /Codedefenders/
WORKDIR /Codedefenders/
COPY ./local-repo ./local-repo/
COPY pom.xml .
# Download dependencies before copying the source code as the dependencies don't change that often
RUN mvn dependency:go-offline
COPY ./src ./src/
RUN mvn package -DskipTests -DskipCheckstyle


FROM tomcat:9.0-jdk11-openjdk AS tomcat

RUN mkdir /srv/codedefenders

# Install ANT and MAVEN
RUN apt-get update && apt-get install -y ant maven

COPY ./installation/installation-pom.xml .
COPY ./docker/docker-entrypoint.sh /usr/local/bin/

COPY --from=build /Codedefenders/target/codedefenders /usr/local/tomcat/webapps/ROOT/

EXPOSE 8080

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
