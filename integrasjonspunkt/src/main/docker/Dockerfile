FROM openjdk:8-jre-alpine
MAINTAINER DIFI <espen.korra@difi.no>

LABEL package="no.difi" artifact="meldingsutveksling" version="1.0" description="Direktoratet for forvaltning og IKT (Difi)"

ENV APP_DIR=/var/lib/difi \
    APP_PROFILE=dev \
    HOST=tt02.altinn.basefarm.net \
    PORT=443 \
    KEYSTOREFILE=${JAVA_HOME}/lib/security/cacerts \
    KEYSTOREPASS=changeit

RUN apk add --update \
    ca-certificates \
    tini \
    curl \
    openssl  && \
    update-ca-certificates && \
    rm -rf /var/cache/apk/* && \
    openssl s_client -connect ${HOST}:${PORT} </dev/null | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > ${HOST}.cert && \
    keytool -import -noprompt -trustcacerts -alias ${HOST} -file ${HOST}.cert -keystore ${KEYSTOREFILE} -storepass ${KEYSTOREPASS} && \
    mkdir ${APP_DIR} && \
    touch ${APP_DIR}/integrasjonspunkt-local.properties && \
    echo -e '#!/bin/sh\njava -jar ${APP_JAVA_PARAMS} ${APP_DIR}/app.jar ${APP_MAIN_CLASS} --spring.profiles.active=${APP_PROFILE} "$@"' > ${APP_DIR}/run.sh && \
    chmod a+x ${APP_DIR}/run.sh

WORKDIR ${APP_DIR}

EXPOSE 8080

# Use tini to reap processes and signal handling
ENTRYPOINT ["/sbin/tini", "-g", "--", "./run.sh"]

# Add jar last, since this changes the most
ADD *.jar ${APP_DIR}/app.jar