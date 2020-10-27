# Docker

## Quick Start

Running codedefenders via docker-compose.  
For available versions take a look at the [dockerhub codedefenders repository](https://hub.docker.com/repository/docker/codedefenders/codedefenders).

```sh
cd docker
cp .env.example .env
nano .env # Adapt environment variables to your needs
docker-compose up
```

You can add other environment variables for configuration to the `.env` file.  
See [the configuration documentation](./Configuration.md) for details.

The docker container supports only a subset of the available configuration properties.  
E.g. all `cluster.*` related properties (inclusive `force.local.execution`) are not supported because we have no slurm support inside the container.

## Building docker images

We build from the git repository root, so we can simply copy the source code for building.

```sh
cd <path to codedefenders repository>
docker build --file ./docker/Dockerfile --tag codedefenders/codedefenders:<Codedefenders version> --label "maintainer=$(git config --get user.email)" .
docker push codedefenders/codedefenders:<codedefenders version>
```
