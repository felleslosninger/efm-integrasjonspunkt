#!/bin/sh
# author: Dervis M
# Builds an image and creates a container.
# DAVID: Difi Application Deployer

# Build params
IMAGE_NAME=difi/difi_integrasjonspunkt_$1
CONTAINER_NAME=Difi_Integrasjonspunkt_$1

# Set current execution dir to this folder
cd $(cd -P -- "$(dirname -- "$0")" && pwd -P)
WORKING_DIR=$(pwd)

# Debug info
echo "Building Docker image $IMAGE_NAME and container $CONTAINER_NAME"

# Must stop any running container to continue
STATUS_ID=$(docker ps -q -f name=${CONTAINER_NAME})
if [ -n "$STATUS_ID" ]; then
  echo "Stopping $CONTAINER_NAME..."
  docker stop ${CONTAINER_NAME}
fi

# Specify where the Certificate is
if [ -d "$WORKING_DIR/certificates" ]; then
  # When running from Linux
  CERTIFICATE_DIR=${WORKING_DIR}/certificate
else
  # When running from source folder
  CERTIFICATE_DIR=${WORKING_DIR}/src/main/resources
fi

PORT=$1
echo "Working dir: $WORKING_DIR"

if [ $# -ge 2 ]; then

  # Required parameteres
  # ---------------------

  # Port number
  if [ -z "$1" ]; then
    echo "You have to specify a port number. Format ./build-docker.sh portNumber orgNumber"
    exit 1
  fi

  # Organization number
  if [ -z "$2" ]; then
    echo "You have to specify an organization number. Format ./build-docker.sh portNumber orgNumber"
    exit 1
  else
    echo "orgnumber=$2" > integrasjonspunkt-local.properties
  fi

  # Optional parameteres
  # ---------------------

  if [ $# -gt 2 ]; then

    # If specified, add a custom Admin serviceUrl
      if [ -n "$3" ]; then
        echo "spring.boot.admin.client.serviceUrl=$3" >> integrasjonspunkt-local.properties
      fi

      # If specified, add a Admin serverUrl
      if [ -n "$4" ]; then
        echo "spring.boot.admin.url=$4" >> integrasjonspunkt-local.properties
      fi

      # If specified, add a custom Admin clientName
      # This will override the 'spring.application.name' property
      if [ -n "$5" ]; then
        echo "spring.boot.admin.client.name=$5" >> integrasjonspunkt-local.properties
      fi
  fi

  # Misc settings
  echo "spring.boot.admin.autoDeregistration=true" >> integrasjonspunkt-local.properties
else
  echo
  echo "Invalid number of arguments."
  echo "Required parameters missing: portNumber orgNumber (1) (2) (3)"
  echo
  echo "Optional parameters:"
  echo "(1) local serviceUrl: This application's monitoring URL, typically just its server ip and port number"
  echo "(2) monitor serverUrl: The URL to the external monitoring application"
  echo "(3) clientName: The name that such be used when registering the application into the monitoring application."
  echo
  exit 1
fi

# Remove any existing container
OLD_CONTAINERS=$(docker ps -a | grep ${IMAGE_NAME})
if [ "$OLD_CONTAINERS" != "" ]; then
  echo "Removing old containers:"
  docker rm $(docker ps -a | grep ${IMAGE_NAME} | awk '{print $1}')
fi

# Remove all unused images
OLD_IMAGES=$(docker images | grep '<none>')
if [ "$OLD_IMAGES" != "" ]; then
  echo "Removing old images:"
  docker rmi $(docker images | grep '<none>' | awk '{print $3}')
fi

# Build new image
docker build --no-cache -t ${IMAGE_NAME} ${WORKING_DIR} &&\

# Create new container
docker create --name ${CONTAINER_NAME} -v ${CERTIFICATE_DIR}:/var/lib/difi/crt -p ${PORT}:8080 ${IMAGE_NAME}

# Done
echo "$CONTAINER_NAME is build."
echo "Starting the container. To see log output, run docker logs -f $CONTAINER_NAME (CTRL+C to exit logs)."
echo

docker start $CONTAINER_NAME
