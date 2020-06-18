# Configuration

Below is the order in which CodeDefenders tries to load an `exampleProperty`.
Properties found in a later stage overwrite properties from earlier stages.

1. Try to load `example.property` from a `codedefenders.property` file found in the classpath.
2. Try to load `example.property` from a `codedefenders.property` file found in the `$CATALINA_HOME/config`.
3. Try to load `example.property` from a `codedefenders.property` file found in the directory specified in the `CODEDEFENDERS_CONFIG` environment variable.
4. Try to load `example.property` from a `codedefenders.property` file found in the directory specified in the `codedefenders.config` system property.
5. Try to load `java:comp/env/codedefenders/example.property` from the container context.
6. Try to load the content of the `CODEDEFENDERS_EXAMPLE_PROPERTY` environment variable.
7. Try to load the content of the `codedefenders.example.property` system property.
