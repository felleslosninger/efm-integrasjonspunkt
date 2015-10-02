#!/bin/sh

# Set current execution dir to this folder
cd $(dirname $(readlink -f $0))

FILENAME=$(ls builds/integrasjonspunkt/integrasjonspunkt*jar)
PACKAGE=integrasjonspunkt

# Uncomment to upload to BinTray.com Maven repo
# ./upload.sh ${FILENAME} ${PACKAGE}

echo "Building Docker-image and container..."
/home/miif/builds/integrasjonspunkt/build-docker.sh -port=8098 -orgnr=910075918 -ip="http://integrasjonstest-miif.difi.local:8098" -monitor="http://integrasjonstest-miif.difi.local:8090" -name="Difi Nor"

if [ -f builds/integrasjonspunkt/integrasjonspunkt-local.properties ]; then
  echo "Cleaning up after build."
  rm builds/integrasjonspunkt/integrasjonspunkt-local.properties
fi

/home/miif/builds/integrasjonspunkt/build-docker.sh -transport=mock -port=8099 -orgnr=910077473 -ip="http://integrasjonstest-miif.difi.local:8099" -monitor="http://integrasjonstest-miif.difi.local:8090" -name="Difi Oslo"
