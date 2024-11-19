#!/bin/bash
#
# automates the installation of edth.sql
# see: https://flamingo.ics.uci.edu/toolkit/
# see also: https://flamingo.ics.uci.edu/toolkit/docs/UdfDoc.html

URL="https://flamingo.ics.uci.edu/toolkit/toolkit_2021-05-18.tgz"

if [ "$OSNAME" != "linux-gnu" ]; then
	echo "error: unsupported OS"
	exit 1
fi

sudo apt-get install -y gcc make mysql-server libmysqlclient-dev

PLUGIN_DIR="/usr/lib/mysql/plugin"
if [ ! -d PLUGIN_DIR ]; then
	echo "missing dir: ${PLUGIN_DIR} - please find it"
	echo "placing libedth.so in pwd instead"
	PLUGIN_DIR="$(pwd)"
fi

rm -rf ./toolkit
rm -rf ./._toolkit
curl "$URL" -L > ./toolkit_2021-05-18.tgz
tar -xzvf ./toolkit_2021-05-18.tgz
pushd toolkit/src/udf/mysql/ed
make libedth.so
if [ -z $? ]; then
	mv libedth.so "${PLUGIN_DIR}"
	mv edth.sql "$(dirs +1)"
fi
popd
rm -rf ./toolkit_2021-05-18.tgz
rm -rf ./toolkit
rm -rf ./._toolkit

sudo /etc/init.d/mysql restart


