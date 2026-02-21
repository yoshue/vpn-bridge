#!/usr/bin/env bash
##############################################################################
## Gradle start up script for UN*X
##############################################################################
export APP_BASE_NAME=`basename "$0"`
export APP_HOME=`pwd`
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
