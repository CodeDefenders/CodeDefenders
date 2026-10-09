# Vagrant

The repository contains a `Vagrantfile` for use with [Vagrant](https://www.vagrantup.com/) to quickly create a dev(!) environment.

If you have `vagrant` installed and configured, simply execute `vagrant up`. This will create a VM with all required software installed, but initially without the `codedefenders.war` file deployed.  
By default port forwarding for the tomcat server to port 8080, JVM remote debugging to port 8000 and for the database port to port 3306 are set up.

The database name, user and password are all `codedefenders`.  
The tomcat manager application (api and web) can be accessed with username and password `manager`.  
Admin access to codedefenders is possible with username and password `admin`.

Codedefenders can be deployed to the VM e.g. via the [maven deployment steps](/README.md#automatic-deployment-via-maven) and the `manager` credentials mentioned above. Everything required should already be setup.
