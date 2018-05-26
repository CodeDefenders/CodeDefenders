FROM codedefenders/tomcat:9

# Delete existing default applications
RUN rm -rf /usr/local/tomcat/webapps/ROOT
RUN rm -rf /usr/local/tomcat/webapps/docs
RUN rm -rf /usr/local/tomcat/webapps/examples
RUN rm -rf /usr/local/tomcat/webapps/host-manager

# Install ANT
RUN apt-get update && apt-get install -y \
	ant

# Setup the directory structure for CD by copying it from the host
COPY codedefenders/ /codedefenders/
RUN ls -la /codedefenders/*

# Define admin roles in tomcat
COPY tomcat-users.xml /usr/local/tomcat/conf/tomcat-users.xml

# Enable remove access to admin
COPY context.xml /usr/local/tomcat/webapps/manager/META-INF/context.xml
