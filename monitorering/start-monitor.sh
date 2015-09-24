#!/bin/sh
# author: Dervis M
# Builds and starts the monitoring server for the Integrasjonspunkt application
# This is version should be used when running Mac.

cd $(cd -P -- "$(dirname -- "$0")" && pwd -P)

# Build parameters
WORKING_DIR=$(pwd)
SPRING_EUREKA=${WORKING_DIR}/integrasjonspunkt-eureka
SPRING_ADMIN=${WORKING_DIR}/integrasjonspunkt-monitor

# Trigger a Maven build if jar files 
# does not exist.
__jar() {
    _JAR=$(ls $1/target/*jar)
    if [ -z "$_JAR" ]; then
        echo "There is no jar file in target folder."
	mvn clean install -DskipTests=true
    else
       echo "Found $_JAR"
    fi
}

# Check if jar files exits
# Docker will not not build otherwise.
__jar ${SPRING_EUREKA}
__jar ${SPRING_ADMIN}

# Build Docker-images and then start the containers.
echo "Building and starting Spring Eureka Server"
${SPRING_EUREKA}/build-docker.sh mac

echo "Building and starting Spring Boot Admin UI"
${SPRING_ADMIN}/build-docker.sh 8090 mac
