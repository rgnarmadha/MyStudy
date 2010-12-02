#!/bin/sh

if [ ! -d jakarta-jmeter-2.4 ]
then
  curl http://apache.mirror.rbftpnetworks.com//jakarta/jmeter/binaries/jakarta-jmeter-2.4.tgz > jakarta-jmeter-2.4.tgz
  tar xvzf jakarta-jmeter-2.4.tgz
fi

if [ ! -d results ] 
then 
   mkdir results
fi

jakarta-jmeter-2.4/bin/jmeter -n -l results/kern-1187.jtl -t kern-1187/login_out_s3.jmx
jakarta-jmeter-2.4/bin/jmeter -n -l results/kern-1176.jtl -t kern-1176/login_assertion.jmx
