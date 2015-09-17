#!/bin/sh
# author: Dervis M
# Builds an image and creates a container.

# Build params
IMAGE_NAME=difi/difi_integrasjonspunkt_$1
CONTAINER_NAME=Difi_Integrasjonspunkt_$1
echo ${CONTAINER_NAME}

# Must stop any running container to continue
docker stop ${CONTAINER_NAME}

# Specify where the Dockerfile and Certificate is
WORKING_DIR=$(pwd)
CERTIFICATE_DIR=${WORKING_DIR}/src/main/resources
PORT=$1
echo "Working dir: $WORKING_DIR"

# Require a port
if [ -z "$1" ]; then
  echo "You have to specify a port number. Format ./build-docker.sh portNumber orgNumber"
  exit 1
fi

# Require an organization number
if [ -z "$2" ]; then
  echo "You have to specify an organization number. Format ./build-docker.sh portNumber orgNumber"
  exit 1
else
  echo "orgnumber=$2" > integrasjonspunkt-local.properties
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
