#!/bin/sh
# This git will recapture the snapshots based on a commit that contains the changes to snapshots
# use recapturesnapshot.sh 20100823 b5e3f578d2f7aa992dc84c37f4682137b4b83dad
# you can do git diff b5e3f578d2f7aa992dc84c37f4682137b4b83dad^1...b5e3f578d2f7aa992dc84c37f4682137b4b83dad to check what the set will be.

repo=~/.m2/repository

function install {


   if [ ! -f $repo/org/apache/sling/${1}/${2}-SNAPSHOT/${1}-${2}-SNAPSHOT.jar ] || [ ! -f $repo/org/apache/sling/${1}/${2}-SNAPSHOT/${1}-${2}-SNAPSHOT-sources.jar ]
   then
cat > getsnap.xml << EOF
<?xml version="1.0" encoding="ISO-8859-1"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>org.sakaiproject.nakamura</groupId>
  <artifactId>capture-snapshots</artifactId>
  <packaging>pom</packaging>
  <version>0.1-SNAPSHOT</version>
  <name>Sakai Nakamura :: Temp Download Snapshots Sources</name>
  <repositories>
    <repository>
      <id>maven repo</id>
      <name>maven repo</name>
      <url>http://repo1.maven.org/maven2/</url>
      <snapshots>
        <enabled>false</enabled>
      </snapshots>
      <releases>
        <enabled>true</enabled>
      </releases>
    </repository>
    <repository>
      <id>apache-snapshots</id>
      <name>Apache Snapshot Repository</name>
      <url>http://repository.apache.org/snapshots/</url>
      <snapshots>
        <enabled>true</enabled>
      </snapshots>
      <releases>
        <enabled>false</enabled>
      </releases>
    </repository>
    <repository>
      <id>sakai-releases</id>
      <name>Sakai Releases</name>
      <releases>
        <enabled>true</enabled>
      </releases>
      <snapshots>
        <enabled>false</enabled>
      </snapshots>
      <url>http://source.sakaiproject.org/maven2/</url>
    </repository>
    <repository>
      <id>sakai-snapshots</id>
      <name>Sakai Snapshots</name>
      <layout>default</layout>
      <url>http://source.sakaiproject.org/maven2-snapshots</url>
      <releases>
        <enabled>false</enabled>
      </releases>
      <snapshots>
        <enabled>true</enabled>
      </snapshots>
    </repository>
  </repositories>
  <dependencies>
    <dependency>
       <groupId>org.apache.sling</groupId>
       <artifactId>$1</artifactId>
       <version>$2-SNAPSHOT</version>
    </dependency>
  </dependencies>
</project>
EOF
      mvn -f getsnap.xml dependency:sources
   fi
  

   if [ -f $repo/org/apache/sling/${1}/${2}-${3}/${1}-${2}-${3}.jar ]
   then
     echo Version ${1}-${2}-${3} exists.
   else
     pushd tools
     mvn install:install-file -DgroupId=org.apache.sling -DartifactId=${1} -Dversion=${2}-${3} -Dpackaging=jar -DcreateChecksum=true -DgeneratePom=true -Dfile=$repo/org/apache/sling/${1}/${2}-SNAPSHOT/${1}-${2}-SNAPSHOT.jar
     popd 
   fi
   if [ -f $repo/org/apache/sling/${1}/${2}-${3}/${1}-${2}-${3}-sources.jar ]
   then
     echo Version ${1}-${2}-${3}-sources exists.
   else
     pushd tools
     mvn install:install-file -DgroupId=org.apache.sling -DartifactId=${1} -Dversion=${2}-${3} -Dpackaging=jar -DcreateChecksum=true -Dclassifier=sources -Dfile=$repo/org/apache/sling/${1}/${2}-SNAPSHOT/${1}-${2}-SNAPSHOT-sources.jar
     popd
   fi

}




set -o nounset
#set -o errexit
capture=$1
gitsha=$2

versionstocapture=`git diff $gitsha~1...$gitsha | awk '/artifactId/ { artifact=$1 }; /groupId/ { group=$1 }; /SNAPSHOT/ { if ( artifact ) print substr(group,10,length(group)-19) ":" substr(artifact,13,length(artifact)-25) ":"  substr($2,10,length($2)-28) }; ' | sort -u `


for i in $versionstocapture
do
   group=`echo $i | cut -d':' -f1`
   artifact=`echo $i | cut -d':' -f2`
   version=`echo $i | cut -d':' -f3`
   if [ "a$group" == "aorg.apache.sling"  ]
   then
     echo Installing $i
     install "$artifact" "$version" "$capture"
   else
    echo Skipping $i
   fi
done

pushd $repo
tar cvzf snapshots-capture-$capture.tgz ` find org/apache/sling -type d -name "*$capture*"`
popd


echo All done, there is a tarball at $repo/snapshots-capture-$capture.tgz ready to untar on the maven repo.
  

