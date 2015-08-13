#!/bin/bash
set -e

USER=meldingsutveksling
BINTRAY_APIKEY=b08f6db8fc698ba992a15131ecfcf93c8e8957fd

function _colored() {
	COLOR=${1?}
	TEXT=${2}

	if tty -s; then
		echo -e "\033[${COLOR}m${TEXT}\033[0m";
	else
		echo "${TEXT}"
	fi
}

function _confirm() {
	TEXT=${1?}
	read -p "${TEXT} [y/n]" -n 1 -r; echo
	[[ ! $REPLY =~ ^[Yy]$ ]]
}

function _bold() { 
	_colored "1;37" "$1" 
}

function _red() { 
	_colored "31" "$1"
}

function _abort() {
	_bold "Aborted."
	exit
}


FILENAME=$1
VERSION=$(echo $FILENAME | grep -oE "([0-9\.]+).*\d")
PACKAGE=$2

echo Filename $(_red $FILENAME)
echo Version  $(_red $VERSION)
echo Package  $(_red $PACKAGE)

_confirm "Upload? " && _abort

curl -v -# -o output -T $FILENAME -u$USER:$BINTRAY_APIKEY -H "X-Bintray-Publish: 1" -H "X-Bintray-Override: 1" https://api.bintray.com/content/meldingsutveksling/maven/$PACKAGE/$VERSION/$FILENAME;bt_package=$PACKAGE;bt_version=$VERSION & rm output


