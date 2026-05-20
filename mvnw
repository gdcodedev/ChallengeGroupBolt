#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup batch script, version 3.2.0
# ----------------------------------------------------------------------------

# If JAVA_HOME is set, use it directly
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# Download maven-wrapper.jar if not present
MAVEN_WRAPPER_JAR=".mvn/wrapper/maven-wrapper.jar"
if [ ! -f "$MAVEN_WRAPPER_JAR" ]; then
    MAVEN_WRAPPER_URL=$(grep wrapperUrl .mvn/wrapper/maven-wrapper.properties | cut -d= -f2)
    echo "Downloading Maven wrapper jar from $MAVEN_WRAPPER_URL..."
    if command -v curl > /dev/null; then
        curl -o "$MAVEN_WRAPPER_JAR" "$MAVEN_WRAPPER_URL"
    elif command -v wget > /dev/null; then
        wget -O "$MAVEN_WRAPPER_JAR" "$MAVEN_WRAPPER_URL"
    fi
fi

exec "$JAVACMD" -classpath "$MAVEN_WRAPPER_JAR" \
    "-Dmaven.multiModuleProjectDirectory=$PWD" \
    org.apache.maven.wrapper.MavenWrapperMain "$@"
