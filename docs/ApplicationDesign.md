# Application Design

Description of the overall design of the application. This also includes best practices and recommended approaches when adding new features.

This document doesn't necessarily describe the current status of the project but rather the targeted state.
So you can also refer to this document when refactoring or replacing other parts of the application.


## Cross-Cutting Concerns

### Logging

We use _SLF4J_.  
`Logger` should be listed as first line in a class as `private static final` variable.


### Metrics

We include/support exposing [Prometheus Metrics](https://prometheus.io/) via the [Prometheus Java Client Library](https://github.com/prometheus/client_java).  
Exposing metrics currently has to be enabled via a configuration property.

For an introduction take a look at the available [Metric Types](https://prometheus.io/docs/concepts/metric_types/) and the [Best Practices for Metric and Label Naming](https://prometheus.io/docs/practices/naming/).

Metrics should be used/registered/listed just like `Logger`s as `private static final` variables, after the `Logger` at the top of a class.

This should AFAIK not influence the testability of the classes in any way.


### Authentication/Authorization

We use [Apache Shiro](https://github.com/apache/shiro).  
Mostly located in `org.codedefenders.auth` and `org.codedefenders.servlets.auth`.

For checking/accessing Authentication/Authorization Info please use `org.codedefenders.service.AuthService`.


### (Database) Transactions

Related classes are mostly located in `org.codedefenders.transaction`.  
For most use-cases it should be enough to annotate the Service method with the `@Tranasctional` annotation.  
Transactions can be explicit requested/managed via `org.codedefenders.transaction.TransactionManager` if desired.


## Layers

### Persistence layer / Database access

Classes for accessing/storing data should go into `org.codedefenders.database.access`. The class names should end with `DAO`.

The classes which represent the stored data should go into `org.codedefenders.database.entities`.


### Service / Business logic layer

Classes should be stored inside `org.codedefenders.database.service`.

The classes should return DTOs or objects assembled from DTOs.

Most methods for accessing data should take at least a kind of `User` object as argument


### Data transfer objects / DTOs

View only representations of the data stored in the persistence layer.
Returned from the Business Logic Layer and passed to Servlets/JSP/.tag files.


### Servlets / Presentation Layer

The Presentation Layer can be the webview (Servlets + JSPs) or some kind of API (REST, GraphQL, etc).

Servlet classes should only access the business logic layer.

Reusable components should be JSP `.tag` files placed under `src/webapp/WEB-INF/tags`.
They should not contain Java Code (Scriplets) instead they should use the JSTL (JSP - Standard Tag Library) and the JSP EL (Expression Language).
Most tag-files probably have some kind of backing Bean. This must NOT be included via the `<jsp:useBean>` tag, because afaik the creation/management of this bean doesn't happen through WELD/CDI and all the CDI features are then not accessible in this class.
Inside the EL tags (`${}`) CDI beans can simply be accessed by their name (either given through the `@Named` annotation or the class name starting with a lower case letter).
To get IDE support (at least in Intellij) you can add a JSP comment specifying the type of the variable: `<%-- @elvariable id="className" type="java.lang.Boolean" --%>`

#### Client side dependencies

Its rather easy to just copy-past needed js or css files into the git repository.
The problems with this approach are:
 - It is difficult to determine if the files in the repo and the upstream version differ or not.
 - So everyone is afraid of updating these dependencies as things could break.
 - Tracking versions/dependencies + updating is time consuming

So we manage client side dependencies via maven and [WebJars](https://www.webjars.org/).
To use a new dependency:
 - Look at their website if there exists a webjar which packages the dependency.
 - Add the corresponding maven dependency fragment to the `pom.xml`.
 - Load it via `webjars/<artifact-name>/<version>/<path-to-file>`.
   The path has in many cases similarities to the cdn url. In addition, Intellij IDEA has autocompletion for the path.
