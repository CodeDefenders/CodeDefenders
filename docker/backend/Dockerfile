FROM mysql:8.0

ARG GIT_COMMIT=unknown
ARG RELEASE_DATE=unknown

LABEL maintainer="alessio.gambi@uni-passau.de"

LABEL codedefenders.git.commit=$GIT_COMMIT
LABEL codedefenders.release.date=$RELEASE_DATE

COPY codedefenders.sql /docker-entrypoint-initdb.d
