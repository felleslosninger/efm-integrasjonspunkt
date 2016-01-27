#!/bin/sh
# author: Dervis M
# Builds an image and creates a container.
#
# This script uses the following format for its configuration parameters:
# ./build-docker.sh -parameterName1=parameterValue1 -parameterName2=parameterValue2
#
# To see the help menu, simply run it with the -h or -help parameters
# ./build-docker.sh -h

# Init
set -e

# Set current execution dir to this folder
cd $(cd -P -- "$(dirname -- "$0")" && pwd -P)
WORKING_DIR=$(pwd)

# Configure some colors if we
# running in terminal mode.
RED=""; BLACK=""; RESET=""
if [ -t 1 ]; then
    RED="\033[1;31m"; BLACK="\033[1;30m"; RESET="\033[0m"
fi

# Manual, help menu
__help() {
  echo "Usage: ./build-docker.sh -port=portNum -orgnr=orgNum (-i,m,n,t)"
  echo
  echo "${BLACK}Required parameters:${RESET}"
  echo
  echo  "   -p | -port :         Which port Docker should map this container on your computer"
  echo  "   -o | -orgnr :        The organization number that should be used by this Integrasjonspunkt"
  echo
  echo "${BLACK}Optional parameters:${RESET}"
  echo
  echo "    -i | -ip             This application's server ip and port number"
  echo "    -m | -monitor        The URL or IP to the external monitoring application"
  echo "    -n | -name           The name that should be used when registering the application into the monitoring application"
  echo "    -t | -transport      The transport channel the application should use"
  echo
}

# Standard value
TRANSPORT=altinn

# Verbose parsing of parameters
for i in "$@"; do
  case $i in
      -p=*|-port=*)
        PORT="${i#*=}"
      ;;
      -o=*|-orgnr=*)
        ORGNUM="${i#*=}"
      ;;
      -i=*|-ip=*)
        INTERNAL_SERVER_IP="${i#*=}"
      ;;
      -m=*|-monitor=*)
        EXTERNAL_MONITOR_IP="${i#*=}"
      ;;
      -n=*|-name=*)
        DOCKER_NAME="${i#*=}"
      ;;
      -t=*|-transport=*)
        TRANSPORT="${i#*=}"
      ;;
      -h|-help)
        __help && exit
      ;;
      *)
         # unknown option
         echo "Unknown parameter ${i} found. Ignoring it."
      ;;
  esac
done

if [ $# -ge 2 ]; then

  # Required parameteres
  # ---------------------

  # Port number
  if [ -z "$PORT" ]; then
    echo "You have to specify a port number. Run with -h or -help parameter to see usage."
    exit 1
  fi

  # Organization number
  if [ -z "$ORGNUM" ]; then
    echo "You have to specify an organization number. Run with -h or -help parameter to see usage."
    exit 1
  else
    echo "orgnumber=$ORGNUM" > integrasjonspunkt-local.properties
  fi

  # Build params
  IMAGE_NAME=difi/difi_integrasjonspunkt_$PORT
  CONTAINER_NAME=Difi_Integrasjonspunkt_$PORT

  # Debug info
  echo "Building Docker image $IMAGE_NAME and container $CONTAINER_NAME"
  echo "Working dir: $WORKING_DIR"

  # Optional parameteres
  # ---------------------

  if [ $# -gt 2 ]; then

    # If specified, add a custom Admin serviceUrl
      if [ -n "$INTERNAL_SERVER_IP" ]; then
        echo "Configuring application ip: $INTERNAL_SERVER_IP"
        echo "spring.boot.admin.client.serviceUrl=$INTERNAL_SERVER_IP" >> integrasjonspunkt-local.properties
      fi

      # If specified, add a Admin serverUrl
      if [ -n "$EXTERNAL_MONITOR_IP" ]; then
        echo "Configuring external monitor ip: $EXTERNAL_MONITOR_IP"
        echo "spring.boot.admin.url=$EXTERNAL_MONITOR_IP" >> integrasjonspunkt-local.properties
      fi

      # If specified, add a custom Admin clientName
      # This will override the 'spring.application.name' property
      if [ -n "$DOCKER_NAME" ]; then
        echo "Configuring Docker container name: $DOCKER_NAME"
        echo "spring.boot.admin.client.name=$DOCKER_NAME" >> integrasjonspunkt-local.properties
      fi

      # If specified, add a custom Transport channel
      case $TRANSPORT in
        mock*)
            echo "Configuring Transport channel: $TRANSPORT"
            echo "altinn.streamingservice.url=http://Integrasjonstest-miif.difi.local:9999/" >> integrasjonspunkt-local.properties
            echo "altinn.brokerservice.url=http://Integrasjonstest-miif.difi.local:9999/" >> integrasjonspunkt-local.properties
        ;;
      esac
  fi

  # Misc settings
  echo "spring.boot.admin.autoDeregistration=true" >> integrasjonspunkt-local.properties
else
  echo
  echo "${RED}Invalid number of arguments${RESET}."
  echo
  __help
  exit 1
fi

exit

# Must stop any running container to continue
STATUS_ID=$(docker ps -q -f name=${CONTAINER_NAME})
if [ -n "$STATUS_ID" ]; then
  echo "Stopping $CONTAINER_NAME..."
  docker stop ${CONTAINER_NAME}
fi

# Specify where the Certificate is
if [ -d "$WORKING_DIR/certificates" ]; then
  # When running from Linux
  CERTIFICATE_DIR=${WORKING_DIR}/certificates
else
  # When running from source folder
  CERTIFICATE_DIR=${WORKING_DIR}/src/main/resources
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
