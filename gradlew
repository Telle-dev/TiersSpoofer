#!/bin/sh
#
# Gradle start up script for UN*X
#
APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"
GRADLE_OPTS="$GRADLE_OPTS \"-Xmx64m\" \"-Xms64m\""

warn () {
    echo "$*"
}
die () {
    echo
    echo "$*"
    echo
    exit 1
}

APP_HOME=$( cd "${APP_HOME:-./}" && pwd -P ) || exit
APP_LIB_HOME="${APP_HOME}/gradle/wrapper"

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
fi

exec "$JAVACMD" -jar "$APP_LIB_HOME/gradle-wrapper.jar" "$@"
