package org.sakaiproject.nakamura.site;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import org.apache.jackrabbit.api.security.user.Group;

import javax.jcr.RepositoryException;

public class GroupKey extends AuthorizableKey {

  private Group group;
  
  public GroupKey(Group group) throws RepositoryException {
    super(group);
    this.group = group;
  }
  
  public Group getGroup() {
    return group;
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
