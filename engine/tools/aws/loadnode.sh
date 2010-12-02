#!/bin/sh
#
# To use, sh loadnode.sh <full path to aws private key pem> <host name of aws instance> <nakamura instance>
# It expects to have the sun JDK the same directory as the script and the app image in your local maven repo.
# It will upload the JCK, configure it, then upload the Nakamura Jar and write a run.sh to start it.
# All you have to do then is run the run.sh as instructed.
# This is targetted at Linux Instances.


PEM_NAME=$1
AWSNAME=$2
NAK_VERSION=$3

scp -i ${PEM_NAME} jdk-1_5_0_22-linux-i586.bin  root@$AWSNAME:
scp -i ${PEM_NAME} confignode.sh  root@$AWSNAME:
scp -i ${PEM_NAME} ~/.m2/repository/org/sakaiproject/nakamura/org.sakaiproject.nakamura.app/${NAK_VERSION}/org.sakaiproject.nakamura.app-${NAK_VERSION}.jar root@$AWSNAME:
ssh -i ${PEM_NAME} root@$AWSNAME -C sh confignode.sh ${NAK_VERSION}
echo Run To start: ssh -i ${PEM_NAME} root@$AWSNAME -C sh start.sh 
echo Run To Stop: ssh -i ${PEM_NAME} root@$AWSNAME -C sh stop.sh 
echo Run To Use: Browse to http://$AWSNAME:8080 to use 

