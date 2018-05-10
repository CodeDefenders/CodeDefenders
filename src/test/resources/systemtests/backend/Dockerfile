FROM mysql:8.0

# SET ENV VARIABLES
ENV MYSQL_ROOT_PASSWORD=root \
	MYSQL_DATABASE=defender \
	MYSQL_USER=defender \
	MYSQL_PASSWORD=defender

# EXECUTE THE INIT SCRIPTS 
COPY ./mysql-init.sql /docker-entrypoint-initdb/

