# Configuration

## Loading of configuration properties

Below is the order in which CodeDefenders loads configuration properties.

The list is ordered by precedence (properties defined in locations higher in the list override those defined in lower locations).

The `location` for 4., 5. and 6. is a path to either a file in which case we directly load this file, or a directory in which case we load the `codedefenders.properties` file inside this directory.

Properties are loaded from:
1. JNDI attributes below `java:comp/env/codedefenders/` (e.g.: `example.property`)
2. Java System properties (e.g.: `codedefenders.example.property`)
3. OS Environment Variables (e.g.: `CODEDEFENDERS_EXAMPLE_PROPERTY`)
4. A location specified in the JNDI attribute `java:comp/env/codedefenders/config`
5. A location specified in the `codedefenders.config` system property.
6. A location specified in the `CODEDEFENDERS_CONFIG` environment.
7. The `codedefenders.properties` file in the `$CATALINA_BASE/conf` directory
8. The `codedefenders.properties` file in the classpath.  
(More precisely the `src/main/resources/codedefenders.properties` file which provides default values)

## Admin permission setup

Here is how to acquire admin permissions on a fresh Code Defenders instance:
- Create the admin accounts as regular user accounts, if you haven't already.
- Add the usernames of the accounts to the `auth.admin.users` config value.
Accounts named in this config value will be promoted to admin on application startup.
- (Re)start the application. The accounts should have admin permissions now.
If not, please check the logs for errors.
- Remove the usernames from `auth.admin.users` again.

## Renaming/Deprecation of configuration properties

Changing configuration properties is split into two steps, which will always happen in two different releases:
1. Deprecation of the old property and specification of the alternative property.  
  If the property has a default value the default value is only set on the old value and not on the new value.  
  On validating the configuration we will log a warning.  
  If the new property is set it will always have precedence over the deprecated property, regardless where the two are specified within the properties loading list.
2. Removal of the old property.  
  If the property has a default value it is now set on the new property.

This enables the following migration schema:
- Version A.B: 
   Deprecation of `old.property` with default value `default`.  
   The new property is `new.property` currently with now default value.  
   The application is configured to use `old.property` with value `other`.
- The admin sees the warning regarding the deprecated property and updates his configuration.  
  `old.property` defaults to `default` but is in any case overwritten by the configured `new.property` with value `other`
- Version X.Y (> A.B):
  Removal of `old.property`.
  `new.property` now has the default value of `default`.
  The application is configured to use `new.property` with value `other`.


## Running multiple instances of CodeDefenders on the same Tomcat server

To have different configuration files for each instance you have to configure the file location via the `java:comp/env/codedefenders/config` JNDI attribute.  
This can (in tomcat) be set on a per instance basis through a `.xml` file located at `$CATALINA_BASE/conf/[enginename]/[hostname]/[context path].xml`.

For running two instances of codedefenders on a Debian 12 host with the context paths `/` and `test` you would deploy the following files:

`/var/lib/conf/catalina/localhost/ROOT.xml`  
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Context>
    <Environment
            name="codedefenders/config"
            type="java.lang.String"
            value="/var/lib/conf/codedefenders-prod.properties"/>
</Context>
```

and 

`/var/lib/conf/catalina/localhost/test.xml`  
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Context>
    <Environment
            name="codedefenders/config"
            type="java.lang.String"
            value="/var/lib/conf/codedefenders-test.properties"/>
</Context>
```

and then configure them via the `/var/lib/conf/codedefenders-prod.properties` and `/var/lib/conf/codedefenders-test.properties` files.  
Configuration common to both instances (like `ant.home`) can be configured through the default `/var/lib/conf/codedefenders.properties` file.
