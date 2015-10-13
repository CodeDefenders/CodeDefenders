DOC_NAME:=main
GEN:=generated_files
FIG:=figures

default: *.java
	mkdir -p WEB-INF/classes
	javac *.java -d WEB-INF/classes
	jar cvf gammut.war html WEB-INF

clean:
	rm -f WEB-INF/classes
