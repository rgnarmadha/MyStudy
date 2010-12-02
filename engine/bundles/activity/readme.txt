Activity feed notes
-------------------

This bundles provides the following functionality:

1) Create an activity

Triggered by doing a POST to node.activity.json with 2 parameters
 - applicationId
 - templateId
 
 It the checks if the resource has a child called 'activity'
 If it doesn't it creates one with a sling:resourceType of 'sakai/activityStore'
 
 It then creates an item stored with a hash where the name is a random UUID.
 It then forwards the request to that newly created item.
 After this it sets the property sakaiActivityFeedActor on the node with the current user as value.
 Finally it launches an OSGi event with the path to this node.
 
 2) Handle Activity events
 This is an OSGI event handler which listens for activity events. (AFAICT these only get launched in the createSiteServlet)
 It retrieves all the connections of the actor (the user who triggered the event).
 In the private space of those connections there is a node activityFeed which is a BigStore
   sling:resourceType = sakai/activityFeed
 The activity item get's copied over to this bigstore
 
 3) Servlet's to expand bigstores.