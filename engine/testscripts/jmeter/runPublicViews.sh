#!/bin/sh
RESULTSFILENAME=PublicViews.jtl

if [ ! -d jakarta-jmeter-2.4 ]
then
  echo 'Pulling down jakarta jmeter version 2.4'
  curl http://apache.mirror.rbftpnetworks.com//jakarta/jmeter/binaries/jakarta-jmeter-2.4.tgz > jakarta-jmeter-2.4.tgz
  tar xvzf jakarta-jmeter-2.4.tgz
fi

if [ ! -d results ]
then
  mkdir results
fi

if [ -f results/${RESULTSFILENAME} ]
then
  echo 'Results will be appended to existing' ${RESULTSFILENAME}
fi

jakarta-jmeter-2.4/bin/jmeter -n -l results/${RESULTSFILENAME} -t PublicViews/Public_Content_Views.jmx
