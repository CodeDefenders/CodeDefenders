Steps for running the GamMut Web App

-- PREREQUISITES ON LOCAL MACHINE --

You need to have:

Apache Tomcat
Apache Ant

The app also uses:

Google DiffMatchPatch (Java files already included)
Bootstrap (CSS Files already included)
JUnit (Java file already included)

-- RUNNING THE WEB APP ON YOUR LOCAL MACHINE --

To create war file, on the root directory, execute:
$ make

Alternatively:

1. Compile all the Java Code in the GamMut directory into WEB-INF/classes:
$ javac *.java -d WEB-INF/classes

2. Package the project as a web archive:
$ jar cvf gammut.war html WEB-INF

3. Store the gammut.war web archive file in the apache-tomcat-<version no.>\webapps folder

4. Ensure the Tomcat Server is running
(Assuming env variables set up as in tomcat setup guide, command is "%CATALINA_HOME%\bin\startup.bat" on windows)

5. Once the server is running, the system can be accessed at:

localhost:8080/gammut/intro
localhost:8080/gammut/attacker
localhost:8080/gammut/defender
localhost:8080/gammut/scores

-- EXPLANATION OF THE STRUCTURE OF THE PROJECT --

Individual Web Pages:

	The code for each web page spans two places:

	- A Java Servlet in the Gammut directory
	- A Java Server Page in the html directory

	CSS for all webpages is currently in a single CSS file, in the css directory, called "gamestyle"

Information Storage Classes:

	The game information is stored in Java Classes:

	- GameState contains information about the progress of the game (Also storing lists of Mutants and Tests)
	- Mutant contains associated mutant information
	- Test contains information about a particular test

	Mutants and Tests keep track of how many points they have scored themselves.

Mutant and Test Storage in Filesystem:

	Code entered by the user is stored as Java Files in the WEB-INF directory:

	- Mutants contains Java class and source code for all entered mutants (these files are stored in timestamped folders for unique identification of mutants)
	- Tests contains Java class and source code for all entered tests (same as above, timestamped folders)
	- Resources currently contains the playable game levels. Storing matching Java class and source files here allows them to be played by the user
	- Classes contains JUnit, diff_match_patch and the rest of the class files for the project.

Mutation Tester and build.xml File:

The MutationTester file is a Java class that can interact with the Ant Build file for the project, using the command line to execute ant targets. The MutationTester file is in the Gammut folder and the build.xml file is stored in the WEB-INF folder.

The build file has four targets:

	- compile-mutant (Compiles the mutant source file)
	- compile-test (Compiles the test source file)
	- test-original (Runs a test against the original class file)
	- test-mutant (Runs a test against a particular mutant)

	There are no dependencies in these targets: this is handled by the Java code. Code cannot be added to the file system if it doesn't compile properly, and it cannnot be tested against anything if it does not exist in the file system.

The MutationTester class has a private method which passes command line ant targets, and four publicly callable methods which run a specific target using the private method.

If you require any more information about running the project, please contact me at rsharp1@sheffield.ac.uk


