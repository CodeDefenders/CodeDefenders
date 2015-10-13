DOC_NAME:=main
GEN:=generated_files
FIG:=figures
WARFILE:=gammut.war

default: *.java
	mkdir -p WEB-INF/classes
	javac *.java -d WEB-INF/classes
	jar cvf ${WARFILE} html WEB-INF
	cp ${WARFILE} ${CATALINA_HOME}/webapps
	${CATALINA_HOME}/bin/startup.sh
clean:
	rm -fR WEB-INF/classes
	rm -f ${WARFILE}
