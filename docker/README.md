# Docker build instructions
1. Create compose network + containers but not starting them
```
docker-compose up --no-start
```
2. Next we are going to get the IP of the docker host for the ```docker_default``` network (docker host IP <=> gateway IP in docker_default network). Execute following command and copy its output:
```
docker network inspect docker_default --format '{{(index (index .IPAM.Config) 0).Gateway}}'
```
3. Create a ```CodeDefenders/docker/.env``` file that includes the following content
```
# Replace this version number with the Codefender's version you want to install
RELEASE_VERSION=v1.6.1
# Replace with the calculated IP in step 2.
ALLOWED_REMOTE_ADDR_REGEXP=172.XXX.YYY.ZZZ
```
2. Start the containers:
```
docker-compose up
```

Now you can access to CodeDefenders:
- [Regular UI](http://localhost/codedefenders)
- [Administrative UI](http://localhost/codedefenders/admin). Accessing the Admnistrative UI requires an additional user+password combination (check ```CODEDEF_MANAGER_USERNAME``` below).

# Configuration options for CodeDefenders (```frontend``` service)

| Variable                            | Defaul value | Meaning |
| :---:                               |    :----:    | :---: |
| CODEDEF_MANAGER_USERNAME            | user         | Username for the [Apache Tomcat manager](https://tomcat.apache.org/tomcat-9.0-doc/manager-howto.html#Configuring_Manager_Application_Access) |
| CODEDEF_MANAGER_PASSWORD            | password     | Apache Tomcat manager user password |
| CODEDEF_MANAGER_ROLES               | manager-gui  | Roles asigned to the Apache Tomcat manager user |
| CODEDEF_MANAGER_ALLOWED_REMOTE_ADDR | 127.\d+.\d+.\d+ &#124; ::1 &#124; 0:0:0:0:0:0:0:1                                      | [Regular expression to check allowed IPs for Apache Tomcat manager](https://tomcat.apache.org/tomcat-9.0-doc/config/valve.html#Remote_Address_Valve) |
| CODEDEF_ADMIN_USERNAME              | admin        | Username used to access CodeDefenders administrative interface |
| CODEDEF_ADMIN_PASSWORD              | password     | CodeDefenders administrative user password |
| CODEDEF_ADMIN_ROLES                 | manager-gui  | Roles asigned to the CodeDefenders administrative user |
| CODEDEF_LOAD_BALANCER_IP            | IP of the "load-balancer" container | IP of the load-balancer use for CodeDefenders. This IP is configured as an [Apache Tomcat remote trusted proxy](https://tomcat.apache.org/tomcat-9.0-doc/config/valve.html#Remote_IP_Valve). |


