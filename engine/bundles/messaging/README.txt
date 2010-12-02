Message Bundle

This bundle manages messages.

It stores messages under the users /_user/message space in a folder

/_user/message/.../<userid>

where ... is a BigStore hash.

This is setup when the user is created, with a resourceType of sakai/messagestore, 

Individual messages are stored within this subtree in the form
/_user/message/.../<userid>/messages/.../<messageid>

These JCR storage paths are mapped to the URL space

/_user/message/<messageid>

All the REST URL's request dispatch to the standard Sling servlets so respond 
to all the form properties.

Create a New Message
POST to 
/_user/message.create.html
with
sakai:type - the message type.
sakai:to - the recipient of the message
sakai:read - is set to true
sakai:from - who the message is from

The POST url may create other properties as desired.
When the node is created it is given a resourcetype of sakai/message

This creates the message and sends a json response containing the path and id of the message,

{
  "id":"5bb73d71dc302f59d096fc29ac364ad110d447bc", 
  "message": {
      "sakai:sendstate":"pending", 
      "sakai:messagebox":"drafts", 
      "sakai:from":"aaron1246420381", 
      "sakai:to":"nico1246420381", 
      "sakai:read":"true", 
      "sling:resourceType":"sakai/message", 
      "jcr:created":"2009-07-01T04:53:01.980+01:00", 
      "sakai:type":"internal", 
      "jcr:primaryType":"sling:Folder"
      }
}

Further posts to the URL will update properties on the message node.

Sending:
Sending is achieved by changing the box of the message to outbox with a 
state of pending or none.

eg
POST to the message with 
sakai:messagebox = outbox
sakai:state = pending

---------------------------------------------------
Implementation Note:
On post in MessagePostProcessor when a node state is updated if the box is 
outbox and the state is none or pending then an OSGi event at 
org/sakaiproject/nakamura/message/pending with the property location set to 
the path of the node.

A OSGi Event listener, MessageSentListener picks this event up and dispatches 
it on sakai:type to a handler registered against that type.

This is picked up by listeners to perform the real send
---------------------------------------------------

Message Handlers.
Message Handlers listen to the OSGi events, there are 2 at the moment.
InternalMessageHandler and ChatMessageHandler
Message handlers bind to the sakai:type property on the node




Message Node properties
sakai:type - the type of the message (see MessageConstants, internal, chat)
sakai:messagebox - the box where the message is stored ( outbox, inbox )
sakai:sendstate - the state of the message (none, pending {pending a send}, notified {notified of send})
sakai:previousmessage - the path to the previous message that this responded to 
sakai:body - the body of the message
sakai:subject - the subject of the message
sakai:from - who the message was from (a Authorizable ID)
sakai:to - who the message is to. ( comma separated list of Authorizable id's)
sakai:read - true if this message has been read.


Types (sling:resourceType), these are set by the system.
sakai/messagestore - resource type for a message store 
sakai/message - resource type for a message


Message Type "internal"
For each recipient in the sakai:to list, creates a new node in the target users 
message store and copies the node properties to that location.
Filters any properties starting with jcr: (a jcr copy is not being used apparently due to
problems with path not found exceptions)


Message Type "chat"
Looks like its identical to the handling of "internal" messages.
There is a chat cleaner which 


Listing Boxes.
Listing of boxes is achieved by search operations.
At the moment we have the following sets
/_user/message/all.json  
   Lists all internal messages with sort order.
   parameter "sortOn" is the sort of the messages, defaults to jcr:created
   parameter "sortOrder" is the sort order, defaults to descending
/_user/message/box.json
   adds the parameter "box" which is the value of the sakai:messagebox
   (
     there is also a {path} parameter which does not make sense, IMHO should be {_userPrivate}
     also type = message which looks wrong
   )
/_user/message/from.json
   as all.json, adds parameter "from" to search by the sender.
   (
     also type = message which looks wrong
   )

/_user/message/messageFilter.json
   allows filtering on a property "prop" with a value of "val" 
   (
     there is also a {path} parameter which does not make sense, IMHO should be {_userPrivate}
   )
/_user/message/to.json
   as all, allows searching for "to" 


There are also a similar set for types of chat and internal.




