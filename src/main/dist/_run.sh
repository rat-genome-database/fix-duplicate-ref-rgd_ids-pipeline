#!/usr/bin/env bash
# shell script to run fixDuplicateRefRgdIds pipeline
. /etc/profile

APPNAME="fix-duplicate-ref-rgd-ids-pipeline"
APPDIR=/home/rgddata/pipelines/$APPNAME

cd $APPDIR
java -Dspring.config=$APPDIR/../properties/default_db2.xml \
    -Dlog4j.configurationFile=file://$APPDIR/properties/log4j2.xml \
    -jar lib/${APPNAME}.jar "$@"
