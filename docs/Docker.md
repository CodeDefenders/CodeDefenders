# Docker

We provide a `docker-compose` file for running Codedefenders.  
Beginning with version 1.8, we publish docker images for released versions at the [dockerhub codedefenders repository](https://hub.docker.com/repository/docker/codedefenders/codedefenders).

## Quick Start

As all [configuration](#configuration) values have sensible defaults you can simply do:

```sh
cd docker
docker-compose up
```

to get a working Codedefenders instance.  
By default, it will be available at http://localhost:8080/ in your browser.

## Configuration

Configuring the `docker-compose` setup is possible via environment variables.  
You can find all available docker compose specific variables with their default values in the `.env.example` file.

For environment variables which are available by default see  [the configuration documentation](./Configuration.md).

The docker container supports only a subset of the available configuration properties.  
E.g. all `cluster.*` related properties (inclusive `force.local.execution`) are not supported because we have no slurm support inside the container.

**WARNING:**  
Changing the `CODEDEFENDERS_DB_*` variables after creating the container for the first time requires you to update the settings in the database container manually.  
Otherwise, you have to destroy the [volumes](#persistence).


## Building docker images

We build from the git repository root, so we can simply copy the source code for building.

```sh
cd <path to codedefenders repository>
docker build --file ./docker/Dockerfile --tag codedefenders/codedefenders:<Codedefenders version> --label "maintainer=$(git config --get user.email)" .
docker push codedefenders/codedefenders:<codedefenders version>
```

## Using docker for testing a not published state

For testing, you can tag a local build docker image and use this for the docker-compose.  
Be aware that we use named docker volumes which are persisted on the host.  
So building new images and deploying them will still get the old data.

```sh
cd <path to codedefenders repository>
docker build --file ./docker/Dockerfile --tag codedefenders/codedefenders:dev .
cd docker
nano .env # Set CODEDEFENDERS_VERSION=dev
docker-compose up
```

## Persistence

The `docker-compose` file uses named volumes to persist the database and codedefenders data directory.  
The names used in a normal setup should be `docker_datavolume` and `docker_dbvolume`.

Those are managed via the `docker volume` command.

As long as those volumes are there, the content of the mysql db and the data folder are persisted.
So if you have stopped, and even destroyed, the containers by using the same docker-compose file the data are preserved.

**WARNING:**  
You should only ever `rm` both volumes at the same time as otherwise the database will reference no longer existing files, or the database will try to write at places where already some files exist.

More information can be found in the [docker documentation](https://docs.docker.com/storage/volumes/)

### Reliability
A minimum of reliability is guaranteed by automatically restarting the container if the fail or are manually stopped (by mistake). Therefore, unless you stop the docker-compose, containers will automatically restart.

**Note** Restarting the containers *might* not preserve the user sessions, that is, you users might have to re-login in the system after a crash/restart.

### Customization
If you need more instances or if you need to customize the deployment, you must update the Docker and various configurations files under the ```/docker``` folder. But at that point your are on your own.
