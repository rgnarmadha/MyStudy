#!/bin/bash

## iterate over all command line parameters after $0
for i in $*
do
  ## push the appropriate bundle dir to the path stack
  pushd bundles/$i

  ## clean and build the bundle
  mvn $BUILD_OPTS clean install

  if [[ $? -ne 0 ]]
  then
     ## exit with message if bundle compilation failed
     echo "Build of $i Failed "
     exit 10
  fi

  ## remove the bundle dir from the path stack
  popd
done

## rebundle things
mvn $BUILD_OPTS -Pbundle clean install

if [[ $? -ne 0 ]]
then
   ## exit with message if rebundling failed
   echo "Bundle Failed "
   exit 10
fi

