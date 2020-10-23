FROM maven:3.6-jdk-8 AS build
RUN mkdir /Codedefenders/
WORKDIR /Codedefenders/
COPY pom.xml .
COPY ./local-repo ./local-repo/
COPY ./src ./src/
RUN mvn package -DskipTests -Dcheckstyle.skip


FROM tomcat:9.0-jdk8-openjdk AS tomcat

RUN mkdir /srv/codedefenders

# Install ANT and MAVEN
RUN apt-get update && apt-get install -y ant maven

COPY ./installation/installation-pom.xml .
COPY ./docker/docker-entrypoint.sh /usr/local/bin/

COPY --from=build /Codedefenders/target/codedefenders /usr/local/tomcat/webapps/ROOT/

EXPOSE 8080

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
