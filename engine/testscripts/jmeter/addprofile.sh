#!/bin/sh

host=$2
port=$3
n=0
for i in `cat $1`
do
   id=`echo $i | cut -f1 -d',' `
   pass=`echo $i | cut -f2 -d',' `
   firstName=`echo $i | cut -f3 -d',' `
   lastName=`echo $i | cut -f4 -d',' `
   path=`curl -s http://admin:admin@$host:$port/system/userManager/user/$id.json | cut -f4 -d'"'`
   curl -s -F:operation=copy -F:dest=/_user$path/pages -F:replace=true http://admin:admin@$host:$port/var/templates/site/defaultuser > /dev/null 2>&1 
   curl -s -Fvalue=$firstName http://admin:admin@$host:$port/~$id/public/authprofile/basic/elements/firstName > /dev/null 2>&1 
   curl -s -Fvalue=$lastName http://admin:admin@$host:$port/~$id/public/authprofile/basic/elements/lastName > /dev/null 2>&1 
   echo "Done $id $n"
   let n=n+1
done
