#!/usr/bin/env sh

JAR="sdp-node/target/sdp-node-1.0-SNAPSHOT.jar"

# Compile if jar is missing
if [ -f $JAR ]
then
  mvn clean package -DskipTests
fi


