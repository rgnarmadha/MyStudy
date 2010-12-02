package org.sakaiproject.nakamura.site;

import org.apache.jackrabbit.api.security.user.Authorizable;

import javax.jcr.RepositoryException;

public class AuthorizableKey {

  private String id;
  private Authorizable authorizable;
  private String firstName = "";
  private String lastName = "";

  public AuthorizableKey(Authorizable authorizable) throws RepositoryException {
    this.id = authorizable.getID();
    this.authorizable = authorizable;
  }
  
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof AuthorizableKey)) 
      return false;
    return ((AuthorizableKey)obj).getID().equals(getID());
  }

  private String getID() {
    return id;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  public Authorizable getAuthorizable() {
    return authorizable;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getLastName() {
    return lastName;
  }
}
