#!/bin/bash

#Sakai 2+3 Hybrid Nightly
# don't forget to trust the svn certificate permanently: svn info https://source.sakaiproject.org/svn

export K2_TAG="0.9"
export S2_TAG="tags/sakai-2.8.0-a04"
export UX_TAG="v_0.5.0_release"

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
export MAVEN_OPTS="-Xmx512m -XX:MaxPermSize=256m"
export JAVA_OPTS="-server -Xmx1024m -XX:MaxPermSize=512m -Djava.awt.headless=true -Dsun.lang.ClassLoader.allowArraySyntax=true -Dorg.apache.jasper.compiler.Parser.STRICT_QUOTE_ESCAPING=false -Dsakai.demo=true -Dsakai.cookieName=SAKAI2SESSIONID"
export K2_OPTS="-server -Xmx1024m -XX:MaxPermSize=256m -Djava.awt.headless=true"
BUILD_DATE=`date "+%D %R"`

# get some shell scripting setup out of the way...
# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`/"$link"
    fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`
PRGDIR=`pwd "$PRGDIR"`

# ensure logs directory exists
if [ ! -d $BUILD_DIR/logs ]
then
	mkdir -p $BUILD_DIR/logs
fi

# shutdown all running instances
killall -9 java

# Exit immediately if a simple command exits with a non-zero status
set -o errexit

# clean previous builds
cd $BUILD_DIR
if [ $# -gt 0 ]
then
    if [ $1 == "clean" ]
    then
        echo "Starting clean build..."
        rm -rf sakai
        rm -rf sakai2-demo
        rm -rf 3akai-ux
        rm -rf sakai3
        rm -rf ~/.m2/repository/org/sakaiproject
    else
        echo "Starting incremental build..."
    fi
else
    echo "Starting incremental build..."
fi

# build 3akai-ux
cd $BUILD_DIR
mkdir -p 3akai-ux
cd 3akai-ux
if [ -f .lastbuild ]
then
    echo "Skipping build 3akai-ux@$UX_TAG..."
else
    echo "Building 3akai-ux@$UX_TAG..."
    git clone -q git://github.com/sakaiproject/3akai-ux.git
    cd 3akai-ux
    git checkout -b "build-$UX_TAG" $UX_TAG
    # enable My Sakai 2 Sites widget
    # // "personalportal":true --> "personalportal":true,
    perl -pwi -e 's/\/\/\s+"personalportal"\:true/"personalportal"\:true\,/gi' devwidgets/s23courses/config.json
    # //"grouppages":true, --> "grouppages":true,
    perl -pwi -e 's/\/\/"grouppages"\:true\,/"grouppages"\:true\,/gi' devwidgets/sakai2tools/config.json
    # //"grouppages":true, --> "grouppages":true,
    perl -pwi -e 's/\/\/"grouppages"\:true\,/"grouppages"\:true\,/gi' devwidgets/basiclti/config.json
    mvn -B -e clean install
    date > ../.lastbuild
fi

# build sakai 3
cd $BUILD_DIR
mkdir -p sakai3
cd sakai3
if [ -f .lastbuild ]
then
    echo "Skipping build nakamura@$K2_TAG..."
else
    echo "Building nakamura@$K2_TAG..."
    git clone -q git://github.com/sakaiproject/nakamura.git
    cd nakamura
    git checkout -b "build-$K2_TAG" $K2_TAG
    mvn -B -e clean install
    date > .lastbuild
fi

# start sakai 3 instance
echo "Starting sakai3 instance..."
cd app/target/
K2_ARTIFACT=`find . -name "org.sakaiproject.nakamura.app*[^sources].jar"`
# configure TrustedLoginTokenProxyPreProcessor via file install
mkdir -p load
echo 'sharedSecret=e2KS54H35j6vS5Z38nK40' > load/org.sakaiproject.nakamura.proxy.TrustedLoginTokenProxyPreProcessor.cfg
echo 'port=8080' >> load/org.sakaiproject.nakamura.proxy.TrustedLoginTokenProxyPreProcessor.cfg
echo 'hostname=localhost' >> load/org.sakaiproject.nakamura.proxy.TrustedLoginTokenProxyPreProcessor.cfg
java $K2_OPTS -jar $K2_ARTIFACT -p 8008 -f - > $BUILD_DIR/logs/sakai3-run.log.txt 2>&1 &

# build sakai 2
cd $BUILD_DIR
if [ -f $BUILD_DIR/sakai/.lastbuild ]
then
    echo "Skipping build sakai2/$S2_TAG..."
else
    echo "Building sakai2/$S2_TAG..."
    # untar tomcat
    tar -xzf apache-tomcat-5.5.30.tar.gz 
    mv apache-tomcat-5.5.30 sakai2-demo
    mkdir -p sakai2-demo/sakai
    svn checkout -q "https://source.sakaiproject.org/svn/sakai/$S2_TAG" sakai
    cd sakai/
    REPO_REV=`svn info|grep Revision`
    # # SAK-17223 K2AuthenticationFilter
    # rm -rf login/
    # svn checkout -q https://source.sakaiproject.org/svn/login/branches/SAK-17223-2.7 login
    # SAK-17222 NakamuraUserDirectoryProvider
    # rm -rf providers
    # svn checkout -q https://source.sakaiproject.org/svn/providers/branches/SAK-17222-2.7 providers
    # enable NakamuraUserDirectoryProvider
    perl -pwi -e 's/<\/beans>/\t<bean id="org.sakaiproject.user.api.UserDirectoryProvider"\n\t\tclass="org.sakaiproject.provider.user.NakamuraUserDirectoryProvider"\n\t\tinit-method="init">\n\t\t<property name="threadLocalManager">\n\t\t\t<ref bean="org.sakaiproject.thread_local.api.ThreadLocalManager" \/>\n\t\t<\/property>\n\t<\/bean>\n<\/beans>/gi' providers/component/src/webapp/WEB-INF/components.xml
    mvn -B -e clean install sakai:deploy -Dmaven.tomcat.home=$BUILD_DIR/sakai2-demo
    # add hybrid webapp module
    svn checkout -q https://source.sakaiproject.org/svn/hybrid/tags/hybrid-1.1.0 hybrid
    cd hybrid
    mvn -B -e clean install sakai:deploy -Dmaven.tomcat.home=$BUILD_DIR/sakai2-demo
    # configure sakai 2 instance
    cd $BUILD_DIR
    # change default tomcat listener port numbers
    perl -pwi -e 's/\<Connector\s+port\="8080"/\<Connector port\="8880"/gi' sakai2-demo/conf/server.xml
    perl -pwi -e 's/\<Connector\s+port\="8009"/\<Connector port\="8809"/gi' sakai2-demo/conf/server.xml
    # sakai.properties
    echo "ui.service = $S2_TAG on HSQLDB" >> sakai2-demo/sakai/sakai.properties
    echo "version.sakai = $REPO_REV" >> sakai2-demo/sakai/sakai.properties
    echo "version.service = Built: $BUILD_DATE" >> sakai2-demo/sakai/sakai.properties
    echo "serverName=sakai23-hybrid.sakaiproject.org" >> sakai2-demo/sakai/sakai.properties
    echo "webservices.allowlogin=true" >> sakai2-demo/sakai/sakai.properties
    echo "webservice.portalsecret=nightly" >> sakai2-demo/sakai/sakai.properties
    echo "samigo.answerUploadRepositoryPath=/tmp/sakai2-hybrid/" >> sakai2-demo/sakai/sakai.properties
    # enable SAK-17223 NakamuraAuthenticationFilter
    echo "top.login=false" >> sakai2-demo/sakai/sakai.properties
    echo "container.login=true" >> sakai2-demo/sakai/sakai.properties
    echo "org.sakaiproject.login.filter.NakamuraAuthenticationFilter.enabled=true" >> sakai2-demo/sakai/sakai.properties
    echo "org.sakaiproject.login.filter.NakamuraAuthenticationFilter.validateUrl=http://localhost:8008/var/cluster/user.cookie.json?c=" >> sakai2-demo/sakai/sakai.properties
    # configure SAK-17222 NakamuraUserDirectoryProvider
    echo "org.sakaiproject.provider.user.NakamuraUserDirectoryProvider.validateUrl=http://localhost:8008/var/cluster/user.cookie.json?c=" >> sakai2-demo/sakai/sakai.properties
    echo "x.sakai.token.localhost.sharedSecret=default-setting-change-before-use" >> sakai2-demo/sakai/sakai.properties
    # declare shared secret for trusted login from nakamura
    echo "org.sakaiproject.util.TrustedLoginFilter.sharedSecret=e2KS54H35j6vS5Z38nK40" >> sakai2-demo/sakai/sakai.properties
    echo "org.sakaiproject.util.TrustedLoginFilter.safeHosts=localhost;127.0.0.1;129.79.26.127" >> sakai2-demo/sakai/sakai.properties
    # enabled Basic LTI provider
    echo "imsblti.provider.enabled=true" >> sakai2-demo/sakai/sakai.properties
    echo "imsblti.provider.allowedtools=sakai.forums:sakai.messages:sakai.synoptic.messagecenter:sakai.poll:sakai.profile:sakai.profile2:sakai.announcements:sakai.synoptic.announcement:sakai.assignment.grades:sakai.summary.calendar:sakai.schedule:sakai.chat:sakai.dropbox:sakai.resources:sakai.gradebook.tool:sakai.help:sakai.mailbox:sakai.news:sakai.podcasts:sakai.postem:sakai.site.roster:sakai.rwiki:sakai.syllabus:sakai.singleuser:sakai.samigo:sakai.sitestats" >> sakai2-demo/sakai/sakai.properties
    echo "imsblti.provider.12345.secret=secret" >> sakai2-demo/sakai/sakai.properties
    echo "webservices.allow=.+" >> sakai2-demo/sakai/sakai.properties
    # enable debugging for UDP
    echo "log.config.count=3" >> sakai2-demo/sakai/sakai.properties
    echo "log.config.1 = ALL.org.sakaiproject.log.impl" >> sakai2-demo/sakai/sakai.properties
    echo "log.config.2 = OFF.org.sakaiproject" >> sakai2-demo/sakai/sakai.properties
    echo "log.config.3 = DEBUG.org.sakaiproject.provider.user" >> sakai2-demo/sakai/sakai.properties
    date > $BUILD_DIR/sakai/.lastbuild
fi

# start sakai 2 tomcat
echo "Starting sakai2 instance..."
cd $BUILD_DIR/sakai2-demo
./bin/startup.sh 

# run nakamura integration tests
echo "Running integration tests..."
cd $BUILD_DIR/sakai3/nakamura
date > $BUILD_DIR/logs/sakai3-integration-tests.log.txt
./tools/runalltests.rb >> $BUILD_DIR/logs/sakai3-integration-tests.log.txt 2>&1
