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

PACKAGE=$1
VERSION=$2

echo Package  $(_red $PACKAGE)
echo Version  $(_red $VERSION)

_confirm "Delete?" && _abort

curl -X DELETE -u$USER:$BINTRAY_APIKEY https://api.bintray.com/packages/meldingsutveksling/maven/$PACKAGE/versions/$VERSION




