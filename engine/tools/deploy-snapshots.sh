#!/bin/bash
# This script deploys snapshots to the local repo based on the current snapshot version and creates a tarball of the repo.
# Pass it a version number to use to name the snapshots. We often use today's date, like this:
# date +"%Y%m%d"

version=$1
repo=~/.m2/repository
function install {
   if [ -f $repo/org/apache/sling/${1}/${2}-${3}/${1}-${2}-${3}.jar ]
   then
     echo Version ${1}-${2}-${3} exists.
   else
     mvn install:install-file -DgroupId=org.apache.sling -DartifactId=${1} -Dversion=${2}-${3} -Dpackaging=jar -Dfile=$repo/org/apache/sling/${1}/${2}-SNAPSHOT/${1}-${2}-SNAPSHOT.jar
   fi
}
function install_felix {
   if [ -f $repo/org/apache/felix/${1}/${2}-${3}/${1}-${2}-${3}.jar ]
   then
     echo Version ${1}-${2}-${3} exists.
   else
     mvn install:install-file -DgroupId=org.apache.felix -DartifactId=${1} -Dversion=${2}-${3} -Dpackaging=jar -Dfile=$repo/org/apache/felix/${1}/${2}-SNAPSHOT/${1}-${2}-SNAPSHOT.jar
   fi
}

install "org.apache.sling.commons.auth" "0.9.0" $version
install "org.apache.sling.commons.osgi" "2.0.7" $version
install "org.apache.sling.commons.testing" "2.0.5" $version
install "org.apache.sling.adapter" "2.0.5" $version
install "org.apache.sling.api" "2.0.9" $version
install "org.apache.sling.bundleresource.impl" "2.0.5" $version
install "org.apache.sling.engine" "2.0.7" $version
install "org.apache.sling.openidauth" "0.9.1" $version
install "org.apache.sling.jcr.contentloader" "2.0.7" $version
install "org.apache.sling.jcr.base" "2.0.7" $version
install "org.apache.sling.jcr.resource" "2.0.7" $version
install "org.apache.sling.fsresource" "1.0.1" $version
install "org.apache.sling.scripting.core" "2.0.11" $version
install "org.apache.sling.scripting.jsp" "2.0.9" $version
install "org.apache.sling.scripting.jsp.taglib" "2.0.7" $version
install "org.apache.sling.servlets.get" "2.0.9" $version
install "org.apache.sling.servlets.post" "2.0.5" $version
install "org.apache.sling.servlets.resolver" "2.0.9" $version
install "org.apache.sling.commons.log" "2.0.7" $version
install "org.apache.sling.extensions.webconsolebranding" "0.0.1" $version
install "org.apache.sling.jcr.webconsole" "1.0.0" $version
install "org.apache.sling.extensions.groovy" "1.0.0" $version
install "org.apache.sling.jcr.api" "2.0.7" $version
install "org.apache.sling.jcr.base" "2.0.7" $version
install "org.apache.sling.jcr.webdav" "2.0.9" $version
install "org.apache.sling.jcr.davex" "0.9.0" $version
install "org.apache.sling.jcr.jackrabbit.server" "2.0.7" $version
install "org.apache.sling.commons.testing" "2.0.5" $version
install "org.apache.sling.jcr.jackrabbit.usermanager" "2.0.5" $version
install "org.apache.sling.httpauth" "2.0.5" $version
install "org.apache.sling.servlets.post" "2.0.5" $version
install "org.apache.sling.launchpad.content" "2.0.5" $version
install "org.apache.sling.commons.json" "2.0.5" $version
install "org.apache.sling.jcr.jackrabbit.accessmanager" "2.0.5" $version
install "org.apache.sling.jcr.classloader" "3.1.1" $version


pushd $repo
vsearch="*${version}*"
files=`find . -type f -name $vsearch | grep -v .sha1 `
for i in $files
do 
cat $i | openssl sha1 > $i.sha1
done
files=`find . -type d -name $vsearch`
tar cvzf /tmp/repo.tgz $files
popd
mv /tmp/repo.tgz .



