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

To install Code Defenders automatically, execute the `setup.sh` script under the `installation` folder passing the `config.properties` file as input. 

```bash
cd installation
./setup.sh ../config.properties
```

The script performs a basic availability check of required software. The data directory folder structure and database schema are created. All the required dependencies and files are automatically downloaded.

If any installation step fails, the installation process aborts and prints an error message.

**Note:** Depending on the chosen data directory and Tomcat installation in place, root access may be necessary to create required folders. Similarly, additional configurations might be needed. For example, if Tomcat runs under a different user, data directory accesses and ownership might be need to be adjusted.

**Note:** Code Defenders also requires that its MySQL user owns specific privileges to create databases and tables. Additionally, it requires INDEX privileges, otherwise the installation fails with an error message similar to:

```ERROR 1142 (42000) at line 183: INDEX command denied to user```


### Tomcat User Management
For deployment, Tomcat requires a user with `manager-script` role, which be be configured in `$CATALINA_HOME/conf/tomcat-users.xml` (`CATALINA_HOME` is the Tomcat installation directory).

```xml
<role rolename="manager-script"/>
<user username="<MY_USER>" password="<MY_USER_PASSWORD>" roles="manager-script"/>
```

### Set up Code Defenders admin users
Code Defenders relies on the Tomcat authentication system to identify admin users, who can access protected pages and customize Code Defenders settings.
Access control is enforced through Tomcat using Basic Authentication in the browser.
Adding a tomcat admin can be done by applying the `manager-gui` role to a user.

```xml
<role rolename="manager-gui"/>
<user username="<MY_ADMIN_USER>" password="<MY_ADMIN_PWD>" roles="manager-gui"/>
```

All system configuration and privileged features are accessible for admin users under the `/admin` page. Configurations are organized in three groups:

* Game management: Create bulk games for an entire class, distribute students among the games, and assign roles to students.
* User management: Check and update users settings, and forcefully reset passwords.
* System settings: Customize technical aspects of Code Defenders and include several advanced settings.


## Build and Deployment

For successful deployment, both Tomcat and MySQL services must be running.
Code Defenders is built and deployed with Maven using the following commands.

### Deploy first time

To deploy Code Defenders the _first time_, execute:

```bash
mvn clean compile package install war:war tomcat7:deploy -DskipTests
```

### Redeploy
To redeploy instead use:

```bash
mvn clean compile package install war:war tomcat7:redeploy -DskipTests
```

### System Tests
System tests work by deploying code-defenders inside disposable Docker containers and interacting with it by means of Selenium.

We use Docker containers which are not (yet) registered in the public repository, so we need to manually build them. Once those are in place, system tests can be run from maven as follows (using the ST -System Test- profile):

```bash
mvn clean integration-test -PST
```

This rebuilds and repackages the application using the `config.properties@docker` file. Then, it copies the resulting `.war` file in the right folder. Finally, it runs all the tests which are annotated with `@Category(SystemTest.class)`. Each test starts two docker instances: one for the backend and on one from the front-end. Next, it starts Selenium. And, finally, it dispose all the containers. Data inside the containers are lost.

#### Build codedefenders/frontend:latest

Assuming that you have `docker` and `docker-compose` installed.

```bash
cd src/test/resources/systemtests/tomcat8.5-jdk8
docker build -t codedefenders/tomcat:8.5 .
cd -
cd src/test/resources/systemtests/frontend
./setup-filesystem.sh ./config.properties
docker build -t codedefenders/frontend .
```

#### Manually deploy the system using docker-compose
If you want to try out code-defenders on docker, assuming you have build the right images. Run the following commands:

```bash
mvn clean compile package war:war -PST
cd  src/test/resources/systemtests/
docker-compose up
```

You should see the outputs of both containers on the console. Since the database in build on the fly, it takes more time to start mysql than usual. Tomcat retries to connect to the database several time. Tomcat receives a random port when it starts, so we need to get it from docker.

```bash
docker ps
```

Should produce an output similar to:

```bash
CONTAINER ID        IMAGE                           COMMAND                  CREATED             STATUS              PORTS                     NAMES
5a4893783257        codedefenders/frontend:latest   "catalina.sh run"        3 minutes ago       Up 3 minutes        0.0.0.0:32799->8080/tcp   systemtests_frontend_1
77470959a24c        mysql:latest                    "docker-entrypoint.s…"   3 minutes ago       Up 3 minutes        0.0.0.0:32798->3306/tcp   systemtests_db_1
```
Locate the PORT corresponding to `codedefenders/frontend:latest`, e.g., and connect to code-defenders using the browser, e.g., `http://localhost:32799/codedefenders/`

To shutdown the application, Ctrl-C the docker-compose process.

```bash
Gracefully stopping... (press Ctrl+C again to force)
Stopping systemtests_frontend_1 ... done
Stopping systemtests_db_1       ... done
```


# Project Import

Code Defenders and most of its dependencies are handled via Maven. Code Defenders can also be imported into common IDEs (e.g., IntelliJ and Eclipse).

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
