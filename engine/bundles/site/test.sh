# A simple script for generating a set of users, groups, sites, and memberships for testing
# create some users
curl -u admin:admin -F:name=aaron -Fpwd=aaron -FpwdConfirm=aaron http://localhost:8080/system/userManager/user.create.html
curl -u admin:admin -F:name=nico -Fpwd=nico -FpwdConfirm=nico http://localhost:8080/system/userManager/user.create.html
curl -u admin:admin -F:name=ian -Fpwd=ian -FpwdConfirm=ian http://localhost:8080/system/userManager/user.create.html
# GET http://localhost:8080/system/userManager/user/aaron.json
# create some groups
curl -u admin:admin -F:name=g-group1 http://localhost:8080/system/userManager/group.create.html
curl -u admin:admin -F:name=g-group2 http://localhost:8080/system/userManager/group.create.html
curl -u admin:admin -F:name=g-group3 http://localhost:8080/system/userManager/group.create.html
# GET http://localhost:8080/system/userManager/group/g-group1.json to check it exists
# put the users in some groups
# this should be changed later on so that the /system/userManager/user/<username> prefix is not needed and <username> can be used alone
curl -u admin:admin -F:member=/system/userManager/user/aaron -F:member=/system/userManager/user/nico http://localhost:8080/system/userManager/group/g-group1.update.html
curl -u admin:admin -F:member=/system/userManager/user/aaron -F:member=/system/userManager/user/ian http://localhost:8080/system/userManager/group/g-group2.update.html
curl -u admin:admin -F:member=/system/userManager/user/nico http://localhost:8080/system/userManager/group/g-group3.update.html
# GET http://localhost:8080/system/userManager/group/g-group1.json to check if members are in the group (should see two)
# create site
curl -u admin:admin -F"sakai:title=My Site1" -F"sling:resourceType=sakai/site" -F"sakai:authorizables=g-group1" -F"sakai:authorizables=g-group2" http://localhost:8080/sites/site1
curl -u admin:admin -F"sakai:title=My Site2" -F"sling:resourceType=sakai/site" -F"sakai:authorizables=g-group2" http://localhost:8080/sites/site2
curl -u admin:admin -F"sakai:title=My Site3" -F"sling:resourceType=sakai/site" -F"sakai:authorizables=g-group3" http://localhost:8080/sites/site3
# now we should be able to get the site and see the correct info for the users (no duplicates)
# GET http://localhost:8080/sites/site1.json - check the site
# GET http://localhost:8080/sites/site1.members.json - check the site members listing
