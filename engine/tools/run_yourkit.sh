#!/bin/bash
TOOLSDIR=`dirname $0`
. ${TOOLSDIR}/version
java -Xmx512m -agentpath:/Applications/YourKit_Java_Profiler_9.0.8.app/bin/mac/libyjpagent.jnilib -XX:MaxPermSize=256m -server -jar app/target/org.sakaiproject.nakamura.app-${K2VERSION}.jar $*

