package org.sakaiproject.nakamura.api.site;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import org.apache.jackrabbit.api.security.user.User;
import org.sakaiproject.nakamura.api.site.SortField;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

public class UserKey extends AuthorizableKey {

  private User user;
  private Node profileNode;
  private String firstName;
  private String lastName;

  public UserKey(User user) throws RepositoryException {
    super(user);
    this.user = user;
  }

  public UserKey(User user, Node profileNode) throws RepositoryException {
    super(user);
    this.user = user;
    setProfileNode(profileNode);
  }

  /**
   * Set the profile Node
   * 
   * @param profileNode
   */
  public void setProfileNode(Node profileNode) {
    this.profileNode = profileNode;
  }

  /**
   * @return Get the User.
   */
  public User getUser() {
    return user;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.site.AuthorizableKey#getFirstName()
   */
  @Override
  public String getFirstName() {
    // Lazy load the first name.
    try {
      if (firstName == null) {
        if (profileNode == null) {
          if (user.hasProperty(SortField.firstName.toString())) {
            firstName = user.getProperty(SortField.firstName.toString())[0].getString();
          }
        } else {
          if (profileNode.hasProperty(SortField.firstName.toString())) {
            firstName = profileNode.getProperty(SortField.firstName.toString())
                .getString();
          }
        }
      }
    } catch (RepositoryException e) {
      firstName = "";
    }
    return firstName;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.site.AuthorizableKey#getLastName()
   */
  @Override
  public String getLastName() {
    // Lazy load the last name.
    try {
      if (lastName == null) {
        if (profileNode == null) {
          if (user.hasProperty(SortField.lastName.toString())) {
            lastName = user.getProperty(SortField.lastName.toString())[0].getString();
          }
        } else {
          if (profileNode.hasProperty(SortField.lastName.toString())) {
            lastName = profileNode.getProperty(SortField.lastName.toString()).getString();
          }
        }
      }
    } catch (RepositoryException e) {
      lastName = "";
    }
    return lastName;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.site.AuthorizableKey#equals(java.lang.Object)
   */
  @Override
  @SuppressWarnings(justification = "ID's are Unique, so Authorizable Equals is valid, as is hashcode ", value = { "HE_EQUALS_NO_HASHCODE" })
  public boolean equals(Object obj) {
    return super.equals(obj);
  }
}
