#!/usr/bin/env sh

# Gradle Wrapper Script for Unix/Linux

# Get script location
APP_HOME="$(cd "$(dirname "$0")" && pwd)"

# Use provided JAVA_CMD or find java
if [ -z "$JAVACMD" ]; then
  if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
  else
    JAVACMD=$(which java 2>/dev/null || echo "java")
  fi
fi

# Check if java is available
$JAVACMD -version > /dev/null 2>&1
if [ $? -ne 0 ]; then
  echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
  exit 1
fi

# Set classpath
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# Execute gradle wrapper main class
exec "$JAVACMD" \
  "-Xmx64m" \
  "-Xms64m" \
  "-Dorg.gradle.appname=$(basename "$0")" \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
