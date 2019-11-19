# Inspired by: https://dzone.com/articles/a-dockerfile-for-maven-based-github-projects

############################################################################################################################
# Checkout the latest commit with GIT AS build.
############################################################################################################################

FROM alpine/git AS git
ARG RELEASE_VERSION
WORKDIR /
RUN git clone https://github.com/CodeDefenders/CodeDefenders.git
WORKDIR /CodeDefenders
# Checkout the right tag
RUN git checkout ${RELEASE_VERSION}
# Collect label values to file since we cannot tag containers from within Docker
RUN  echo "export GIT_COMMIT=$(git rev-parse HEAD)" > .git-data && \
     echo "export GIT_DATE=\"$(git log -1 --format=%cd)\"" >> .git-data

############################################################################################################################
# Build the WAR file with Maven and create the data folder structure
############################################################################################################################

FROM maven:3.5-jdk-8-alpine AS build

COPY --from=git /CodeDefenders /CodeDefenders
WORKDIR /CodeDefenders

# Build Code Defenders for Docker using config.properties@docker
RUN cp config.properties@docker.example config.properties && \
	mvn clean compile package war:war -DskipTests

# Run the file system setup script
RUN cd /CodeDefenders/src/test/resources/systemtests/frontend && \
  ./setup-filesystem.sh /CodeDefenders/config.properties

############################################################################################################################
# Install tomcat on JDK, ant, and copy over codedefenders
############################################################################################################################
FROM tomcat:9.0-jdk8-openjdk AS tomcat

### CUSTOMIZE TOMCAT installation

# Delete existing default tomcat web applications
RUN rm -rf /usr/local/tomcat/webapps/ROOT
RUN rm -rf /usr/local/tomcat/webapps/docs
RUN rm -rf /usr/local/tomcat/webapps/examples
RUN rm -rf /usr/local/tomcat/webapps/host-manager

# Copy template config files
COPY templates /usr/local/tomcat/templates
COPY replaceProperties.py /usr/local/bin

# Install codedefenders in tomcat
COPY --from=build /CodeDefenders/target/codedefenders.war /usr/local/tomcat/webapps/codedefenders.war
COPY --from=build /CodeDefenders/src/main/webapp/META-INF/context.xml /usr/local/tomcat/templates/codedefenders-context.xml
COPY webxml.patch /usr/local/tomcat/webapps
RUN cd /usr/local/tomcat/webapps \
  && unzip codedefenders.war -d codedefenders \
  && git apply webxml.patch \
  && rm webxml.patch \
  && cp codedefenders/WEB-INF/web.xml ../templates/codedefenders-web.xml \
  && rm -f codedefenders.war

# Copy the data directory from build
COPY --from=build /codedefenders/ /codedefenders/

# Install ANT
RUN apt-get update && apt-get install -y \
	ant


COPY --from=git /CodeDefenders/.git-data /.git-data

LABEL maintainer="alessio.gambi@uni-passau.de"
#LABEL codedefenders.git.commit=$GIT_COMMIT
#LABEL codedefenders.release.date=$RELEASE_DATE

EXPOSE 8080

COPY docker-entrypoint.sh /usr/local/bin/
ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["catalina.sh", "run"]
