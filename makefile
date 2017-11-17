PROPERTIES=db.url db.username db.password tomcat.username tomcat.password tomcat.url
default:
# After it has been deployed a first time
	mvn clean compile package install war:war tomcat7:redeploy

first:	check-properties
# Deploying for the first time
	mvn clean compile package install war:war tomcat7:deploy

check-properties:
# TODO Somehow this does not fail the build
# Check if the requires properties are specified inside config.properties
	$(foreach p,${PROPERTIES}, \
		test ! -z $$(cat config.properties | grep $p | grep "$p" | sed -e 's|^.*=\(.*\)|\1|') || echo "Missing $p" \
	)
		

#cleanup:
# Revert changes in repository
#	git checkout pom.xml src/main/webapp/META-INF/context.xml

.PHONY : default
