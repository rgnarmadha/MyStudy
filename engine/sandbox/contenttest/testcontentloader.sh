#!/bin/sh
echo This script requrires that there is a server up and running on port 8080
  pushd test1
  mvn package
  popd
  pushd test2
  mvn package
  popd
if [ `curl -s -F:operation=delete http://admin:admin@localhost:8080/contenttest | grep 'deleted("/contenttest")' | wc -l` -ne 1 ]
then
   echo "Warning, Failed to delete /contenttest, might not have existed"
fi
echo "Uploading 1"
../../tools/loadbundle.sh test1/target/org.sakaiproject.nakamura.contenttest1-0.1-SNAPSHOT.jar  
sleep 1
contents1=`curl -s http://localhost:8080/contenttest.tidy.2.json `
echo "Uploading 2"
../../tools/loadbundle.sh test2/target/org.sakaiproject.nakamura.contenttest2-0.1-SNAPSHOT.jar 
sleep 1
contents2=`curl -s http://localhost:8080/contenttest.tidy.2.json `
pushd test1
mvn package
popd
echo "Uploading 1"
../../tools/loadbundle.sh test1/target/org.sakaiproject.nakamura.contenttest1-0.1-SNAPSHOT.jar 
sleep 1
contents3=`curl -s http://localhost:8080/contenttest.tidy.2.json `

if [ `echo $contents1 | grep contentTest1.txt | wc -l` -eq 1 ]
then
   echo "First Load Ok, contentTest1.txt exists"
else
   echo "First Load Failed to create file"
fi
if [ `echo $contents2 | grep contentTest1.txt | wc -l` -eq 1 ]
then
   echo "Second Load Ok, contentTest1.txt exists"
else
   echo "Second Load Failed to create file"
fi

if [ `echo $contents3 | grep contentTest1.txt | wc -l` -eq 1 ]
then
   echo "Third Load Ok, contentTest1.txt exists"
else
   echo "Third Load Failed to create file"
fi

if [ `echo $contents1 | grep contentTest2.txt | wc -l` -eq 1 ]
then 
   echo "First Load failed, contentTest2.txt exists and should not have"
else
   echo "First Load Ok, contentTest2.txt did not exist"
fi

if [ `echo $contents2 | grep contentTest2.txt | wc -l` -eq 1 ]
then 
   echo "Second Load Ok, contentTest2.txt exists"
else
   echo "Second Load Failed to create file"
fi

if [ `echo $contents3 | grep contentTest2.txt | wc -l` -eq 1 ]
then 
   echo "Third Load Ok, contentTest2.txt exists"
else
   echo "Third Load Failed to create file"
fi

function checkExists {
   file=$1
   content=$2
   if [ `curl -s http://localhost:8080/contenttest/$file | grep "$content" | wc -l ` -eq 1 ]
   then
     echo Content Ok for $file 
   else 
     echo Content Not Ok For $file
   fi 
}


checkExists "contentTest1.txt" "ContentTest1"
checkExists "contentTest2.txt" "ContentTest2" 
checkExists "contentFolder1/contentInFolder1.txt" "ContentTest1" 
checkExists "contentFolder1/contentInFolder2.txt" "ContentTest2" 
checkExists "contentFolder2/contentInFolder2.txt" "ContentTest2" 
 




