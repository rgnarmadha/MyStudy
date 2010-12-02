package org.sakaiproject.nakamura.files.pool;

import static javax.jcr.security.Privilege.JCR_ALL;
import static javax.jcr.security.Privilege.JCR_READ;
import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_MEMBERS_NODE;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_USER_MANAGER;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_USER_RT;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_USER_VIEWER;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.util.JcrUtils;

import java.security.Principal;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

public abstract class AbstractContentPoolServlet extends SlingAllMethodsServlet {
  private static final long serialVersionUID = -7948412093724982414L;
  protected static final String[] MANAGER_PRIVILEGES = new String[] { JCR_ALL };
  protected static final String[] VIEWER_PRIVILEGES = new String[] { JCR_READ };

  protected void addMember(Session session, String filePath, Authorizable authorizable,
      String memberType) throws RepositoryException {
    Principal principal = authorizable.getPrincipal();

    // Add (or re-use) a members node for the new viewer or manager.
    String memberPath = getMemberNodePath(filePath, authorizable);
    Node memberNode = JcrUtils.deepGetOrCreateNode(session, memberPath);
    memberNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, POOLED_CONTENT_USER_RT);
    memberNode.setProperty(memberType, new String[] { principal
        .getName() });

    // Update the member's access to the pooled content.
    refreshMemberAccess(session, filePath, principal, memberNode);
  }

  protected String getMembersPath(String filePath) {
    return filePath + POOLED_CONTENT_MEMBERS_NODE;
  }

  protected String getMemberNodePath(String filePath, Authorizable authorizable) throws RepositoryException {
    return filePath + POOLED_CONTENT_MEMBERS_NODE + PersonalUtils.getUserHashedPath(authorizable);
  }

  /**
   * Remove a User or Group from the list of viewers or the list of managers.
   * Currently, the node which points to the user/group remains in place,
   * and only the specified membership property is deleted.
   * Note that a single user/group can be on both lists, and so their
   * access rights might not actually change as a result of being removed
   * from a single list.
   *
   * @param session
   * @param filePath
   * @param authorizable
   * @param memberType
   * @throws RepositoryException
   */
  protected void removeMember(Session session, String filePath, Authorizable authorizable,
      String memberType) throws RepositoryException {
    String memberPath = getMemberNodePath(filePath, authorizable);

    // Is there actually such a member?
    if (session.itemExists(memberPath)) {
      Node memberNode = session.getNode(memberPath);
      if (memberNode.hasProperty(memberType)) {
        // Remove the property.
        memberNode.setProperty(memberType, (Value[]) null);

        // Update ACEs that refer to this member.
        Principal principal = authorizable.getPrincipal();
        refreshMemberAccess(session, filePath, principal, memberNode);
      }
    }
  }

  protected void refreshMemberAccess(Session session, String filePath,
      Principal principal, Node memberNode) throws RepositoryException {
    // Determine the new privileges for this principal.
    String[] privileges;
    if (memberNode.hasProperty(POOLED_CONTENT_USER_MANAGER)) {
      privileges = MANAGER_PRIVILEGES;
    } else if (memberNode.hasProperty(POOLED_CONTENT_USER_VIEWER)) {
      privileges = VIEWER_PRIVILEGES;
    } else {
      privileges = null;
    }

    // Completely replace any existing privileges for this principal.
    String[] removeAll = {JCR_ALL};
    AccessControlUtil.replaceAccessControlEntry(session, filePath, principal,
        privileges, null, removeAll, null);

    // We also need to set rights on the members node itself to avoid conflicts
    // with an ACE that keeps the EveryonePrincipal from seeing the list
    // of viewers and managers.
    AccessControlUtil.replaceAccessControlEntry(session, getMembersPath(filePath),
        principal, privileges, null, removeAll, null);
  }

}