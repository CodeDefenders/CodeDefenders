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
Code Defenders requires a `config.properties` file for initial setup, building and deployment. The file needs to be in the project root directory.
You can copy the `config.properties.example` file, which lists all necessary properties, over and update the sensible default values with your configuration.

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

```bash
mvn clean deploy -DskipTests
```

If you want to deploy the `.war` file by yourself its enough to execute:

```bash
mvn clean package -DskipTests
```

If you want to run the tests be sure the library `libncurses.so.5` is present on your system as the database tests depend on it.

### System Tests
System tests work by deploying Code Defenders inside disposable Docker containers and interacting with it by means of Selenium, which is again, running inside a Docker container.
This means that the DATA inside the DB are lost after the tests end.

Since we use Docker containers which are not (yet) registered in any Docker public repository, we need to manually build them. Once those are in place, system tests can be run from maven as follows (note that we use the System Test profile `ST`):

```bash
mvn clean compile package war:war integration-test -PST
```

This command rebuilds and repackages the application using the `config.properties@docker` file. Then, it copies the resulting `.war` file in the right folder (`src/test/resources/systemtests/frontend`). Finally, it runs all tests, which are annotated with `@Category(SystemTest.class)`. Each test starts two docker instances for Code Defenders (one for the backend and on one for the front-end) and one docker instance for Selenium.
When containers are ready, the test code send the commands to the Selenium instance which must necessarily run on port 4444. When a test ends, all containers are disposed.

There's few catches. Since we use selenium-standalone we can run ONLY one system test at the time. The alternative (not in place) is to start a selenium-hub.

#### Debug system tests

There's a `docker-compose-debug.yml` file under `src/test/resources/systemtests`. This file contains the configuration to run the debug-enabled selenium container, which opens a VNC port 5900 to which you can connect to (using any VNC client).

#### Build codedefenders/frontend:latest

Assuming that you have `docker` and `docker-compose` installed.

```bash
cd src/test/resources/systemtests/tomcat9-jdk8
docker build -t codedefenders/tomcat:9 .
cd ../frontend
./setup-filesystem.sh ./config.properties
docker build -t codedefenders/frontend .
```

#### Manually deploy the system using docker-compose
Code Defenders can be run as a set of docker containers using the provided docker-compose.yml file. To build and run the HEAD VERSION of Code Defenders which is stored in the Git Hub repo, you can run the following commands:

```bash
cd docker
docker-compose up
```

This command will build the required containers by cloning the git repo and by invoking the expected maven commands. As a consequence, the first time you run docker-compose it might take a while.

**Note** On the console, you will see outputs like:
```
db_1: mbind: Operation not allowed
```
You can ignore them.

After all components are started, you can access the application at: ```http://localhost/codedefenders```

To shutdown the entire application, Ctrl-C the docker-compose process.

```bash
Gracefully stopping... (press Ctrl+C again to force)
Stopping systemtests_frontend_1 ... done
Stopping systemtests_db_1       ... done
```

### Scalability
We enable scalability by replicating the front-end component of the application and by using a custom nginx load balancer to handle sticky sessions. Currently, the system supports up to 8 concurrent instances of the front-end. Use the scale parameter of docker run to spin off multiple front ends. The following command starts 4 front-end instances:

```bash
cd docker
docker-compose up --scale=frontend=4
```

### Persistence
Persistency is achieved using docker volumes. You can see which volumes are available, use the command:
```
docker volume ls
```

As long as the docker_datavolume and the docker_dbvolume (names might change) are there, the content of the mysql db and the data folder are persisted.
So if you have stopped, and even destroyed, the containers by using the same docker-compose file the data are preserved.

**Note** If you want to access the content of those volumes, e.g., to backup them, you must spin off a docker container which mounts them.

### Reliability
A minimum of reliability is guaranteed by automatically restarting the container if the fail or are manually stopped (by mistake). Therefore, unless you stop the docker-compose, containers will automatically restart.

**Note** Restarting the containers *might* not preserve the user sessions, that is, you users might have to re-login in the system after a crash/restart.

### Customization
If you need more instances or if you need to customize the deployment, you must update the Docker and various configurations files under the ```/docker``` folder. But at that point your are on your own.


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

# FAQ
### Q: When I try to create a BattleGround, I got a 500 error page
Code Defenders requires Java 1.8 also to compile the JSP. This is not the default options in many tomcat versions, despite you run tomcat on Java 1.8+.

To enable this feature, you must update the main tomcat's `web.xml` file, which is under `<TOMCAT_HOME>/conf/`.

Locate the following XML tags:

```xml
<servlet>
    <servlet-name>jsp</servlet-name>
    <servlet-class>org.apache.jasper.servlet.JspServlet</servlet-class>
    ...
    <load-on-startup>3</load-on-startup>
  </servlet>
```

And add the following inside the XML tag `<servlet>':

```xml
    <init-param>
        <param-name>compiler</param-name>
        <param-value>modern</param-value>
    </init-param>
    <init-param>
        <param-name>compilerSourceVM</param-name>
        <param-value>1.8</param-value>
    </init-param>
    <init-param>
        <param-name>compilerTargetVM</param-name>
        <param-value>1.8</param-value>
    </init-param>
    <init-param>
        <param-name>suppressSmap</param-name>
        <param-value>true</param-value>
    </init-param>
    <init-param>
      <param-name>fork</param-name>
      <param-value>false</param-value>
    </init-param>
    <init-param>
      <param-name>xpoweredBy</param-name>
      <param-value>false</param-value>
    </init-param>
```

This solution is inspired by and adapted from this [solution](https://stackoverflow.com/questions/18208805/does-tomcat-8-support-java-8) presented on StackOverflow.

### Q: I cannot deploy Code Defenders anymore, there's a SQL exception.
If you are running MySQL 5.7 and the SQL exception reads as follows:

```
java.sql.SQLException: Cannot create PoolableConnectionFactory
(The server time zone value 'CEST' is unrecognized or represents
more than one time zone. You must configure either the server or
JDBC driver (via the serverTimezone configuration property) to use
a more specific time zone value if you want to utilize time zone
support.)
```

This happens because the newest versions of mysql-connector perform some extra check on your DB and won't let you establish a connection unless everything is correct.

Run the following command to resolve it (until you restart mysql):

```
mysql -uroot -p
SET GLOBAL time_zone = '+1:00';
```
Use whatever matches your local time zone instead of `'+1:00'`

You can also fix it for good by updating your mysql configuration.

