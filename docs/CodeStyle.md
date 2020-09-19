# Code style

## TLDR

Intellij IDEA automatically honors .editorconfig files, for Eclipse install the [editorconfig-eclipse](https://github.com/ncjones/editorconfig-eclipse) plugin.  
For both IDE are plugins available to support checkstyle. Intellij IDEA: [checkstyle-idea](https://plugins.jetbrains.com/plugin/1065-checkstyle-idea/), Eclipse: [Eclipse Checkstyle Plugin](https://checkstyle.org/eclipse-cs). Use the `checkstyle-codedefenders.xml` file which is for version 8.26.  
 To configure the import order:
   - Eclipse: Tick `Project > Properties > Java Code Style > Organize Imports > Enable project specific settings` and `Import` the `docs/codestyle/eclipse.importorder` file.
   - Intellij IDEA: `File > Settings > Editor > Code Style` and import the `docs/codestyle/intellij-importorder.xml` file with the settings button next to the `Schema` selector.


## .editorconfig

We use a `.editorconfig` file to establish a base formatting between all editors.  
See [editorconfig.org](https://editorconfig.org/).

### Intellij IDEA

Intellij should automatically honor the `.editorconfig`.

### Eclipse

Install the [editorconfig-eclipse](https://github.com/ncjones/editorconfig-eclipse) plugin.  
This unfortunately only handles a subset of the available editorconfig settings.  
The other settings have to be configured manually.

Remove trailing whitespace:
- Go to `Project > Properties > Java Editor > Save Actions`
  - Tick `Enable project specific settings`
  - Tick `Perform the selected actions on save`:
    - Tick `Additional actions` and click on `Configure`:
      - Under `Code Organizing` tick `Remove trailing whitespace` and `All lines`

TODO: Are other settings which we use missing?

## Checkstyle

For more specific java code formatting we use [checkstyle](https://checkstyle.sourceforge.io/).  
We provide our own checkstyle file in the repository: `checkstyle-codedefenders.xml`. This file is for the version 8.26 of checkstyle.

### Intellij IDEA

Install the [checkstyle-idea](https://plugins.jetbrains.com/plugin/1065-checkstyle-idea/) plugin.  
To configure it:
- Go to `File > Settings > Tools > Checkstyle`
  - Ensure `Checkstyle version` is set to the same value as mentioned above.
  - Add our checkstyle file
  - Set `Scan scope` to `Only Java sources (but not tests)`

If you want to manually run a scan use the provided `Check Module` button, as the `.class` files present in the `test/` path break checkstyle.

#### Import order

We have an import order different from the default Intellij one.  
This can be configured under `File > Settings > Editor > Code Style > Java`

- Set `Class count to use import with '*';` to 99
- Set `Names count to use static import with '*';` to 99
- Adapt the `Import Layout` list to match the following (and tick `Layout static imports separately`):
  - `import java.*` Tick `With Subpackages`
  - `<blank line>`
  - `import javax.*` Tick `With Subpackages`
  - `<blank line>`
  - `import org.*` Tick `With Subpackages`
  - `<blank line>`
  - `import all other imports`
  - `<blank line>`
  - `import static all other imports`

### Eclipse

For eclipse there exists the `Eclipse Checkstyle Plugin`.  
You have to manually install the plugin version which corresponds to the checkstyle file version.

TODO: Is this really necessary?

The plugin `.zip` file for checkstyle 8.26 can be found on [bintray](https://bintray.com/eclipse-cs/eclipse-cs/update-site-archive/8.26.0)

To configure it:
- Go to `Project > Properties > Checkstyle`
  - Under `Local Check Configurations`:
    - Add a new `Project Relative Configuration` which points to the `checkstyle-codedefenders.xml` file
  - Under `Main`:
    - Tick `Checkstyle active for this project`
    - Select the created local configuration under `Simple - use the following check configuration for all files`
    - Under exclude from checking:
      - Tick `files outside source directories`
      - Tick and configure `files from packages` to contain:
        - `src/main/resources`
        - `src/test/java`
        - `src/test/resources`

#### Import order

We have an import order different from the default Eclipse one.  
This can be configured under `Project > Properties > Java Code Style > Organize Imports`:
- Tick `Enable project specific settings`
- Configure the sorting order list to look like:
  - `java`
  - `javax`
  - `org`
  - `* - all unmatched type imports`
  - `* - all unmatched static imports`
- Set `Number of imports needed for .*` to 99
- Set `Number of static imports needed for .*` to 99
