# Docker build instructions

1. Create a ```CodeDefenders/docker/.env``` file that includes the following content
```
# Replace this version number with the Codefender's version you want to install
RELEASE_VERSION=v1.7
```

2. Create compose network + containers but not starting them
```
docker-compose up --no-start
```

3. Next we are going to get the IP of the docker host for the ```docker_default``` network (docker host IP <=> gateway IP in docker_default network). Execute following command and copy its output:
```
docker network inspect docker_default --format '{{(index (index .IPAM.Config) 0).Gateway}}'
```
1. Add in ```CodeDefenders/docker/.env``` file the following content
```
# Replace with the calculated IP in step 3.
ALLOWED_REMOTE_ADDR_REGEXP=172.XXX.YYY.ZZZ
```

5. Start the containers:
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
| CODEDEF_ADMIN_ALLOWED_REMOTE_ADDR | 127.\d+.\d+.\d+ &#124; ::1 &#124; 0:0:0:0:0:0:0:1                                      | [Regular expression to check allowed IPs](https://tomcat.apache.org/tomcat-9.0-doc/config/filter.html#Remote_Address_Filter) to access CodeDefender's Administrative UI |
| CODEDEF_LOAD_BALANCER_IP            | IP of the "load-balancer" container | IP of the load-balancer use for CodeDefenders. This IP is configured as an [Apache Tomcat remote trusted proxy](https://tomcat.apache.org/tomcat-9.0-doc/config/valve.html#Remote_IP_Valve). |

## Config.properties / context.xml dynamic properties
| Variable                             | Equivalent in config.properties | Defaul value                  | Meaning |
| :---:                                | :----:                          | :---:                         | :---: |
| CODEDEF_CFG_DATA_DIR                 | data.dir                        | /codedefenders                | The main Code Defenders folder. |
| CODEDEF_CFG_ANT_HOME                 | ant.home                        | /usr                          | Location of Ant command. |
| CODEDEF_CFG_DB_URL                   | db.url                          | jdbc:mysql://db:3306/defender | JDBC url to connect MySQL server. |
| CODEDEF_CFG_DB_USERNAME              | db.username                     | defender                      | Database user name |
| CODEDEF_CFG_DB_PASSWORD              | db.password                     | defender                      | Database password |
| CODEDEF_CFG_CLUSTER_MODE             | cluster.mode                    | disabled                      | Execute tests in cluster mode (SLURM) (see src/main/java/org/codedefenders/execution/AntRunner.java). |
| CODEDEF_CFG_CLUSTER_JAVA_HOME        | cluster.java.home               |                               | Java Home on cluster (see src/main/java/org/codedefenders/execution/AntRunner.java). |
| CODEDEF_CFG_CLUSTER_TIMEOUT          | cluster.timeout                 |                               | SLURM Job timeout (see src/main/java/org/codedefenders/execution/AntRunner.java). |
| CODEDEF_CFG_CLUSTER_RESERVATION_NAME | cluster.reservation.name        |                               | SLURM reservation name (see src/main/java/org/codedefenders/execution/AntRunner.java). |
| CODEDEF_CFG_FORCE_LOCAL_EXECUTION    | force.local.execution           | enabled                       | Force compilation and testing of original version on the local machine (see src/main/java/org/codedefenders/execution/AntRunner.java). |
| CODEDEF_CFG_PARALLELIZE              | parallelize                     | enabled                       | Parallelize Ant task execution (see src/main/java/org/codedefenders/execution/KillMap.java). |
| CODEDEF_CFG_MUTANT_COVERAGE          | mutant.coverage                 | enabled                       | Skip tests on mutants that are not covered (see src/main/java/org/codedefenders/execution/KillMap.java). |
| CODEDEF_CFG_BLOCK_ATTACKER           | block.attacker                  | enabled                       | Block the attackers if there are pending equivalence duels. |


