#!/bin/bash

# --------------------------
# Shutdown script for Sakai
# --------------------------


if [ -f "./sakai.pid" ]
 then
  echo ""
 else 
  clear
  echo "We can't find the sakai.pid file.  This should be in the same location"
  echo "as the startup and shutdown scripts.  You may need to kill java manually."
  echo "Make sure you are running this script directly in the folder it is located in"
  echo ""
  echo ""
  exit
fi

sakai_pid="`cat ./sakai.pid`"


kill -HUP `cat ./sakai.pid`  
rm ./sakai.pid

sleep 10
clear
echo "Sakai should be shutdown now."


