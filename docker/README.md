# Code Defenders

This Code Defenders, a mutation testing game.  
A public instance is available at [code-defenders.org](https://code-defenders.org/) and the source code can be found at [GitHub](https://github.com/CodeDefenders/CodeDefenders/).

We provide a [`docker-compose` file](https://github.com/CodeDefenders/CodeDefenders/blob/master/docker/docker-compose.yml) for a simple setup.  
Documentation for getting started can be found [below](#docker-compose).


## Images and supported tags

We (more or less regularly) publish images to:
- [quay.io/codedefenders/codedefenders](https://quay.io/repository/codedefenders/codedefenders)
- [docker.io/codedefenders/codedefenders](https://hub.docker.com/r/codedefenders/codedefenders)
- [ghcr.io/codedefendes/codedefenders](https://github.com/CodeDefenders/CodeDefenders/pkgs/container/codedefenders)

We publish images for tagged Code Defenders versions/releases (starting at version 1.8).  
But also sometimes (`git-<short ref hash>`) tags, build of the corresponding git commit.

If you want to build images locally take a look at the [Building Container Images section](#building-container-images).


## docker-compose

The `docker-compose` file is available in our GitHub repository at [`docker/docker-compose.yml`](https://github.com/CodeDefenders/CodeDefenders/blob/master/docker/docker-compose.yml).  
The easiest way to get it is to check out the repository locally.  

Otherwise, download the `docker-compose.yml` and `example.env` file, like:
```shell
mkdir docker
curl https://raw.githubusercontent.com/CodeDefenders/CodeDefenders/master/docker/docker-compose.yml > docker/docker-compose.yml
curl https://raw.githubusercontent.com/CodeDefenders/CodeDefenders/master/docker/example.env > docker/example.env
```


### Quick Start

Copy the `docker/example.env` file to `docker/.env` and update the `CODEDEFENDERS_VERSION` variable to the tag of the image you want to use.  

Now you can

```sh
cd docker
docker-compose up
```

to spin up the containers.  
By default, Code Defenders will be available at http://localhost:8080/ in your browser.

---

Note: It is a good idea to check out the corresponding git tag (in this example `v1.8.1`), as it is possible that the docker-compose setup from two versions ago does not work with the docker image from the current version.

### Configuration

Configuring the `docker-compose` setup is possible via environment variables.  
You can find all available docker compose specific variables with their default values in the `example.env` file.

For environment variables which are available by default see [the configuration documentation](https://github.com/CodeDefenders/CodeDefenders/blob/master/docs/Configuration.md).

The docker container supports only a subset of the available configuration properties.  
E.g. all `cluster.*` related properties (inclusive `force.local.execution`) are not supported because we have no slurm support inside the container.

**WARNING:**  
Changing the `CODEDEFENDERS_DB_*` variables after creating the container for the first time requires you to update the settings in the database container manually.  
Otherwise, you have to destroy the [volumes](#persistence).

**NOTE:**  
The container supports the intentionally undocumented `CODEDEFENDERS_TOMCAT_LISTENING_PORT` variable to change the port the tomcat server in the container binds to.  
It can be used to prevent port collisions for setups were the codedefenders containers are run with the (less secure) `--network=host` option.

### Persistence

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

**Note** Restarting the containers *might* not preserve the user sessions, that is, your users might have to re-login in the system after a crash/restart.


### Customization

If you need more instances or if you need to customize the deployment, you must update the Docker and various configurations files under the ```/docker``` folder. But at that point you are on your own.



## Building container images

We build from the git repository root, so we can simply copy the source code for building.

```sh
cd <path to codedefenders repository>
docker build --file ./docker/Dockerfile --tag codedefenders/codedefenders:<Codedefenders version> .
```

The official images are build (and published) from the CI with the help of the [build-container-image](../scripts/ci/build-container-image) and [push-container-images](../scripts/ci/push-container-images) scripts (Repository local links).


## Using docker for local testing/development

You can tag a locally built docker image and use this for the docker-compose.  
Be aware that we use named docker volumes which are persisted on the host.  
So building new images and deploying them will still get the old data.

```sh
cd <path to codedefenders repository>
docker build --file ./docker/Dockerfile --tag localhost/codedefenders:dev .
cd docker
nano .env # Set CODEDEFENDERS_VERSION=dev and CODEDEFENDERS_IMAGE=localhost/codedefenders
docker-compose up
```
