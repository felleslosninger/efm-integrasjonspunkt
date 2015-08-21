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
ADD target/integrasjonspunkt*.jar ${APP_DIR}/

WORKDIR ${APP_DIR}
ENV APP_MAIN_CLASS no.difi.meldingsutveksling.IntegrasjonspunktApplication
ENV APP_PROFILE dev

#
# Start the application
#

CMD APP_NAME=$(ls integrasjonspunkt*.jar) && java -jar -Dprivatekeyalias=974720760 -Dprivatekeyloacation=test-certificates.jks -Dprivatekeypassword=changeit ${APP_NAME} ${APP_MAIN_CLASS} --spring.profiles.active=${APP_PROFILE}
