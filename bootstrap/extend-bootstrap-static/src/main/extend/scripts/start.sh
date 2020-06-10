#!/bin/sh

DIRNAME=`dirname $0`

JAVA_OPTS="-Xms128m -Xmx512m"

$JAVA_HOME/bin/java $JAVA_OPTS -cp $DIRNAME:$DIRNAME/extend-bootstrap-static.jar org.abstracthorizon.extend.server.Bootstrap
