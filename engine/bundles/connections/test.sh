#!/bin/bash
# simple script to test things are working
# forcing a login in web browser
# http://localhost:8080/?sling:authRequestLogin=1
# create a few users
curl -u admin:admin -F:name=aaron -Fpwd=aaron -FpwdConfirm=aaron http://localhost:8080/system/userManager/user.create.html
curl -u admin:admin -F:name=nico -Fpwd=nico -FpwdConfirm=nico http://localhost:8080/system/userManager/user.create.html
curl -u admin:admin -F:name=ian -Fpwd=ian -FpwdConfirm=ian http://localhost:8080/system/userManager/user.create.html
# GET http://localhost:8080/system/userManager/user/aaron.json
# now try to do a request from aaron to nico
curl -u aaron:aaron -F"toRelationships=serverside" -F"fromRelationships=clientside" -F"sakai:types=friend" -F"sakai:types=coworker" -X POST http://localhost:8080/_user/contacts/nico.invite.html
# now try to fetch the connection nodes
curl -u nico:nico http://localhost:8080/_user/contacts/find?state=INVITED
curl -u aaron:aaron http://localhost:8080/_user/contacts/find?state=PENDING
# now try to accept it
curl -u nico:nico -X POST http://localhost:8080/_user/contacts/aaron.accept.html
# now try to fetch the updated connection nodes
curl -u nico:nico  http://localhost:8080/_user/contacts/find?state=ACCEPTED
curl -u aaron:aaron  http://localhost:8080/_user/contacts/find?state=ACCEPTED

# Now try to remove the connection.
curl -u nico:nico -X POST http://localhost:8080/_user/contacts/aaron.remove.html
curl -u nico:nico  http://localhost:8080/_user/contacts/find?state=ACCEPTED
curl -u aaron:aaron  http://localhost:8080/_user/contacts/find?state=ACCEPTED

curl -u aaron:aaron http://localhost:8080/_user/contacts/5a/7a/6a/18/aaron/d6/59/c1/0e/nico.tidy.json
curl -u nico:nico http://localhost:8080/_user/contacts/d6/59/c1/0e/nico/5a/7a/6a/18/aaron.tidy.json

# Now try a new invitation, since no blocking has occurred.
curl -u aaron:aaron -F"sakai:types=ex-coworker" -X POST http://localhost:8080/_user/contacts/nico.invite.html
curl -u nico:nico -X POST http://localhost:8080/_user/contacts/aaron.accept.html

# What do we have?
curl -u nico:nico  http://localhost:8080/_user/contacts/find?state=ACCEPTED
curl -u aaron:aaron  http://localhost:8080/_user/contacts/find?state=ACCEPTED
