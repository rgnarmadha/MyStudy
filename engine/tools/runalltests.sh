#!/bin/bash
TESTS=`find . -name testall.sh`
for i in $TESTS
do
	pushd `dirname $i`
	./testall.sh
	popd
done

