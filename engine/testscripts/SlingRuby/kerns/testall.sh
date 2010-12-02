#!/bin/sh
TESTS=`ls kern-*.rb`
for i in $TESTS
do
echo $i `./$i | grep failure`
done
