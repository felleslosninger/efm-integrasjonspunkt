FROM openjdk:8-jre-alpine
MAINTAINER DIFI <espen.korra@difi.no>

LABEL package="no.difi" artifact="meldingsutveksling" version="1.0" description="Direktoratet for forvaltning og IKT (Difi)"

ENV APP_DIR=/var/lib/difi \
    HOST=tt02.altinn.no \
    PORT=443 \
    KEYSTOREFILE=${JAVA_HOME}/lib/security/cacerts \
    KEYSTOREPASS=changeit \
    DOCKERIZE_VERSION=v0.2.0

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
    wget https://github.com/jwilder/dockerize/releases/download/$DOCKERIZE_VERSION/dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz && \
    tar -C /usr/local/bin -xzvf dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz

WORKDIR ${APP_DIR}

# Use tini to reap processes and signal handling
ENTRYPOINT ["/sbin/tini", "-g", "--", "sh", "-c", "./run.sh"]

# Add jar last, since this changes the most
ADD *.jar ${APP_DIR}/app.jar
ADD run.sh ${APP_DIR}/

HEALTHCHECK CMD curl -s --fail http://localhost:9093/manage/health > /dev/null || exit 1
