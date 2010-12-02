This bundle implements a dynamic group named "sakai:foreignPrincipal". Any user
who has successfully authenticated but who does not yet have a matching
Jackrabbit User record will be treated as a member of this group when checking
access control lists.

Possible use cases:

- Some installations may want to restrict self-registration to users who have
successfully authenticated via CAS, OpenSSO, or OpenID, with the authentication
ID stored as an Authorizable property rather than as the Authorizable ID.

- Installations which are restricted to a subset of the population authenticated
by an institutional source such as CAS. Authentication may succeed even though
the user cannot participate in the site. Automatically logging out rejects is
not a solution because logging out of the CAS server will leave the user without
any explanation. Instead, such users might be directed to a more informative page
(with a prominent logout button). 


EXAMPLE

alias curll='curl -i -b /tmp/cookieTmp -c /tmp/cookieTmp -L'

# By default, "sites" is open to the anonymous public.
curl http://localhost:8080/sites.json
# {"sling:resourceType":"sakai/sites",...}

# Instead, let's keep it to ourselves.
curl -u admin:admin -F principalId=anonymous -F privilege@jcr:read=denied http://localhost:8080/sites.modifyAce.html

curl -i http://localhost:8080/sites.json
# HTTP/1.1 404 Not Found

# Using CAS, let's log into Nakamura as a user with no Jackrabbit record.
# (Sample shell script available on request.)
...

# We have a session.
curll http://localhost:8080/system/sling/info.sessionInfo.json
{"userID":"212380","workspace":"default"}

# But we do not yet have a matching Jackrabbit Authorizable.
curl -u admin:admin http://localhost:8080/system/userManager/user/212380.json
# Page Not Found

# We can see "sites" because we're not anonymous.
curll -i http://localhost:8080/sites.json
# HTTP/1.1 200 OK

# Lock it down.
curl -u admin:admin -F principalId=sakai:foreignPrincipal -F privilege@jcr:read=denied http://localhost:8080/sites.modifyAce.html

# Try again.
curll -i http://localhost:8080/sites.json
# HTTP/1.1 404 Not Found
