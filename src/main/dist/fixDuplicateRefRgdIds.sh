# script env setup
#
APP_HOME=/home/rgddata/pipelines/fixDuplicateRefRgdIds
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`
EMAIL_LIST=mtutaj@mcw.edu
if [ "$SERVER" == "REED" ]; then
  EMAIL_LIST=RGD.Developers@mcw.edu
fi

$APP_HOME/_run.sh 2>&1 > $APP_HOME/run.log
mailx -s "[$SERVER] Fix duplicate RGD References pipeline ran" $EMAIL_LIST < $APP_HOME/logs/status.log
