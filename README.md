# Code Defenders

This is Code Defenders, a mutation testing game.

## Requirements

- Tomcat Server v7
- Ant (set environment variable ANT_HOME)
- Maven
- MySQL

## Data Storage
Code Defenders must store data in /var/lib/codedefenders/
The tomcat user (and possibly the user running Code Defenders) must
have full permissions on this directory.
Major and Evosuite must be stored in their respective folders within
this directory (codedefenders/major and codedefenders/evosuite).

## Deployment

To compile and deploy the first time:

```
$ make first
```

To compile and redeploy:

```
$ make
```

### Passwords

Passwords are needed to deploy to the tomcat webapps directory
(`TOMCAT_PASSWORD`), to connect to MySQL (`MYSQL_PASSWORD`) and to
enable the contact form feature (`EMAIL_PASSWORD`). After editing the
`makefile` file, you can run `make passwords` to apply the changes in
the appropriate places in the project. Please make sure not to push
your local passwords to the repository.


# Public URL

<http://code-defenders.dcs.shef.ac.uk>
