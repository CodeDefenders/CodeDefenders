# Clone Code Defenders HEAD from GitHub 
FROM alpine/git:latest AS repo
WORKDIR /
RUN git clone --depth 1 https://github.com/CodeDefenders/CodeDefenders.git

WORKDIR /CodeDefenders
RUN git checkout ${RELEASE_VERSION}
# Collect label values to file since we cannot tag containers from within Docker
RUN  echo "export GIT_COMMIT=$(git rev-parse HEAD)" > .git-data && \
     echo "export GIT_DATE=\"$(git log -1 --format=%cd)\"" >> .git-data

# Build this image by copying the sql files from the "repo" image and the .git-data
FROM mysql:8.0
LABEL maintainer="alessio.gambi@uni-passau.de"
WORKDIR /
COPY --from=repo /CodeDefenders/.git-data /.git-data
COPY --from=repo /CodeDefenders/src/main/resources/db/codedefenders.sql /docker-entrypoint-initdb.d
