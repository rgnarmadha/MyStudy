#!/bin/sh
n=`date "+%Y%m%d%H%M%S"`
user1=testuser${n}-1
user2=testuser${n}-2
group1=g-testgroup${n}-1
group2=g-testgroup${n}-2
group3=g-testgroup${n}-3
echo "==================================================== create a users"
curl -F:name=${user1} -Fpwd=testuser -FpwdConfirm=testuser http://localhost:8080/system/userManager/user.create.html  2> /dev/null | grep 'id="Status"'
echo " "
curl -F:name=${user2} -Fpwd=testuser -FpwdConfirm=testuser http://localhost:8080/system/userManager/user.create.html  2> /dev/null | grep 'id="Status"'
echo " "
echo "====================================================  create groups "
curl -F:name=${group1} http://admin:admin@localhost:8080/system/userManager/group.create.html  2> /dev/null | grep 'id="Status"'
echo " "
curl -F:name=${group2} http://admin:admin@localhost:8080/system/userManager/group.create.html  2> /dev/null | grep 'id="Status"'
echo " "
curl -F:name=${group3} http://admin:admin@localhost:8080/system/userManager/group.create.html  2> /dev/null | grep 'id="Status"'
echo " "
echo "====================================================  make groups members "
curl -F:member=/system/userManager/group/${group2} http://admin:admin@localhost:8080/system/userManager/group/${group1}.update.html  2> /dev/null | grep 'id="Status"'
echo " " 
curl -F:member=/system/userManager/group/${group3} http://admin:admin@localhost:8080/system/userManager/group/${group2}.update.html  2> /dev/null | grep 'id="Status"'
echo " "
echo "====================================================  make users members of groups"
curl -F:member=/system/userManager/group/${user1} http://admin:admin@localhost:8080/system/userManager/group/${group1}.update.html  2> /dev/null | grep 'id="Status"'
echo " "
curl -F:member=/system/userManager/group/${user2} http://admin:admin@localhost:8080/system/userManager/group/${group2}.update.html  2> /dev/null | grep 'id="Status"'
echo " "
echo "====================================================  check the groups are members"
curl http://admin:admin@localhost:8080/system/userManager/group/${group1}.json
echo " "
curl http://admin:admin@localhost:8080/system/userManager/group/${group2}.json
echo " "
echo "====================================================  look at the users"
curl http://admin:admin@localhost:8080/system/userManager/user/${user1}.json
echo " "
curl http://admin:admin@localhost:8080/system/userManager/user/${user2}.json
echo " "
echo "====================================================  Create a Site"
curl -Fsling:resourceType=sakai/site http://admin:admin@localhost:8080/physics101/semicon
echo " "
echo "====================================================  Authorize Group 1"
curl -Faddauth=${group1} http://admin:admin@localhost:8080/physics101/semicon.authorize.html
echo " "
echo "====================================================  list members"
curl http://admin:admin@localhost:8080/physics101/semicon.members.json
echo " "
echo "====================================================  list membership of user 1"
curl http://${user1}:testuser@localhost:8080/system/sling/membership.json
echo " "
echo "====================================================  list membership of user 1"
curl http://${user2}:testuser@localhost:8080/system/sling/membership.json
echo " "
echo "====================================================  Final Group 1 "
curl http://admin:admin@localhost:8080/system/userManager/group/${group1}.json
echo " "
echo "====================================================  Final Group 2 "
curl http://admin:admin@localhost:8080/system/userManager/group/${group2}.json
echo " "
echo "====================================================  Final Group 3 "
curl http://admin:admin@localhost:8080/system/userManager/group/${group3}.json
echo " "
