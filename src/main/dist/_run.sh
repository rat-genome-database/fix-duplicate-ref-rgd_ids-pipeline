#!/usr/bin/env bash
# shell script to run fixDuplicateRefRgdIds pipeline
. /etc/profile

APPNAME=fixDuplicateRefRgdIds
APPDIR=/home/rgddata/pipelines/$APPNAME

cd $APPDIR
pwd
DB_OPTS="-Dspring.config=$APPDIR/../properties/default_db.xml"
LOG4J_OPTS="-Dlog4j.configuration=file://$APPDIR/properties/log4j.properties"
export FIX_DUPLICATE_REF_RGD_IDS_OPTS="$DB_OPTS $LOG4J_OPTS"

bin/$APPNAME "$@"
