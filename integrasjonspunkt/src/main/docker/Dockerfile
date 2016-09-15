FROM openjdk:8-jre-alpine
MAINTAINER DIFI <espen.korra@difi.no>

LABEL package="no.difi" artifact="meldingsutveksling" version="1.0" description="Direktoratet for forvaltning og IKT (Difi)"

ENV APP_DIR=/var/lib/difi \
    APP_PREFIX=integrasjonspunkt \
    APP_MAIN_CLASS=no.difi.meldingsutveksling.IntegrasjonspunktApplication \
    APP_JAVA_PARAMS="-Dprivatekeyalias=974720760 -Dkeystorelocation=crt/test-certificates.jks -Dprivatekeypassword=changeit" \
    APP_PROFILE=dev \
    HOST=tt02.altinn.basefarm.net \
    PORT=443 \
    KEYSTOREFILE=${JAVA_HOME}/lib/security/cacerts \
    KEYSTOREPASS=changeit


RUN apk add --update \
	    ca-certificates \
        curl \
	    openssl  && \
	    update-ca-certificates && \
	    rm -rf /var/cache/apk/*

#
# Install Java unlimited security policy
#

WORKDIR ${JAVA_HOME}/lib/security

ADD security.tar.gz ${JAVA_HOME}/lib/security

RUN cd /tmp/ && \
    curl -LO "http://download.oracle.com/otn-pub/java/jce/8/jce_policy-8.zip" -H 'Cookie: oraclelicense=accept-securebackup-cookie' && \
    unzip jce_policy-8.zip && \
    rm jce_policy-8.zip && \
    yes |cp -v /tmp/UnlimitedJCEPolicyJDK8/*.jar ${JAVA_HOME}/lib/security/ && \
    openssl s_client -connect ${HOST}:${PORT} </dev/null | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > ${HOST}.cert && \
    keytool -import -noprompt -trustcacerts -alias ${HOST} -file ${HOST}.cert -keystore ${KEYSTOREFILE} -storepass ${KEYSTOREPASS}

#
# Start the application
#

WORKDIR ${APP_DIR}

EXPOSE 8080

ENTRYPOINT ["./run.sh"]

#
# Install application
#

RUN touch ${APP_DIR}/integrasjonspunkt-local.properties; echo -e '#!/bin/sh\njava -jar ${APP_JAVA_PARAMS} ${APP_DIR}/app.jar ${APP_MAIN_CLASS} --spring.profiles.active=${APP_PROFILE} "$@"' > ${APP_DIR}/run.sh && \
	chmod a+x ${APP_DIR}/run.sh
ADD ${APP_PREFIX}*.jar ${APP_DIR}/app.jar
#
# Open ports for incoming connections
#
