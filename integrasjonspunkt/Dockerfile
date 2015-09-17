FROM dervism/dockerjava:jre7
MAINTAINER DIFI <espen.korra@difi.no>

LABEL package="no.difi"
LABEL artifact="meldingsutveksling"
LABEL version="1.0"
LABEL description="Direktoratet for forvaltning og IKT (Difi)"

#
# Open ports for incoming connections
#

EXPOSE 8080

#
# Install Java unlimited security policy
#

WORKDIR ${JAVA_HOME}/lib/security

ADD security.tar.gz ${JAVA_HOME}/lib/security

RUN rm -rf local_policy.jar US_export_policy.jar && \
    cp UnlimitedJCEPolicy/* . && \
    rm -rf UnlimitedJCEPolicy/ && rm ._UnlimitedJCEPolicy

#
# Install application
#

ENV APP_DIR /var/lib/difi
ENV APP_PREFIX integrasjonspunkt
ENV APP_MAIN_CLASS no.difi.meldingsutveksling.IntegrasjonspunktApplication
ENV APP_JAVA_PARAMS "-Dprivatekeyalias=974720760 -Dkeystorelocation=crt/test-certificates.jks -Dprivatekeypassword=changeit"
ENV APP_PROFILE dev

ADD target/${APP_PREFIX}*.jar ${APP_DIR}/
ADD ./integrasjonspunkt-local.properties ${APP_DIR}/

#
# Start the application
#

WORKDIR ${APP_DIR}

CMD APP_NAME=$(ls ${APP_PREFIX}*.jar) && java -jar ${APP_JAVA_PARAMS} ${APP_NAME} ${APP_MAIN_CLASS} --spring.profiles.active=${APP_PROFILE}
