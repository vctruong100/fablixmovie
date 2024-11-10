#!/bin/bash
#
# executes a main class using maven
# takes in a main class argument with variadic args
# as program args

if [ -z $(command -v mvn) ]; then
	echo "mvn: command not found"
	exit 1
fi

if [ $# -eq 0 ]; then
	echo "error: did not specify mainClass"
	exit 1
fi

mainClass="$1"
shift
args="$@"

set -x
mvn clean
mvn compile
mvn exec:java -Dexec.cleanupDaemonThreads=false -Dexec.mainClass="${mainClass}" -Dexec.args="${args}"
