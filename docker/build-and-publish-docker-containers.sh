#! /bin/bash

# This script builds a docker image with the current release of code-defenders and publishes that to docker hub

set -e

. docker.credentials

# It requires the ability of running multi-stage build

# Get maven version. We use this as TAG for the docker image
CURRENT_VERSION=$(mvn -f ../pom.xml -o \
	org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v 'INFO')

# Collect additional metadata for the containers
CURRENT_GIT_COMMIT=$(git rev-parse HEAD)
TODAY=$(date)

# This is to push the new containers on DockerHub
: ${DOCKER_PASS:?Missing}
: ${DOCKER_USER:?Missing}

# This is to download the latest version of codedefenders from our gitlab
: ${GITLAB_USERNAME:?Missing}
: ${GITLAB_ACCESS_TOKEN?Missing}

echo "Build codedefenders/backend:${CURRENT_VERSION}"

cd backend

# Docker does not support including files outside its context so we need to copy the required resources under "."
cp ../../src/main/resources/db/codedefenders.sql .

# No need to specify db credential here, we do it with docker-compose
docker build \
        --build-arg GIT_COMMIT="${CURRENT_GIT_COMMIT}" \
        --build-arg RELEASE_DATE="${TODAY}" \
            -t codedefenders/backend:${CURRENT_VERSION} \
            .

# Clean up
rm codedefenders.sql

cd -

# Note: Handling of credential is problematic since code defenders rely on build/deployment time credentials ...
# For the moment all the credentials are hardcoded
echo "Build codedefenders/frontend:${CURRENT_VERSION}"

cd frontend

# Create the config file for the build
cat > config.properties << EOF
data.dir=/codedefenders
ant.home=/usr

db.url=jdbc:mysql://db:3306/defender
db.username=defender
db.password=defender

cluster.mode=disabled
cluster.java.home=
cluster.timeout=
cluster.reservation.name=

parallelize=enabled
forceLocalExecution=enabled
mutant.coverage=enabled
block.attacker=enabled
EOF

# The front end is build using docker multi-stage build. It might take a while
docker build \
	--build-arg RELEASE_VERSION="${CURRENT_VERSION}" \
        --build-arg GIT_COMMIT="${CURRENT_GIT_COMMIT}" \
        --build-arg RELEASE_DATE="${TODAY}" \
        --build-arg gitlab_username="${GITLAB_USERNAME}" \
        --build-arg gitlab_access_token="${GITLAB_ACCESS_TOKEN}" \
            -t codedefenders/frontend:${CURRENT_VERSION} \
            .

# Clean up
rm config.properties

cd -


echo "Push codedefenders/backend:${CURRENT_VERSION} to Docker Hub"

docker login \
    --username=$DOCKER_USER \
    --password=$DOCKER_PASS

docker push codedefenders/backend:${CURRENT_VERSION}

echo "Push codedefenders/backend:${CURRENT_VERSION} to Docker Hub"

docker push codedefenders/frontend:${CURRENT_VERSION}

# Create a convenient docker-compose file for the current version

cat > docker-compose-${CURRENT_VERSION}.yml << EOF
db:
  image: codedefenders/backend:${CURRENT_VERSION}
  environment:
    MYSQL_ROOT_PASSWORD: root
    MYSQL_USER: defender
    MYSQL_PASSWORD: defender
    MYSQL_DATABASE: defender
  ports:
    - "3306"

frontend:
  image: codedefenders/frontend:${CURRENT_VERSION}
  ports:
    - 8080
  links:
    - db
EOF
