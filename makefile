# after it has been deployed a first time
TOMCAT_PASSWORD=***REMOVED***
MYSQL_PASSWORD=***REMOVED***
EMAIL_PASSWORD=***REMOVED***
default:
	mvn clean compile package install war:war tomcat7:redeploy

# deploying for the first time
first:
	mvn clean compile package install war:war tomcat7:deploy

# write passwords where needed
passwords:
	sed -i.orig 's/\*\*\*REMOVED\*\*\*/${TOMCAT_PASSWORD}/g' pom.xml
	sed -i.orig 's/\*\*\*REMOVED\*\*\*/${MYSQL_PASSWORD}/g' src/main/webapp/META-INF/context.xml
	sed -i.orig 's/\*\*\*REMOVED\*\*\*/${EMAIL_PASSWORD}/g' src/main/webapp/WEB-INF/web.xml
