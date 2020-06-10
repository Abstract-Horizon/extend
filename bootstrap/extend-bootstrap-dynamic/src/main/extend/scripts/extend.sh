#!/bin/sh

DIRNAME=`dirname $0`

JAVA_OPTS="-Xms128m -Xmx512m $JAVA_OPTS "

JAVA_COMMAND=java

#! /bin/sh
# /etc/init.d/blah
#


# Carry out specific functions when asked to by the system

cd /srv/mercury-instance/

ACTION=$1
shift
ARGS="$*"

case "$ACTION" in
  start)
      echo "Starting extend"
      
      nohup >log-extend.log $JAVA_COMMAND $JAVA_OPTS -jar extend.jar start $ARGS &

      ;;
  stop)
      echo "Stopping extend"

      $JAVA_COMMAND $JAVA_OPTS -jar extend.jar shutdown $ARGS

      ;;
  *)
      echo "Usage: $0 {start|stop}"
      exit 1
      ;;
esac

exit 0
