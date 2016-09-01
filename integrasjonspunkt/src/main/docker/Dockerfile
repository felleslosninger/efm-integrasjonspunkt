FROM jeanblanchard/java:jre-8
MAINTAINER DIFI <espen.korra@difi.no>

LABEL package="no.difi"
LABEL artifact="meldingsutveksling"
LABEL version="1.0"
LABEL description="Direktoratet for forvaltning og IKT (Difi)"

ENV APP_DIR /var/lib/difi
ENV APP_PREFIX integrasjonspunkt
ENV APP_MAIN_CLASS no.difi.meldingsutveksling.IntegrasjonspunktApplication
ENV APP_JAVA_PARAMS "-Dprivatekeyalias=974720760 -Dkeystorelocation=crt/test-certificates.jks -Dprivatekeypassword=changeit"
ENV APP_PROFILE dev
ENV HOST tt02.altinn.basefarm.net
ENV PORT 443
ENV KEYSTOREFILE ${JAVA_HOME}/lib/security/cacerts
ENV KEYSTOREPASS changeit

#
# Install application
#

ADD ${APP_PREFIX}*.jar ${APP_DIR}/app.jar
#ADD ./integrasjonspunkt-docker.properties ${APP_DIR}/integrasjonspunkt-local.properties
RUN touch ${APP_DIR}/integrasjonspunkt-local.properties; echo -e '#!/bin/sh\njava -jar ${APP_JAVA_PARAMS} ${APP_DIR}/app.jar ${APP_MAIN_CLASS} --spring.profiles.active=${APP_PROFILE} "$@"' > ${APP_DIR}/run.sh && \
	chmod a+x ${APP_DIR}/run.sh
#
# Open ports for incoming connections
#


RUN apk add --update \
	    ca-certificates \
	    openssl  && \
	    update-ca-certificates && \
	    rm -rf /var/cache/apk/*

EXPOSE 8080

#
# Install Java unlimited security policy
#

WORKDIR ${JAVA_HOME}/lib/security

ADD security.tar.gz ${JAVA_HOME}/lib/security

RUN rm -rf local_policy.jar US_export_policy.jar && \
    cp UnlimitedJCEPolicy/* . && \
    rm -rf UnlimitedJCEPolicy/ && rm ._UnlimitedJCEPolicy


RUN openssl s_client -connect ${HOST}:${PORT} </dev/null | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > ${HOST}.cert && \
    keytool -import -noprompt -trustcacerts -alias ${HOST} -file ${HOST}.cert -keystore ${KEYSTOREFILE} -storepass ${KEYSTOREPASS}

#
# Install application
#

#
# Start the application
#

WORKDIR ${APP_DIR}

ENTRYPOINT ["./run.sh"]
