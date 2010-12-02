The resource bundle provides extensions to the Sling resources.

AbstractResourceTypePostProcessor provides an abstract base class to connect that 
hooks into POST operations on specific resource types. The selection of the resource 
type being made by the concrete class overriding methods in the abstract base class.

Where Sakai needs to perform an action on a POST, this method should be used, as it will re-use
all the existing Sling POST semantics and code base whilst being able to perform post processing actions 
on the result of the post. It is not possible to veto the action using this approach as the modifications
have already been committed. For that you should subscribe to Synchronous OSGi events.
 