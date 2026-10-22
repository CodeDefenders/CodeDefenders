# Development Setup for CodeDefenders

This guide can be used to set up the environment for CodeDefenders in Debian. The code blocks are ready to be copy-pasted 
into your terminal -- no need for customizations.

## 1. Install required packages

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y tomcat10 tomcat10-admin mariadb-server maven ant
```

## 2. Fix Tomcat webapp ownership (so redeploys work)

```bash
sudo chown -R tomcat:tomcat /var/lib/tomcat10/webapps/
```

## 3. Set up MariaDB

```bash
sudo sed -i -E 's|bind-address.*|bind-address = 0.0.0.0|' /etc/mysql/mariadb.conf.d/50-server.cnf
sudo systemctl restart mariadb

sudo mysql -e "CREATE DATABASE codedefenders; CREATE USER 'codedefenders'@'%' IDENTIFIED BY 'codedefenders'; GRANT ALL PRIVILEGES ON codedefenders.* TO 'codedefenders'@'%'; FLUSH PRIVILEGES;"
```

## 4. Add Tomcat manager user (needed for maven deploy + manager UI)

Inserts a new user "manager" into `/etc/tomcat10/tomcat-users.xml`:

```bash
sudo sed -i -E 's|</tomcat-users>|  <role rolename="manager-script"/>\n  <role rolename="manager-gui"/>\n  <user username="manager" password="manager" roles="manager-gui,manager-script"/>\n</tomcat-users>|' /etc/tomcat10/tomcat-users.xml
```

## 5. Prepare the `installation` config file and run `../installation/setup.sh`

Creates `~/config.properties` based on the example, with `data.dir` filled in:

```bash
cat > ~/config.properties <<'EOF'
tomcat.username=manager
tomcat.password=manager
tomcat.url=http://127.0.0.1:8080/manager/text
tomcat.path=/
data.dir=/srv/codedefenders
EOF
```

Run the setup script (needs root to write under `/srv`):

```bash
cd installation
sudo ./setup.sh ~/config.properties
```

This creates `/srv/codedefenders` with the `lib`, `sources`, `mutants`, `tests`, `ai` subfolders.

Fix ownership afterward:

```bash
sudo chown -R tomcat:tomcat /srv/codedefenders
```

## 6. Enable Tomcat to write to the data dir + remote debugging

```bash
sudo mkdir -p /etc/systemd/system/tomcat10.service.d/
sudo tee /etc/systemd/system/tomcat10.service.d/override.conf > /dev/null <<'EOF'
[Service]
Environment="CATALINA_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000"
ReadWritePaths="/srv/codedefenders"
EOF
```

## 7. Create `/etc/tomcat10/codedefenders.properties`

Based on `example_codedefenders.properties`, with `auth.admin.users` used to grant yourself admin on first startup:

```bash
sudo tee /etc/tomcat10/codedefenders.properties > /dev/null <<'EOF'
data.dir=/srv/codedefenders
db.password=codedefenders
auth.admin.users=admin
metrics=true
javamelody=true
EOF
```

- `db.host`, `db.port`, `db.name`, `db.username` all default correctly (`127.0.0.1`, `3306`, `codedefenders`, `codedefenders`), so they're omitted.
  You can add them (or the commented version) manually, though. See [example.codedefenders.properties](/example.codedefenders.properties) for the full config.
- `auth.admin.users=admin` grants the Code Defenders user named `admin` admin rights. You later need to create a user with the name "admin" and restart tomcat10. 
   This is meant for initial setup only; once you've logged in as `admin`, remove this line.

## 8. Reload and start services

```bash
sudo daemon-reload 2>/dev/null; sudo systemctl daemon-reload
sudo systemctl restart tomcat10
sudo systemctl restart mariadb
```

If you're running this inside of WSL and `systemctl` isn't available in your shell, enable systemd first:
```ini
# /etc/wsl.conf
[boot]
systemd=true
```
then `wsl --shutdown` from Windows and reopen it.

## 9. Build and deploy Code Defenders

_Note: if you want to use the translations provided in this repo, install gettext first (`sudo apt install gettext`, see [i18n.md](i18n.md))._

```bash
mvn clean package -DskipTests
```

Deploy automatically to the Tomcat root via Maven, using the `../config.properties` from step 5 (Maven deploy plugin picks up `tomcat.*` from that same file):
```bash
mvn clean deploy -DskipTests
```

## 10. Verify

```bash
sudo systemctl status tomcat10
sudo systemctl status mariadb
```

Then visit http://localhost:8080 in your browser, register a user named `admin`, and `sudo systemctl restart tomcat10` to gain admin privileges.

## 11. Install Puzzles

You can install the provided default puzzles by going to [Admin > Puzzles](http://localhost:8080/admin/puzzles/management) and selecting `Install default puzzles`,
or install your own puzzles by uploading them as ZIP files.
