#! /usr/bin/env bash

BASE_DIR=`dirname "$0"`/..
BASE_DIR=`cd "$BASE_DIR"; pwd`
echo "BASE_DIR=$BASE_DIR"

JAVA=""
if [ "$JAVA_HOME" != "" ]; then
    JAVA=$JAVA_HOME/bin/java
else
   echo "JAVA_HOME must be set."
   exit 1
fi

#Set the classpath

if [ "$CLASSPATH" != "" ]; then
   CLASSPATH=${CLASSPATH}:$JAVA_HOME/lib/tools.jar
else
   CLASSPATH=$JAVA_HOME/lib/tools.jar
fi

for f in $BASE_DIR/target/*.jar; do
   CLASSPATH=${CLASSPATH}:$f
done

for f in $BASE_DIR/lib/*.jar; do
   CLASSPATH=${CLASSPATH}:$f
done

#echo "CLASSPATH=$CLASSPATH"

exec "$JAVA" -classpath "$CLASSPATH" -Dkinetic.console.home=$BASE_DIR -Dkinetic.rest.home=$BASE_DIR com.seagate.kinetic.console.KineticConsole "$@"