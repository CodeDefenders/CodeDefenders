TOMCAT_PASSWORD=password
MYSQL_PASSWORD=password

default:
# After it has been deployed a first time
	mvn clean compile package install war:war tomcat7:redeploy

first:	passwords
# Deploying for the first time
	mvn clean compile package install war:war tomcat7:deploy

passwords:	cleanup
# Write passwords where needed
	@test $(TOMCAT_PASSWORD) || echo "ERROR: missing TOMCAT_PASSWORD=..."
	@test $(MYSQL_PASSWORD) || echo "ERROR: missing MYSQL_PASSWORD=..."
	sed -i.orig 's/\*\*\*REMOVED\*\*\*/${TOMCAT_PASSWORD}/g' pom.xml
	sed -i.orig 's/\*\*\*REMOVED\*\*\*/${MYSQL_PASSWORD}/g' src/main/webapp/META-INF/context.xml

cleanup:
# Revert changes in repository
	git checkout pom.xml src/main/webapp/META-INF/context.xml

.PHONY : default
