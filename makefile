WARFILE:=gammut.war

default: *.java compile deploy
compile:
	mkdir -p WEB-INF/classes
	javac -cp ${CLASSPATH}:${CATALINA_HOME}/lib/servlet-api.jar:lib/diffutils-1.2.1.jar *.java -d WEB-INF/classes
	jar cvf ${WARFILE} html WEB-INF
deploy:
	cp ${WARFILE} ${CATALINA_HOME}/webapps
	${CATALINA_HOME}/bin/startup.sh
clean:
	rm -fR WEB-INF/classes
	rm -f ${WARFILE}
