# Code Defenders

This is Code Defenders, a mutation testing game. Publicly available at [code-defenders.org](<http://code-defenders.org>).

# Installation

## Software Requirements

- Tomcat Server (Version 7 or later)
- Apache Maven
- Apache Ant
- MySQL (e.g. [MariaDB](https://mariadb.org/))

## Installation and Setup
### Configuration file
Code Defenders requires a `config.properties` file for initial setup, building and deployment. All necessary configuration properties are listed. The file needs to be in the project root directory.

`config.properties` can hold confidential configuration data, so **please** do not include it into the repository.

Following properties are required:
```bash
# The main Code Defenders folder. E.g. /var/lib/codefenders
data.dir=...

# Location of Ant command
ant.home=...

# MySQL database URL and credential to access it
db.url=...
db.username=...
db.password=...

# Tomcat credentials with <manager-script> role.
tomcat.username=...
tomcat.password=...
# Deployment URL ,looks like http://<domain>:<port>/manager/text
tomcat.url=...
# Path to Tomcat executable
tomcat.path=...
```

### Database
`config.properties` requires a URL to an existing database. The database needs to be created before installation.

### Installation script

To install Code Defenders automatically, execute the `setup.sh` script under the `installation` folder passing the `config.properties` file as input. Depending on the chosen data directory, root access may be required to create folders.

```bash
cd installation
./setup.sh ../config.properties
```

The script performs a basic availability check of required software. The data directory folder structure and database schema are created Required dependencies and files are downloaded.

If any installation steps fail, the installation process aborts and prints an error message.

### Tomcat User Management
For deployment, Tomcat requires a user with `manager-script` role, which be be configured in `$CATALINA_HOME/conf/tomcat-users.xml` (CATALINA_HOME is the Tomcat installation directory).
```xml
<role rolename="manager-script"/>
<user username="<MY_USER>" password="<MY_USER_PASSWORD>" roles="manager-script"/>
```

### Set up Code Defenders admin users
Code Defenders relies on the Tomcat authentication system to identify admin users, who can access protected pages). Access control is enforced through Tomcat using Basic Authentication in the browser.
Adding a tomcat admin can be done by applying the `manager-gui` role to a user.
```xml
<role rolename="manager-gui"/>
<user username="<MY_ADMIN_USER>" password="<MY_ADMIN_PWD>" roles="manager-gui"/>
```

## Build and Deployment

For successful deployment, both Tomcat and MySQL services must be running.
Code Defenders is built and deployed with Maven using the following commands.

### Deploy first time

To deploy Code Defenders the _first time_, execute:

```bash
mvn clean compile package install war:war tomcat7:deploy -DskipTests
```

### Redeploy
To _redeploy_ instead use:

```bash
mvn clean compile package install war:war tomcat7:redeploy -DskipTests
```

# Project Import

Code Defenders and most of its dependencies is organised and can be imported into IDEs (IntelliJ) using Maven.

<!--
TODO: Do we really need this?
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
-->
