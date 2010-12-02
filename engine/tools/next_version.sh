#!/bin/bash

#
# This script Moves the version on after perfomring a release and tagging
# to the specified version.
#
# Command line options
#
# ./next_version [from version] [to version] [captured-snapshot-version]
# Example: ./next_version.sh 0.2 0.3 20100128
#

set -o nounset
set -o errexit
cversion=$1
nversion=$2
snapshot=${3:-"SNAPSHOT"}


echo "Moving from $cversion-SNAPSHOT to $nversion-SNAPSHOT "


listofpoms=`find . -exec grep -l SNAPSHOT {} \;| egrep -v ".git|do_release.sh|target|binary/release|uxloader/src/main/resources|last-release|cachedir"`
listofpomswithversion=`grep -l $cversion-SNAPSHOT $listofpoms`
set +o errexit
echo "Incrementing version"
for i in $listofpomswithversion
do
  sed "s/$cversion-SNAPSHOT/$nversion-SNAPSHOT/" $i > $i.new
  mv $i.new $i
done


listofsnap=`find . -exec grep -l $snapshot {} \;| egrep -v ".git|do_release.sh|target|binary/release|uxloader/src/main/resources|last-release|cachedir"`
echo "Connecting to SNAPSHOTS"
for i in $listofsnap
do
   sed "s/-$snapshot/-SNAPSHOT/" $i > $i.new
   mv $i.new $i
done

echo "Done, dont forget to commit."


