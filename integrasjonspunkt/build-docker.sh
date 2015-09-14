#!/bin/sh
# author: Dervis M
# Builds an image and creates a container.

# Build params
IMAGE_NAME=difi/difi_integrasjonspunkt_$1
CONTAINER_NAME=Difi_Integrasjonspunkt_$1

echo ${CONTAINER_NAME}

# Must stop any running container to continue
docker stop ${CONTAINER_NAME}

# Must be the root folder
WORKING_DIR="/home/miif/builds"
CERTIFICATE_DIR=${WORKING_DIR}/certificates
PORT=$1

echo "Working dir: $WORKING_DIR"

if [ -z "$1" ]; then
  echo "You have to specify a port number. Format ./build-docker.sh portNumber"
  exit
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
docker build --no-cache -t ${IMAGE_NAME} /home/miif/builds &&\

# Create new container
docker create --name ${CONTAINER_NAME} -v ${CERTIFICATE_DIR}:/var/lib/difi/crt -p ${PORT}:8080 ${IMAGE_NAME}

# Done
echo "$CONTAINER_NAME is build. Starting the container. To see log output, run docker logs -f $CONTAINER_NAME (CTRL+C to exit logs)."
echo

docker start $CONTAINER_NAME
