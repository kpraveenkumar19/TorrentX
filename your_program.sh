#!/bin/sh
set -e
mvn -q -DskipTests package
exec java -cp target/classes Main "$@"
