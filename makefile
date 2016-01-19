# after it has been deployed a first time
default:
	mvn clean compile package install war:war tomcat7:redeploy

# deploying for the first time
first:
	mvn clean compile package install war:war tomcat7:deploy
