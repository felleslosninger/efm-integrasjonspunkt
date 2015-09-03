#!/bin/sh
# author: Dervis M
# Builds an image and creates a container.

# Build params
IMAGE_NAME=difi/difi_integrasjonspunkt
CONTAINER_NAME=Difi_Integrasjonspunkt

# Must be the root folder
WORKING_DIR=$(pwd)
RESOURCES_DIR=${WORKING_DIR}/src/main/resources/
CERTIFICATE_DIR=${WORKING_DIR}/build/certificate
BUILD_DIR=build/certificate

# Copy the certificate to the build folder
CERTIFICATE_FILE=$(ls ${BUILD_DIR}/*.jks)
if [ "$TMP" == "" ]; then
  echo "Copying certificate to build folder"
  cp ${RESOURCES_DIR}/*.jks ${BUILD_DIR}
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
docker build --no-cache -t ${IMAGE_NAME} . &&\

# Create new container
docker create --name ${CONTAINER_NAME} -v ${CERTIFICATE_DIR}:/var/lib/difi/crt -p 8080:8080 ${IMAGE_NAME}

# Done
echo "$CONTAINER_NAME is build. To start the container, write: docker start $CONTAINER_NAME"
echo
