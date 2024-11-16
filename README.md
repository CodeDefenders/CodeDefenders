# Code Defenders

This is Code Defenders, a mutation testing game. Publicly available at [code-defenders.org](http://code-defenders.org).

## Vagrant
The repository contains a `Vagrantfile` for use with [Vagrant](https://www.vagrantup.com/) to quickly create a development environment.

### Steps to Use Vagrant:
1. Install and configure Vagrant on your system.
2. Run the following command to create a virtual machine:
   ```bash
   vagrant up
   ```
   - This creates a VM with all required software installed but does not deploy the `codedefenders.war` file.
3. By default, the following port forwarding settings are configured:
   - Tomcat server: Port 8080
   - JVM remote debugging: Port 8000
   - Database: Port 3306
4. Default credentials:
   - Database: Username: `codedefenders`, Password: `codedefenders`
   - Tomcat manager application: Username: `manager`, Password: `manager`
   - Admin access to Code Defenders: Username: `admin`, Password: `admin`

After the VM setup, deploy Code Defenders via the [Maven deployment steps](#automatic-deployment-via-maven).

## Prerequisites
This project requires the following software:
- Java (Version 1.8 or later)
- Tomcat Server (Version 10)
- Apache Maven
- Apache Ant
- MySQL (e.g., [MariaDB](https://mariadb.org/))

## Installation Steps
### Manual Installation
1. **Copy the configuration file**:
   Copy the `example.codedefenders.properties` file to `/var/lib/tomcat10/conf/codedefenders.properties` and set the values to match your environment.
   - The `data.dir` property is required, while other properties have default values.
2. **Adapt systemd service**:
   On Debian 10, modify the `tomcat10` service to allow `data.dir` outside the `/var/lib/tomcat10/webapps/` hierarchy:
   ```ini
   [Service]
   ReadWritePaths="/path/to/data.dir"
   ```
3. **Setup the database**:
   Create the MySQL or MariaDB database and user. Ensure the user has privileges to create databases, tables, and indexes.
4. **Create the data directory**:
   Create the `data.dir` folder with appropriate permissions for the tomcat user.
5. **Run the installation script**:
   Execute the `setup.sh` script:
   ```bash
   cd installation
   ./setup.sh /var/lib/tomcat10/conf/codedefenders.properties
   ```
   - The script verifies software availability, creates the required folder structure, and downloads dependencies.

   **Notes**:
   - If Tomcat runs under a different user, ensure data directory ownership is adjusted.
   - Root access may be needed for folder creation.

6. **Set up admin users**:
   To assign admin privileges, add a mapping in the `tomcat-users.xml` file (default location: `/path/to/tomcat/conf`):
   ```xml
   <role rolename="codedefenders-admin"/>
   <user username="admin_user" roles="codedefenders-admin"/>
   ```

### Automatic Deployment via Maven
1. **Configure Maven and Tomcat**:
   - Add a user with the `manager-script` role in `tomcat-users.xml`:
     ```xml
     <role rolename="manager-script"/>
     <user username="YOUR_USER" password="YOUR_PASSWORD" roles="manager-script"/>
     ```
2. **Deploy using Maven**:
   ```bash
   mvn clean deploy -DskipTests
   ```
   - Ensure the `tomcat10-admin` package is installed for Debian-based systems.

### Verification
Run the application by accessing it on the configured Tomcat server port (default: 8080).

## Build and Deployment
- To manually deploy, copy the `codedefenders.war` file to the Tomcat `webapps` directory.
- Ensure Tomcat and MySQL services are running.
- Download the `.war` file from the [latest release](https://github.com/CodeDefenders/CodeDefenders/releases) or build it with:
  ```bash
  mvn clean package -DskipTests
  ```

## External Documents
For more details, refer to:
- [Docker Documentation](docker/README.md)
- [Testing Document](docs/Testing.md)

## Help and Support
For FAQs and commonly encountered errors, check our pull requests and issues channel.

For troubleshooting errors like `INDEX command denied to user`, ensure the MySQL user has appropriate privileges.

## Version History
Refer to the [releases](https://github.com/CodeDefenders/CodeDefenders/releases) for version details.

## Supporters
This project is supported by [IMPRESS](https://impress-project.eu/). Visit [the official website](https://code-defenders.org/about) for a detailed list of supporters.
