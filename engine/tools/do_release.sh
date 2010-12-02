#!/bin/bash

#
# This script will create a release candidate and tag it after a sucessfull build,  and leave the head version on the original version.
# all git commits will be to the local checked out branch, nothing will be pushed to the central repo. 
# once the RC tag has been created dont forget to push it up to the repo with a
# git push origin master
# git push origin tag 0.2-RC2
#
# Once a vote has taken place yo can retag the RC tag with a release tag
# git tag -s 0.2 <the SHa1 of the 0.2-RC2 tag>
#
# Command line options
#
# ./do_release [from version] [to version] [Release Candidate] [test files to ignore seperated by |] 
#
# Example: ./do_release.sh 0.2 0.3 RC2 kern-483.rb|kern-330.rb 
# This will create a release for version 0.3 tag it as 0.3-RC2 and then move back to 0.2-SNAPSHOT for developers
# It will ignore kern-485 and kern-330
#

curl -f http://localhost:8080/index.html 2> /dev/null
if [[ $? -ne 7 ]]
then
   echo "There is already a server on port 8080, please stop it before attempting to perform a release" 
   portline=`lsof | grep IPv | grep http-alt`
   processnum=`echo $portline | cut -f2 -d ' '`
   echo $portline
   ps auxwww | grep $processnum
   echo kill $processnum
   exit -1
fi

if [ -f last-release/stage3 ]
then
   echo "Remove last-release/stage3 and commit the change if necessary to perform a release"
   exit -1
fi

set -o nounset
set -o errexit
cversion=$1
nversion=$2
rc=${3:-""}
ignoreTests=${4:-"__none__"}

if [[ $rc == "" ]]
then
  echo "Please only create RC versions, and versions. You can tag an RC once its accepted"
  exit -1
else
  tagversion="$cversion-$rc"
fi

echo "Creating tagged version: $cversion at tag $tagversion "

mkdir -p last-release

if [[ -f last-release/stage1 ]]
then
   echo "Release has been built, continuing ... (to start again remove last-release/stage1) "
else
  listofpoms=`find . -exec grep -l SNAPSHOT {} \;| egrep -v ".git|do_release.sh|target|binary/release|uxloader/src/main/resources|last-release|cachedir"`
  listofpomswithversion=`grep -l $cversion-SNAPSHOT $listofpoms`
  set +o errexit
  hascommits=`git status -uno | grep -c "nothing to commit"`
  if [[ $hascommits -ne 1 ]]
  then
    git status
    echo "Please commit uncommitted work before performing a release "
    exit -1
  fi
  set -o errexit
  
  
  uname -a > last-release/who

  echo "Creating Release"
  for i in $listofpomswithversion
  do
    sed "s/$cversion-SNAPSHOT/$cversion/" $i > $i.new
    mv $i.new $i
  done
  git diff > last-release/changeversion.diff
  
  echo "Remaining SNAPSHOT versions in the release"
  echo "=================================================="
  grep -C5 SNAPSHOT $listofpoms
  echo "=================================================="
  
  rm -rf ~/.m2/repository/org/sakaiproject/nakamura
  rm -rf ~/.m2/repository/org/apache/sling
  if [ -f repo.tgz ]
  then
     echo "Unpacking Repo Image ...."
     cat repo.tgz | ( cd ~/.m2/repository; tar xzf - )
  else
     echo "No Repo Image found, have you removed all SNAPSHOTS ? Create a dummy repo.tgz if you have  "
     exit
  fi
     
  mvn clean install  | tee last-release/build.log 
  mvn -PwithContrib clean install | tee last-release/contribBuild.log
  date > last-release/stage1

  echo "Build complete, preparing startup "
fi

if [[ -f last-release/stage2 ]]
then
   echo "Integration tests complete, continuing ... (to start again remove last-release/stage2 )"
else 
  rm -rf sling
  has_32_bit=`java -help | grep -c "\-d32"`
  if [[ $has_32_bit == "1" ]]
  then
    d32="-d32"
  else
    d32=""
  fi
  
  
  echo "Starting server, log in last-release/run.log"
  java  $d32 -XX:MaxPermSize=128m -Xmx512m -server -Dcom.sun.management.jmxremote -jar app/target/org.sakaiproject.nakamura.app-$cversion.jar -f - 1> last-release/run.log 2>&1 & 
  pid=`ps auxwww | grep java | grep  app/target/org.sakaiproject.nakamura.app | cut -c7-15`
  tsleep=30
  retries=0
  while [[ $tsleep -ne 0 ]]
  do
    if [[ $retries -gt 31 ]]
    then
	  echo "Too Many retries attempted trying to start K2 for testing, server left running."
          exit -1
    fi
    echo "Sleeping for $tsleep seconds while server starts ... "
    sleep $tsleep
    set +o errexit
    curl -f http://localhost:8080/index.html > /dev/null
    if [[ $? -ne 0 ]]
    then
      tsleep=10
    else
      tsleep=0
    fi
    set -o errexit
    let retries=retries+1
  done
  sleep 5
  
  echo "Server Started, running integration tests, log in last-release/integration.log"
  TESTS=`find . -name testall.sh`
  (
  set -o errexit
  for i in $TESTS
  do
          pushd `dirname $i`
          ./testall.sh  
          popd
  done 
  set +o errexit
  ) > last-release/integration.log
  date > last-release/stage2
fi

egrep -v "$ignoreTests" last-release/integration.log > last-release/integration-check.log


failures=` grep -v "0 failures" last-release/integration-check.log  | grep failures | wc -l`
errors=` grep -v "0 errors" last-release/integration-check.log  | grep errors | wc -l `
testsrun=`grep "failures" last-release/integration-check.log  | wc -l`
if [[ $testsrun -eq 0 ]]
then
   echo "No tests were run, cant perform release"
   cat last-release/integration-check.log 
   exit -1
fi
echo "$testsrun tests completed"

if [ $errors -ne 0 -o $failures -ne 0 ]
then
   echo "There were failures or errors in integration, cant perform release"
   set +o errexit
   grep -v "0 errors" last-release/integration-check.log  | grep errors
   grep -v "0 failures" last-release/integration-check.log  | grep failures
   exit -1
fi
    
echo "All Ok, release is good,  Comitting, tagging and moving on"

git add last-release
git commit -a -m "[release-script] preparing for release tag"

# Check if our new commit still works, we do all the above tests again.


set +o errexit
git tag -d $tagversion
set -o errexit
git tag -s -m "[release-script] tagging release $cversion " $tagversion HEAD
echo "Reverting pom changes."
patch -p1 -R < last-release/changeversion.diff

if [ $rc == "" ]
then
  # There was no RC provided, this means we go from 0.2-SNAPSHOT -> 0.2 (tag) -> 0.3-SNAPSHOT
  listofpoms=`find . -exec grep -l SNAPSHOT {} \;| egrep -v ".git|do_release.sh|target|binary/release|uxloader/src/main/resources|last-release|cachedir"`
  listofpomswithversion=`grep -l $cversion-SNAPSHOT $listofpoms`
  for i in $listofpomswithversion
  do
    sed "s/$cversion-SNAPSHOT/$nversion-SNAPSHOT/" $i > $i.new
    mv $i.new $i
  done
  date > last-release/stage3
  git add last-release
  git commit -a -m "[release-script] new development version"
else
  # There was an RC provided, this means we go from 0.2-SNAPSHOT -> 0.2-RCx (tag) -> 0.2-SNAPSHOT
  # We revert the previous git commit.
  git add last-release/
  git commit -a -m "[release-script] adding last-release audit logs and reverting to SNAPSHOT version "
fi




