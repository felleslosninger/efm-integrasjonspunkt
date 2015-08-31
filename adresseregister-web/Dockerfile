FROM dervism/dockerjava:jre8
MAINTAINER DIFI <espen.korra@difi.no>

LABEL package="no.difi"
LABEL artifact="meldingsutveksling"
LABEL version="1.0"
LABEL description="Direktoratet for forvaltning og IKT (Difi)"

#
# Open ports for incoming connections
#

EXPOSE 9999

#
# Install application
#

ENV APP_DIR /var/lib/monitor/eureka
ENV APP_PREFIX adresseregister-web-1.0-SNAPSHOT
ENV APP_MAIN_CLASS no.difi.meldingsutveksling.adresseregister.Main
ENV JAVA_PARAMS ""

ADD target/${APP_PREFIX}*.jar ${APP_DIR}/

#
# Start the application
#

WORKDIR ${APP_DIR}

CMD APP_NAME=$(ls ${APP_PREFIX}*.jar) && java -jar ${JAVA_PARAMS} ${APP_NAME} ${APP_MAIN_CLASS}
