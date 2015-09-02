#!/bin/sh

WORKING_DIR=$(pwd)

docker build --no-cache -t difi/difi_integrasjonspunkt . && \
docker create --name Difi_Integrasjonspunkt -v ${WORKING_DIR}/src/main/resources/:/var/lib/difi/crt -p 8080:8080 difi/difi_integrasjonspunkt