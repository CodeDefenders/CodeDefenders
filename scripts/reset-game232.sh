#!/bin/bash

shopt -s expand_aliases
. ~/.bash_profile

mysql -pal3ss10 defender < /Users/gambi/CodeDefenders/src/test/resources/db/emptydb.sql 
rm -rf /tmp/defender/sources/IntHashMap
rm -rf /tmp/defender/tests/mp
rm -rf /tmp/defender/mutants/mp
tomcat-shutdown
mv /Users/gambi/Downloads/apache-tomcat-8.5.23/logs/catalina.out /Users/gambi/Downloads/apache-tomcat-8.5.23/logs/catalina.out.bkp
touch /Users/gambi/Downloads/apache-tomcat-8.5.23/logs/catalina.out
tomcat-start
