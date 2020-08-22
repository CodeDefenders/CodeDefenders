# Application Design

Description of the overall design of the application. This also includes best practices and recommended approaches when adding new features.

This document doesn't necessarily describe the current status of the project but rather the targeted state.
So you can also refer to this document when refactoring or replacing other parts of the application.


## Persistence layer / Database access

Classes for accessing/storing data should go into `org.codedefenders.database.access`. The class names should end with `DAO`.

The classes which represent the stored data should go into `org.codedefenders.database.entities`.


## Service / Business logic layer

Classes should be stored inside `org.codedefenders.database.service`.

The classes should return DTOs or objects assembled from DTOs.

Most methods for accessing data should take at least a kind of `User` object as argument


## Data transfer objects / DTOs

View only representations of the data stored in the persistence layer.
Returned from the Business Logic Layer and passed to Servlets/JSP/.tag files.


## Servlets

Servlet classes should only access the business logic layer.


## Presentation Layer

This can be the webview or some kind of API (REST, GraphQL, etc).

Reusable components should be JSP `.tag` files placed under `src/webapp/WEB-INF/tags`.
They should not contain Java Code (Scriplets) instead they should use the JSTL (JSP - Standard Tag Library) and the JSP EL (Expression Language).
Most tag-files probably have some kind of backing Bean. This must NOT be included via the `<jsp:useBean>` tag, because afaik the creation/managment of this bean doesn't happen through WELD/CDI and all the CDI features are then not accessible in this class.
Inside the EL tags (`${}`) CDI beans can simply be accessed by their name (either given through the `@Named` annotation or the class name starting with a lower case letter).
To get IDE support (at least in Intellij) you can add a JSP comment specifying the type of the variable: `<%-- @elvariable id="className" type="java.lang.Boolean" --%>`

### Client side dependencies

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
