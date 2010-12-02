package org.sakaiproject.nakamura.api.site;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import org.apache.jackrabbit.api.security.user.Group;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

public class GroupKey extends AuthorizableKey {

  private Group group;
  private List<AuthorizableKey> children;

  public GroupKey(Group group) throws RepositoryException {
    super(group);
    this.group = group;
    this.children = new ArrayList<AuthorizableKey>();
  }

  public GroupKey(Group group, List<AuthorizableKey> children) throws RepositoryException {
    super(group);
    this.group = group;
    setChildren(children);
  }

  public Group getGroup() {
    return group;
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

  /**
   * @param children
   *          the children to set
   */
  public void setChildren(List<AuthorizableKey> children) {
    this.children = children;
  }

  /**
   * @return the children
   */
  public List<AuthorizableKey> getChildren() {
    return children;
  }

}
