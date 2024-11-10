#!/bin/bash
#
# extracts the stanford movie dataset by first
# downloading the tarball (if it doesn't exist)
# and untarring it

URL="https://canvas.eee.uci.edu/courses/63501/files/26125574/download"

if [ -d ./stanford-movies ]; then
	exit 0;
fi

if [ ! -f ./stanford-movies.tar.gz ]; then
	curl "$URL" -L > ./stanford-movies.tar.gz
fi

tar -xzvf ./stanford-movies.tar.gz
