#!/bin/sh
zn=`date "+%Y%m%d%H%M%S"`
z=0
while [[ $z -lt $2 ]] 
do 
  time (
    n=$zn$z
    x=1
    while [[ $x -lt $1 ]]
    do
      ((curl -F:name=u${x}testuser${n} -Fpwd=testuser -FpwdConfirm=testuser http://admin:admin@localhost:8080/system/userManager/user.create.html >/dev/null 2>/dev/null \
      && echo Created ${x}testuser${n}) \
      || echo ERROR creating ${x}testuser${n})
      let x=x+1
    done
  ) &
 let z=z+1
done

