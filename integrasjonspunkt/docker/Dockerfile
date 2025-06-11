FROM maven:3.9-eclipse-temurin-21 AS builder

ARG GIT_PACKAGE_TOKEN
ARG GIT_PACKAGE_USERNAME

ENV GIT_PACKAGE_TOKEN=${GIT_PACKAGE_TOKEN}
ENV GIT_PACKAGE_USERNAME=${GIT_PACKAGE_USERNAME}

COPY /integrasjonspunkt/docker/settings.xml /root/.m2/settings.xml

WORKDIR /home/app
COPY pom.xml ./
COPY  . ./

RUN echo "Contents of /root/.m2/settings.xml:" && \
    cat /root/.m2/settings.xml && \
    echo "GIT_PACKAGE_TOKEN: ${GIT_PACKAGE_TOKEN}" && \
    echo "GIT_PACKAGE_USERNAME: ${GIT_PACKAGE_USERNAME}"

RUN --mount=type=cache,target=/root/.m2/repository mvn -B package dependency:go-offline -Dmaven.gitcommitid.skip=true -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre-alpine

# Create non-root user
RUN addgroup -g 1000 java && \
    adduser -u 1000 -G java -s /bin/sh -D java

# Set environment
ENV APP_DIR=/opt/integrasjonspunkt \
    HOST=tt02.altinn.no \
    PORT=443 \
    KEYSTOREFILE=${JAVA_HOME}/lib/security/cacerts \
    KEYSTOREPASS=changeit \
    APPLICATION=integrasjonspunkt

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
COPY --from=builder /home/app/integrasjonspunkt/target/${APPLICATION}.jar ${APP_DIR}/app.jar
RUN chown -R java:java ${APP_DIR} && chmod +x ${APP_DIR}/*

# Drop to non-root user
USER java

# Healthcheck (customize port/path as needed)
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:9093/manage/health || exit 1

# Run the app
ENTRYPOINT ["java", "-jar", "/opt/integrasjonspunkt/app.jar"]
