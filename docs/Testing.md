# Testing

System tests are currently not working!

## System Tests
System tests work by deploying Code Defenders inside disposable Docker containers and interacting with it by means of Selenium, which is again, running inside a Docker container.
This means that the DATA inside the DB are lost after the tests end.

Since we use Docker containers which are not (yet) registered in any Docker public repository, we need to manually build them. Once those are in place, system tests can be run from maven as follows (note that we use the System Test profile `ST`):

```bash
mvn clean package integration-test -PST
```

This command rebuilds and repackages the application using the `docker.properties` file. Then, it copies the resulting `.war` file in the right folder (`src/test/resources/systemtests/frontend`). Finally, it runs all tests, which are annotated with `@Tag(SYSTEM)`. Each test starts two docker instances for Code Defenders (one for the backend and on one for the front-end) and one docker instance for Selenium.
When containers are ready, the test code send the commands to the Selenium instance which must necessarily run on port 4444. When a test ends, all containers are disposed.

There's few catches. Since we use selenium-standalone we can run ONLY one system test at the time. The alternative (not in place) is to start a selenium-hub.

### Debug system tests

There's a `docker-compose-debug.yml` file under `src/test/resources/systemtests`. This file contains the configuration to run the debug-enabled selenium container, which opens a VNC port 5900 to which you can connect to (using any VNC client).

### Build codedefenders/frontend:latest

Assuming that you have `docker` and `docker-compose` installed.

```bash
cd src/test/resources/systemtests/tomcat9-jdk8
docker build -t codedefenders/tomcat:9 .
cd ../frontend
./setup-filesystem.sh ./config.properties
docker build -t codedefenders/frontend .
```
