#!/bin/sh
# author: Dervis M
# Builds and starts the Adresseregister-Web module

# Set current execution dir to this folder
if [ -z "$1" ]; then
  # Linux
  cd $(dirname $(readlink -f $0))
  WORKING_DIR=/home/miif/github_source/meldingsutveksling-mellom-offentlige-virksomheter/adresseregister-web
else
  # Mac
  cd $(cd -P -- "$(dirname -- "$0")" && pwd -P)
  WORKING_DIR=$(pwd)
fi

# Build parameters
ADRESSEREGISTER_WEB=${WORKING_DIR}/

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
__jar ${ADRESSEREGISTER_WEB}

# Build Docker-images and then start the containers.
echo "Building and starting Adresseregister Web"
${WORKING_DIR}/build-docker-adresseregister.sh 9999
