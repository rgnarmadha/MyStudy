#!/bin/bash
#
# this script assumes that the jmeter/run.sh script has been run,
# placing a copy of jmeter 
#

[[ -n "$1" ]] || { echo "Usage: load-data.sh <server host> <server port>"; exit 1 ; }

if [ ! -d ../jakarta-jmeter-2.4 ]
then
 echo Edit this script to indicate where jmeter is installed in your environment
 exit 1
fi


HOST=$1
PORT=$2

if [ -e netids01.csv ]
then
 NUMUSERS=`wc -l netids01.csv | awk '{print $1}'`
 echo loading $NUMUSERS users to http://$HOST:$PORT
 perl ../usersfromcsv.pl netids01.csv $HOST $PORT
else
 echo "unable to find netids01.csv"
 exit 1
fi

if [ !  -e recipients.csv ]
then
   echo "unable to find message recipients file recipients.csv"
   exit 1
fi

if [ ! -e load-messages.jmx ]
then
   echo "unable to find test script load-messages.jmx"
   exit 1
fi

NUMMESSAGES=`wc -l recipients.csv | awk '{print $1}'`
echo "loading $NUMMESSAGES messages"
../jakarta-jmeter-2.4/bin/jmeter --nongui -l loadmessageslog.txt --testfile load-messages.jmx

if [ ! -e content.txt ]
then
 echo "unable to find content load file content.txt"
 exit 1
fi

if [ ! -e load-files.jmx ]
then
 echo "unable to find content loading test script load-files.jmx"
 exit 1
fi


NUMCONTENT=`wc -l content.txt | awk '{print $1}'`
echo loading $NUMCONTENT files
../jakarta-jmeter-2.4/bin/jmeter --nongui -l loadfileslog.txt --testfile load-files.jmx


if [ ! -e content.txt ]
then
 echo "unable to find user tag file user-tags.csv"
 exit 1
fi

if [ ! -e load-tags.jmx ]
then
 echo "unable to find content loading test script load-tags.jmx"
 exit 1
fi

NUMTAGS=`wc -l user-tags.csv | awk '{print $1}'`
echo loading $NUMTAGS tags
../jakarta-jmeter-2.4/bin/jmeter --nongui -l loadtagslog.txt --testfile load-tags.jmx


