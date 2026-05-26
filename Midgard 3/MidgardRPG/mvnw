#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.
# ----------------------------------------------------------------------------

# Determine the Java command to use
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# Project base directory
MAVEN_PROJECTBASEDIR="$(cd "$(dirname "$0")" && pwd)"

WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"

# Download wrapper jar if not present
if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Downloading Maven wrapper..."
    if command -v curl > /dev/null 2>&1; then
        curl -sL -o "$WRAPPER_JAR" "$WRAPPER_URL"
    elif command -v wget > /dev/null 2>&1; then
        wget -q -O "$WRAPPER_JAR" "$WRAPPER_URL"
    else
        echo "ERROR: curl or wget required to download Maven wrapper"
        exit 1
    fi
fi

# Run Maven via wrapper
exec "$JAVACMD" \
    -jar "$WRAPPER_JAR" \
    "$@"
