#!/bin/sh
seed=`date +%y%m%d%H%M%s`
counter=1
time (
while [[ $counter -lt 2000 ]]
do
nodepath=/$seed/`date +%y/%H/%M`/$counter
curl -s  -F"sling:resourceType=sakai/activity" \
     -F"sakai:appId=sakaiMessages"   \
     -F"sakai:templateId=1234" \
     -F"userUrl=#$counter" \
     -F"user=Samantha Jones$counter" \
     -F"messageUrl=#$counter" \
     -F"messageSubject=Getting startedi$counter" \
     -F"siteName=CHEM 101-1001$counter" \
     -F"siteUrl=#$counter" \
     http://admin:admin@localhost:8080/activitytest$nodepath > /dev/null
echo $counter
let counter=counter+1
done )
