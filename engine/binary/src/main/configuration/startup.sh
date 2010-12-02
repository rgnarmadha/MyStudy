#!/bin/bash
 K2VERSION=0.10-SNAPSHOT


# ---------------------------------
# Checking for system dependencies
# ---------------------------------

clear
echo "Checking for dependencies..."
echo ""
echo ""


if [ -f "./startup.sh" ]
 then
  echo ""
 else
  clear
  echo ""
  echo "You don't seem to be running the startup script correctly.  You must change directory ( cd ) "
  echo "to the folder it is in and run it from there."
  echo ""
  echo "i.e  cd sakai && ./startup.sh"
  echo ""
  echo ""
  echo "" 
  exit
fi



if [ -f "./system.cfg" ]
 then
  echo "Startup configuration file found"
 else
  clear
  echo "We can't seem to find your 'system.cfg' file.  This is required to be able to start Sakai."
  echo "You may need to extract your tarball again"
  exit
fi

source './system.cfg'
export java_bin



if [ -n "${java_bin:+x}" ]
 then 
  echo "Your java_bin ENV VAR seems to be set...  Now checking it's validity..."
 else
  echo "Your java_bin ENV VAR seems to be empty.  This maybe because we couldn't automatically find it.  Please edit system.cfg"
  exit
fi

if [ -f "$java_bin" ]
 then
  echo "Java found..."
 else
  echo "Java not found.  Exiting.  Check that you have java installed correctly, you may need to edit the 'java_bin' ENV VAR in system.cfg."
 exit
fi

# ---------------
# Starting Sakai 
# ---------------


clear
echo "We seem to have met all requirements, now starting Sakai."
echo ""
echo ""
echo ""


$java_bin -Xmx128m -jar lib/org.sakaiproject.nakamura.app-${K2VERSION}.jar & > /dev/null 2>&1

echo $! > sakai.pid

clear
sleep 15
clear
echo "Sakai has now started, it's current PID is `cat sakai.pid`"
echo ""

