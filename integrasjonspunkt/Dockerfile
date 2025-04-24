FROM eclipse-temurin:21-jre-alpine

# Create non-root user
RUN addgroup -g 1000 java && \
    adduser -u 1000 -G java -s /bin/sh -D java

# Set environment
ENV APP_DIR=/opt/integrasjonspunkt \
    HOST=tt02.altinn.no \
    PORT=443 \
    KEYSTOREFILE=${JAVA_HOME}/lib/security/cacerts \
    KEYSTOREPASS=changeit

# Install tools and import external cert
RUN apk update && apk add --no-cache \
    ca-certificates \
    wget \
    openssl \
    && update-ca-certificates \
    && openssl s_client -connect ${HOST}:${PORT} </dev/null \
        | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > ${HOST}.cert \
    && keytool -import -noprompt -trustcacerts -alias ${HOST} \
        -file ${HOST}.cert -keystore ${KEYSTOREFILE} -storepass ${KEYSTOREPASS} \
    && mkdir -p ${APP_DIR} \
    && touch ${APP_DIR}/integrasjonspunkt-local.properties

# Set working directory and permissions
WORKDIR ${APP_DIR}
COPY target/integrasjonspunkt.jar ${APP_DIR}/app.jar
RUN chown -R java:java ${APP_DIR} && chmod +x ${APP_DIR}/*

# Drop to non-root user
USER java

# Healthcheck (customize port/path as needed)
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:9093/manage/health || exit 1

# Run the app
ENTRYPOINT ["java", "-jar", "/opt/integrasjonspunkt/app.jar"]
