#!/bin/bash
#
# automates the installation of edth.sql
# see: https://flamingo.ics.uci.edu/toolkit/
# see also: https://flamingo.ics.uci.edu/toolkit/docs/UdfDoc.html

URL="https://flamingo.ics.uci.edu/toolkit/toolkit_2021-05-18.tgz"
PLUGIN_DIR="/usr/lib/mysql/plugin"
BAD_CNT=0

if [ "$OSTYPE" != "linux-gnu" ]; then
	echo -e "\e[31merror: unsupported OS\e[0m"
	exit 1
fi

apt-get install sudo > /dev/null
sudo apt-get install -y gcc make mysql-server libmysqlclient-dev > /dev/null

if [ ! -d "${PLUGIN_DIR}" ]; then
	echo -e "\e[31mmissing dir: ${PLUGIN_DIR} - please find it\e[0m"
	echo -e "\e[33mplacing libedth.so in pwd instead\e[0m"
	PLUGIN_DIR="$(pwd)"
fi

rm -rf ./toolkit
rm -rf ./._toolkit
curl "$URL" -s -L > ./toolkit_2021-05-18.tgz
tar -xzf ./toolkit_2021-05-18.tgz
pushd toolkit/src/udf/mysql/ed > /dev/null
make libedth.so
if [ $? -eq 0 ]; then
	mv libedth.so "${PLUGIN_DIR}"
	BAD_CNT=$(($BAD_CNT + $?))
	mv edth.sql ~1
	BAD_CNT=$(($BAD_CNT + $?))
fi
popd > /dev/null
rm -rf ./toolkit_2021-05-18.tgz
rm -rf ./toolkit
rm -rf ./._toolkit

sudo /etc/init.d/mysql restart

if [ $BAD_CNT -eq 0 ]; then
	echo -e "\e[32msuccess -- please run edth.sql (no database option required)\e[0m"
else
	echo -e "\e[31mencountered error while running\e[0m"
fi
