# Code Defenders

This is Code Defenders, a mutation testing game.

## Requirements

- Tomcat Server v7 or later
- Ant (default executable `/usr/local/bin/ant`, set environment variable `ANT_HOME`)
- Maven
- MySQL

## Build
Building code-defenders requires a `config.properties` file to be in the root of the project. This file contains all the configuration and properties that are required to build, install, and deploy code-defenders.

Maven does a basic check that the `config.properties` is there, and fails the build otherwise.

During the build, maven will output the value of some of those variables for you to check if they are correct.

Since the `config.properties` file contains also your passwords, please **do not commit** it to the repo.

In case you want to provide a file with a different name (let's say `config.test`), you can add the following option to your maven command: `-Dconfig.properties=config.test` 

Having the `config.properties` ready and a running tomcat instance we can move on to deploy it (see Deployment).

## Installation and Setup

To install code-defenders invoke the `setup.sh` script under the `installation folder` passing the `config.properties` as input:

```bash
cd installation
./setup.sh ../config.properties
```

In particular, installing code-defenders requires these variables to have a value:

```bash
# The main code-defenders folder, e.g., /var/lib/codefenders
data.dir=...

# Location of ant command
ant.home=...

# MySQL Database URL and credential to access it
db.url=...
db.username=...
db.password=...
```

The script performs a basic check on the availability of the required software, creates the database, creates the folder structure, and download all the required dependencies and files. If any of this installation step fails, all the installation process fails.

### Set up code-defenders admin users
Code-defenders relies on Tomcat authentication system to identify admin users, that is, users which can access the protected pages of the application. If a user try to reach one of such pages, the browser (not the code-defenders) will ask for the username and password and tomcat (not the code-defenders) will check those. The standard way to configure Tomcat security is to edit the `$CATALINA_HOME/conf/tomcat-users.xml` and add your credentials, e.g., `<MY_ADMIN_USER>` and `<MY_ADMIN_PWD>`:

```xml
<role rolename="manager-gui"/>  
<user username="<MY_ADMIN_USER>" password="<MY_ADMIN_PWD>" roles="manager-gui"/>
```

## Deployment

### Tomcat admin user

Add manager-script role and user to `$CATALINA_HOME/conf/tomcat-users.xml` (`$CATALINA_HOME` should be set to your Tomcat installation root directory):

```xml
<role rolename="manager-script"/>  
<user username="adminscript" password="adm3b5eM3JG" roles="manager-script"/>  
```

The same values must be present inside the `config.properties` file.

  
### Deploy first time

Tomcat and MySQL passwords must be provided inside the `config.properties` file to deploy code-defenders, and both tomcat and MySQL must be running and reacheable.

To deploy code-defenders the _first time_, invoke the following command:

```bash
mvn clean compile package install war:war tomcat7:deploy -DskipTests
```

### Redeploy
To _redeploy_ the application instead use the following command:

```bash
mvn clean compile package install war:war tomcat7:redeploy -DskipTests
```


<!--, either by editing `makefile` or by passing them as arguments:

```bash
make first [TOMCAT_PASSWORD=... MYSQL_PASSWORD=...]
```
### Redeploy

To compile and _redeploy_:

```bash
make
```
-->

<!--## Database

To create the database, execute `src/main/resources/db/codedefenders.sql`:

```bash
mysql -u [username] -p
> source src/main/resources/db/codedefenders.sql;
```
-->
<!--## Data Storage

Classes, tests and mutants are stored in `/var/lib/codedefenders/`, these directories must exist:

```bash
mkdir -p /var/lib/codedefenders/sources /var/lib/codedefenders/tests /var/lib/codedefenders/mutants
```

The tomcat user (and possibly the user running Code Defenders) must have full permissions on this directory.

Major and Evosuite must be stored in their respective folders within this directory (codedefenders/major and codedefenders/evosuite).
-->

<!--## Integration testing
Before running integration tests, which are activated by using the maven IT profile (-PIT) you need to set up the test resources. Otherwise, the enforcer plugin will break your build.

You can setup the resources by copying the required libraries inside the following folder:
```
src/test/resources/itests/lib/
```

The required libraries are listed inside the following file: ```src/test/resources/itests/lib/libraries.list```
 
Otherwise, you can runn the following script (Mac/Linux and default maven installation) which looks for those libraries in your local maven repository and copies then in the right folder:

```bash
mvn -U clean compile test-compile
cd src/test/resources/itests/lib
for F in $(cat libraries.list); do
  find ~/.m2 -iname "${F}" -exec cp {} . \;
done
cd -
```
Once these libraries are in place you can run the integration tests (note the war:war in the command):

```mvn compile test-compile war:war test -PIT```
-->

## IntelliJ Project

- Import Maven project from existing sources
- Configure Tomcat server
  - Preferences -> Build, Execution, Deployment -> Application Servers -> Add Tomcat Server
- Configure Artifact (as Web Application: Exploded), it must include:
  - WEB-INF/classes/[module compile output]
  - WEB-INF/lib/[all maven dependencies]
  - \`resources\` directory contents
  - \`webapps\` directory contents
- Add Run/Debug Configuration
  - Run -> Edit Configurations... -> Add New Tomcat Server configuration -> Add \`Build artifact\` in \`Before launch\` panel and check On Update action: Redeploy. -> OK

## Public URL

<http://code-defenders.org>
