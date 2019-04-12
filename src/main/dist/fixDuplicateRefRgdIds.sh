# script env setup
#
APP_HOME=/home/rgddata/pipelines/fixDuplicateRefRgdIds
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`
EMAIL_LIST=mtutaj@mcw.edu
if [ "$SERVER" == "REED" ]; then
  EMAIL_LIST=RGD.Developers@mcw.edu
fi

$APP_HOME/_run.sh > $APP_HOME/run.log  2>&1
mailx -s "[$SERVER] Fix duplicate RGD References pipeline ran" $EMAIL_LIST < $APP_HOME/logs/summary.log
