WEBAPP := gammut-1.0-SNAPSHOT
CATALINA_HOME ?= /Users/jmr/lib/tomcat7
ifeq (,$(wildcard ${CATALINA_HOME}))
    $(error Tomcat dir does not exist!)
endif

default: clean compile deploy
compile:
	mvn clean compile package install war:war

deploy:
	${CATALINA_HOME}/bin/shutdown.sh
	cp target/${WEBAPP}.war ${CATALINA_HOME}/webapps
	${CATALINA_HOME}/bin/startup.sh


clean:
	rm -fR ${CATALINA_HOME}/webapps/${WEBAPP}*
	rm -fR ${CATALINA_HOME}/webapps/${WEBAPP}.war
	rm -fR target/*
