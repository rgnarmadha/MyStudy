package org.sakaiproject.nakamura.site;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import org.apache.jackrabbit.api.security.user.User;
import org.sakaiproject.nakamura.api.site.SortField;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

public class UserKey extends AuthorizableKey {

  private User user;

  public UserKey(User user) throws RepositoryException {
    super(user);
    this.user = user;
  }

  public UserKey(User user, Node profileNode) throws RepositoryException {
    super(user);
    this.user = user;
    if ( profileNode == null ) {
      if (user.hasProperty(SortField.firstName.toString())) {
        setFirstName(user.getProperty(SortField.firstName.toString())[0].getString());
      }
      if (user.hasProperty(SortField.lastName.toString())) {
        setLastName(user.getProperty(SortField.lastName.toString())[0].getString());
      }
    } else {
      if (profileNode.hasProperty(SortField.firstName.toString())) {
        setFirstName(profileNode.getProperty(SortField.firstName.toString()).getString());
      }
      if (profileNode.hasProperty(SortField.lastName.toString())) {
        setLastName(profileNode.getProperty(SortField.lastName.toString()).getString());
      }
    }
  }

  public User getUser() {
    return user;
  }
  
  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.site.AuthorizableKey#equals(java.lang.Object)
   */
  @Override
  @SuppressWarnings(justification="ID's are Unique, so Authorizable Equals is valid, as is hashcode ",value={"HE_EQUALS_NO_HASHCODE"})
  public boolean equals(Object obj) {
    return super.equals(obj);
  }
}
