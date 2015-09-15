FROM dervism/dockerjava:jre7
MAINTAINER DIFI <espen.korra@difi.no>

LABEL package="no.difi"
LABEL artifact="meldingsutveksling"
LABEL version="1.0"
LABEL description="Direktoratet for forvaltning og IKT (Difi)"

#
# Open ports for incoming connections
#

EXPOSE 8090

#
# Install application
#

ENV APP_DIR /var/lib/monitor
ENV APP_PREFIX integrasjonspunkt-monitor-1.0
ENV APP_MAIN_CLASS integrasjonspunkt.IntegrasjonspunktMonitorApplication
ENV APP_PROFILE docker
ENV JAVA_PARAMS ""

ADD target/${APP_PREFIX}*.jar ${APP_DIR}/

#
# Start the application
#

WORKDIR ${APP_DIR}

CMD APP_NAME=$(ls ${APP_PREFIX}*.jar) && java -jar ${JAVA_PARAMS} ${APP_NAME} ${APP_MAIN_CLASS} --spring.profiles.active=${APP_PROFILE}
