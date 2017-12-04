# Code Defenders

This is Code Defenders, a mutation testing game.

## Requirements

- Tomcat Server v7 or later
- Ant (default executable `/usr/local/bin/ant`, set environment variable `ANT_HOME`)
- Maven
- MySQL

## Database

To create the database, execute `src/main/resources/db/codedefenders.sql`:

```bash
mysql -u [username] -p
> source src/main/resources/db/codedefenders.sql;
```

## Data Storage

Classes, tests and mutants are stored in `/var/lib/codedefenders/`, these directories must exist:

```bash
mkdir -p /var/lib/codedefenders/sources /var/lib/codedefenders/tests /var/lib/codedefenders/mutants
```

The tomcat user (and possibly the user running Code Defenders) must have full permissions on this directory.

Major and Evosuite must be stored in their respective folders within this directory (codedefenders/major and codedefenders/evosuite).

## Build
Building codedefenders now requires a config.properties file to be in the root of the project. This file contains all the properties that are required to deploy and run codedefenders.

Maven enforces this rule, so if the config.properties is not there, the build will fail.

Therefore, create a config.properties file using the provided config.properties.template. Properties name are self-explanatory.

During the build, maven will output the value of those variables for you to check if they have the correct value.

Since the config.properties contains also your passwords, please **do not commit** it to the repo.

In case you want to provide a file with a different name (let's say config.test), you can add the following option to your maven command: `-Dconfig.properties=config.test` 

## Integration testing
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

## Deployment

### Tomcat admin user

Add manager-script role and user to `$CATALINA_HOME/conf/tomcat-users.xml` (`$CATALINA_HOME` should be set to your Tomcat installation root directory):

```xml
<role rolename="manager-script"/>  
<user username="adminscript" password="adm3b5eM3JG" roles="manager-script"/>  
```
  
### Deploy first time

Tomcat and MySQL passwords must be provided to compile and deploy _the first time_, either by editing `makefile` or by passing them as arguments:

```bash
make first [TOMCAT_PASSWORD=... MYSQL_PASSWORD=...]
```
### Redeploy

To compile and _redeploy_:

```bash
make
```

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
