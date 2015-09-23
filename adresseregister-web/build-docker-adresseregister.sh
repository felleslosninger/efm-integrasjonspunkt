#!/bin/sh
# author: Dervis M
# Builds an image and creates a container.

# Build params
IMAGE_NAME=difi/adresseregister
CONTAINER_NAME=Difi_AdresseRegister

# Must stop any running container to continue
echo "Stopping $CONTAINER_NAME"
docker stop ${CONTAINER_NAME}

# Must be the root folder
WORKING_DIR=$(pwd)
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
docker build --no-cache -t ${IMAGE_NAME} . &&\

# Create new container
docker create --name ${CONTAINER_NAME} -p ${PORT}:9999 ${IMAGE_NAME}

# Done
echo "$CONTAINER_NAME is build. Starting the container."
echo "To see log output, run docker logs -f $CONTAINER_NAME (CTRL+C to exit logs)."
echo

docker start $CONTAINER_NAME

# Special handling for AdresseRegister
# Insert certificates here:
echo "Updating Adresseregister with default certificates. Please wait."

# Wait for the application to start
sleep 20

# Then try to post the certificates
curl -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache" -H "Postman-Token: df94c5f5-4ac8-0866-9467-f0912d21ac5d" -d '{
"organizationNumber" : "974720760", 
"orgName" : "DIFI",
 "pem" : "-----BEGIN CERTIFICATE-----\nMIID/DCCAuSgAwIBAgIEAs0dWzANBgkqhkiG9w0BAQsFADBeMRIwEAYDVQQKEwlEaWZpIHRlc3Qx\nEjAQBgNVBAUTCTk5MTgyNTgyNzE0MDIGA1UEAxMrRElGSSB0ZXN0IHZpcmtzb21oZXRzc2VydGlm\naWF0IGludGVybWVkaWF0ZTAeFw0xNTAxMDEyMzQ2MzNaFw0xNzAyMDEyMzQ2MzNaMD0xEjAQBgNV\nBAUTCTk3NDcyMDc2MDEnMCUGA1UEAxMeRElGSSB0ZXN0IHZpcmtzb21oZXRzc2VydGlmaWF0MIIB\nIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAs5x8oBOdp/8UhixN20lSNl8z9WyrgUSGNtKC\nvuArrIgMw8AcdozlrzJ2sp1Lg94czA1iURtAsky/F3DJ8VGCjvk04jcj/dKuOsMvwGK2TJPn4JPn\nBIeJls/8yvBT1VJnQ0+0xxuenL7vAH5sbP1B2qC6Pnf3WN9l8iQrUTsAzR7nzrwlDI1BWJsmI0PD\ngsoQ5ErEfgw7cDSGi/4S0NiPZUYfeWjha1LTeObDNP6+gfxzI/smT4e/JxcwgYMS/SJLFnPxb7zj\nQNoXuakilYZ3TyPxk8ZwHzJztV5UcykElVGuyhswAOTs/xrJHCnUfF1bLNlffTUVr+zWSfHugQTo\nhQIDAQABo4HiMIHfMIGLBgNVHSMEgYMwgYCAFBwaROx+jMV17T8nbBo+6Vf0hn38oWKkYDBeMRIw\nEAYDVQQKEwlEaWZpIHRlc3QxEjAQBgNVBAUTCTk5MTgyNTgyNzE0MDIGA1UEAxMrRElGSSB0ZXN0\nIHZpcmtzb21oZXRzc2VydGlmaWF0IGludGVybWVkaWF0ZYIEIbToeDAdBgNVHQ4EFgQUg1V9fWXp\n17AzpIFKLRg+w+lCnA0wCQYDVR0TBAIwADAVBgNVHSAEDjAMMAoGCGCEQgEBAQFkMA4GA1UdDwEB\n/wQEAwIEsDANBgkqhkiG9w0BAQsFAAOCAQEARBc7eqN4p0a9Qifsm1IblS9F5HC06VYQRA6MjihV\nzZILeN3nf1w4TShzrWCKPJBjPBycfXoKzxsIaQpSIKVw/hBmZ/l/xyTGZinAG2YgpDEdS+FbEh24\ndXhMNRKO07YhArVcUacrmV9UqZwCwulrvF34Y2kKpLUBZs1Sp9b29heLOR+Z1Xefl3k496HViZs9\n6EJuAak/ydgonwcxxLRhgP70E/IFEygTOLiOxNR9Hec4snWK2nRpMLZq/oQlpxC3SbbkOVAx2ufl\nd+SaMaGEVI32ySdbRSuEsskIDx0zy5rZuzaudcNdwLKWaHtB8pk1KJtpEFu2LCo4kK1I4TWx1Q==\n-----END CERTIFICATE-----\n"}
}
' 'http://10.243.200.51:9999/certificates'

curl -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache" -H "Postman-Token: e8ac2fda-6baf-2c5c-5528-9364c46529f2" -d '{
"organizationNumber" : "987464291", 
"orgName" : "DIFI2",
 "pem" : "-----BEGIN CERTIFICATE-----\nMIID/DCCAuSgAwIBAgIENA0fKzANBgkqhkiG9w0BAQsFADBeMRIwEAYDVQQKEwlEaWZpIHRlc3Qx\nEjAQBgNVBAUTCTk5MTgyNTgyNzE0MDIGA1UEAxMrRElGSSB0ZXN0IHZpcmtzb21oZXRzc2VydGlm\naWF0IGludGVybWVkaWF0ZTAeFw0xNTAxMDEyMzQ2MzNaFw0xNzAyMDEyMzQ2MzNaMD0xEjAQBgNV\nBAUTCTk4NzQ2NDI5MTEnMCUGA1UEAxMeRElGSSB0ZXN0IHZpcmtzb21oZXRzc2VydGlmaWF0MIIB\nIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzsS7f45nBO8bL+nQ/oxUHEaSRg+zhwWZ8HVt\nFucoXi+hLt89IFM/OiC9YYaidjGo8P+C4FfDzeUJolsApeQ3c0I94Uhz+MICi0GgPYKbyw9EPMTD\nmuCkjbaMk3e+EbuzDiE2usYNWtIpzdRgqPTxOXToBydD4qFx8rLOfTzRqufTOD85xLTfKm0tlibM\n25z4pf9FMgPnZ8c735EN/Pe7ok2uVpnWDj9YlESGJyhUeQJKZotNsGILAm6o5hNWBUh7bY18rDiG\nZjPjZ36JH0sQRsITRy3Nhc/KpxkDMqXY2LcotMM8XoilI/YKkhJvg/e0qYT6fnFcDaU46hzYVSn9\nwwIDAQABo4HiMIHfMIGLBgNVHSMEgYMwgYCAFBwaROx+jMV17T8nbBo+6Vf0hn38oWKkYDBeMRIw\nEAYDVQQKEwlEaWZpIHRlc3QxEjAQBgNVBAUTCTk5MTgyNTgyNzE0MDIGA1UEAxMrRElGSSB0ZXN0\nIHZpcmtzb21oZXRzc2VydGlmaWF0IGludGVybWVkaWF0ZYIEIbToeDAdBgNVHQ4EFgQU1Nh6rWdb\n8KkNiKfOcQjiAfuGJCEwCQYDVR0TBAIwADAVBgNVHSAEDjAMMAoGCGCEQgEBAQFkMA4GA1UdDwEB\n/wQEAwIEsDANBgkqhkiG9w0BAQsFAAOCAQEAAcMiUkXq1IO3M/wU1YbdGr6+2dhsgxKaGeDt7bl1\nefjyENXI6dM2dspfyVI+/deIqX7VW/ay8AqiNJyFlvA9CMxW51+FivdjGENzRAKGF3pFsvdwNBEw\nFQSZCYoo8/gm59SidmnPNFeziUsE3fbQ22BPxW3l8ScSbYhgLlK9Tkr/ul3h7ByVtUdolP99eyCp\n1/TgC8EBZHZRC1v221+0AQ09A/SI/gyomgCeXVfH1Ll08v7BCTE1nE1aUqMDpDjOeWc73+f2X6vb\nUQdK4QwRU+pl5Oz6QgAFZ2mOD6DmqRfVoibM9sWgCkO5t6lpW86E/wixZBfS9TW/RJgH7461gg==\n-----END CERTIFICATE-----\n"
}
' 'http://10.243.200.51:9999/certificates'

echo "Done. Adresseregister is now ready for use."
