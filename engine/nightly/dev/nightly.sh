#!/bin/bash

#Sakai 3 Demo
export K2_TAG="HEAD"
export UX_TAG="0.4.0"

# Treat unset variables as an error when performing parameter expansion
set -o nounset

# environment
export PATH=/usr/local/bin:$PATH
export BUILD_DIR="/home/hybrid"
export JAVA_HOME=/opt/jdk1.6.0_22
export PATH=$JAVA_HOME/bin:${PATH}
export MAVEN_HOME=/usr/local/apache-maven-2.2.1
export M2_HOME=/usr/local/apache-maven-2.2.1
export PATH=$MAVEN_HOME/bin:${PATH}
export MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=512m"
export JAVA_OPTS="-server -Xmx1024m -XX:MaxPermSize=256m -Djava.awt.headless=true"
export K2_OPTS="-server -Xmx1024m -XX:MaxPermSize=256m -Djava.awt.headless=true"
BUILD_DATE=`date "+%D %R"`

# ensure logs directory exists
if [ ! -d $BUILD_DIR/logs ]
then
	mkdir $BUILD_DIR/logs
fi

# shutdown all running instances
killall -9 java

# Exit immediately if a simple command exits with a non-zero status
set -o errexit

# clean previous builds
cd $BUILD_DIR
rm -rf sakai3
rm -rf ~/.m2/repository/org/sakaiproject

# build sakai 3
echo "Building Nakamura@$K2_TAG UX@$UX_TAG..."
cd $BUILD_DIR
mkdir sakai3
cd sakai3
git clone -q git://github.com/sakaiproject/nakamura.git
cd nakamura
mvn -B -e clean install -Dux=$UX_TAG

# start sakai 3 instance
echo "Starting sakai3 instance..."
cd app/target/
K2_ARTIFACT=`find . -name "org.sakaiproject.nakamura.app*[^sources].jar"`
java $K2_OPTS -jar $K2_ARTIFACT -p 8008 -f - > $BUILD_DIR/logs/sakai3-run.log.txt 2>&1 &

# final cleanup
cd $BUILD_DIR
rm -rf ~/.m2/repository/org/sakaiproject

# run nakamura integration tests
echo "Sleeping ten minutes before running integration tests..."
sleep 600
echo "Running integration tests..."
cd $BUILD_DIR/sakai3/nakamura
date > $BUILD_DIR/logs/sakai3-integration-tests.log.txt
./tools/runalltests.rb >> $BUILD_DIR/logs/sakai3-integration-tests.log.txt 2>&1
