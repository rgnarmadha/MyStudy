#!/bin/sh
TESTS=`ls *-test.rb`
for i in $TESTS
do
echo $i `./$i | grep failure`
done
