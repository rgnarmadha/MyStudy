#!/bin/sh
n=`date "+%Y%m%d%H%M%S"`
x=1
while [[ $x -lt $1 ]]
do
  ((curl -F:name=g-testgroup${n}-${x} --basic -u admin:admin http://localhost:8080/system/userManager/group.create.html >/dev/null 2>/dev/null \
  && echo Created g-testgroup${n}-${x}) \
  || echo Error creating g-testgroup${n}-${x})
  let x=x+1
done

