FROM openjdk:8-jre-slim

RUN groupadd -o -g 1000 java \
    && useradd -o -r -m -u 1000 -g 1000 java

ENV APP_DIR=/opt/integrasjonspunkt \
    HOST=tt02.altinn.no \
    PORT=443 \
    KEYSTOREFILE=${JAVA_HOME}/lib/security/cacerts \
    KEYSTOREPASS=changeit \
    RUNTIME_PORT=9093

RUN apt-get update && apt-get upgrade -y && apt-get install -y \
    ca-certificates \
    curl \
    openssl  && \
    update-ca-certificates && \
    rm -rf /var/cache/apt/* && \
    openssl s_client -connect ${HOST}:${PORT} </dev/null | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > ${HOST}.cert && \
    keytool -import -noprompt -trustcacerts -alias ${HOST} -file ${HOST}.cert -keystore ${KEYSTOREFILE} -storepass ${KEYSTOREPASS} && \
    mkdir ${APP_DIR} && \
    touch ${APP_DIR}/integrasjonspunkt-local.properties && \
    chown -R java:java ${APP_DIR} && \
    chmod +x ${APP_DIR}/*


WORKDIR ${APP_DIR}
COPY --chown=java:java --chmod=755 /target/integrasjonspunkt.jar ${APP_DIR}/app.jar

USER java

HEALTHCHECK CMD curl -s --fail http://localhost:$RUNTIME_PORT/manage/health > /dev/null || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar ${APP_DIR}/app.jar", "$@"]