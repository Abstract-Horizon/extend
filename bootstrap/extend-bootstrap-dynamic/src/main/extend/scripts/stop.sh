#!/bin/sh

DIRNAME=`dirname $0`

$JAVA_HOME/bin/java $JAVA_OPTS -cp $DIRNAME:$DIRNAME/extend-support-client.jar org.abstracthorizon.extend.server.support.Shutdown
