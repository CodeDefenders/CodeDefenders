# FAQ

[[_TOC_]]

## Q: I cannot deploy Code Defenders anymore, there's a SQL exception.
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


## Q: When I try to create a BattleGround, I got a 500 error page
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
