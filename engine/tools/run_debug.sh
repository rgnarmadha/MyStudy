#!/bin/bash
TOOLSDIR=`dirname $0`
. ${TOOLSDIR}/version
has_32_bit=`java -help | grep -c "\-d32"`
if [[ $has_32_bit == "1" ]]
then 
  d32="-d32"
else 
  d32=""
fi
if [[ $1 == "--suspend" ]]
then 
  suspend=y
else
  suspend=n
fi
java $d32 -Xmx512m -server -Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=$suspend -jar app/target/org.sakaiproject.nakamura.app-${K2VERSION}.jar -f - $*

