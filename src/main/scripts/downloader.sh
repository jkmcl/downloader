#!/bin/bash

DOWNLOADER_HOME="$(dirname "$(realpath "${BASH_SOURCE[0]}")")"

if [[ -z "$JAVA_HOME" ]]; then
	echo 'The required JAVA_HOME environment variable is not defined' >&2
	exit 1
fi

"$JAVA_HOME/bin/java" -jar "$DOWNLOADER_HOME/@project.build.finalName@.jar" "$@"
