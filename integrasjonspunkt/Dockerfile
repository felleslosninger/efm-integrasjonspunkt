FROM openjdk:8-jre-slim

RUN groupadd -o -g 1000 java \
    && useradd -o -r -m -u 1000 -g 1000 java

ENV APP_DIR=/opt/integrasjonspunkt \
    HOST=tt02.altinn.no \
    PORT=443 \
    KEYSTOREFILE=${JAVA_HOME}/lib/security/cacerts \
    KEYSTOREPASS=changeit

RUN apt-get update && apt-get upgrade -y && apt-get install -y \
    ca-certificates \
    curl \
    openssl  && \
    update-ca-certificates && \
    rm -rf /var/cache/apt/* && \
    openssl s_client -connect ${HOST}:${PORT} </dev/null | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > ${HOST}.cert && \
    keytool -import -noprompt -trustcacerts -alias ${HOST} -file ${HOST}.cert -keystore ${KEYSTOREFILE} -storepass ${KEYSTOREPASS} && \
    mkdir ${APP_DIR} && \
    touch ${APP_DIR}/integrasjonspunkt-local.properties


WORKDIR ${APP_DIR}

# Add jar last, since this changes the most
RUN chown -R java:java ${APP_DIR}
RUN chmod +x ${APP_DIR}/*
COPY --chown=java:java /target/integrasjonspunkt.jar ${APP_DIR}/app.jar

USER java

HEALTHCHECK CMD curl -s --fail http://localhost:9093/manage/health > /dev/null || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar ${APP_DIR}/app.jar", "$@"]
