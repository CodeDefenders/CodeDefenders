# Docker

Quick Start:

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
