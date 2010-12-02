/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.nakamura.user.servlet;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jackrabbit.usermanager.impl.post.AbstractAuthorizablePostServlet;
import org.apache.sling.jackrabbit.usermanager.impl.resource.AuthorizableResourceProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.api.user.UserConstants.Joinable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;

/**
 * Base class for servlets manipulating groups
 */
public abstract class AbstractSakaiGroupPostServlet extends
    AbstractAuthorizablePostServlet {
  private static final long serialVersionUID = 1159063041816944076L;
  
  /**
   * The JCR Repository we access to resolve resources
   *
   * @scr.reference
   */
  protected transient SlingRepository repository;
  private static final Logger LOGGER = LoggerFactory
      .getLogger(AbstractSakaiGroupPostServlet.class);

  /**
   * Update the group membership based on the ":member" request parameters. If the
   * ":member" value ends with @Delete it is removed from the group membership, otherwise
   * it is added to the group membership.
   *
   * @param request
   * @param authorizable
   * @throws RepositoryException
   */
  protected void updateGroupMembership(SlingHttpServletRequest request,
      Authorizable authorizable, List<Modification> changes) throws RepositoryException {
    updateGroupMembership(request, authorizable,
        SlingPostConstants.RP_PREFIX + "member", changes);
  }

  protected void updateGroupMembership(SlingHttpServletRequest request,
      Authorizable authorizable, String paramName, List<Modification> changes) throws RepositoryException {
    if (authorizable.isGroup()) {
      Group group = ((Group) authorizable);
      String groupPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX
          + group.getID();

      ResourceResolver resolver = request.getResourceResolver();
      Resource baseResource = request.getResource();
      boolean changed = false;

      UserManager userManager = AccessControlUtil.getUserManager(resolver
          .adaptTo(Session.class));

      // first remove any members posted as ":member@Delete"
      String[] membersToDelete = request.getParameterValues(paramName + SlingPostConstants.SUFFIX_DELETE);
      if (membersToDelete != null) {
        for (String member : membersToDelete) {
          Authorizable memberAuthorizable = getAuthorizable(baseResource, member,
              userManager, resolver);
          if (memberAuthorizable != null) {
            if(!UserConstants.ANON_USERID.equals(resolver.getUserID())
                && memberAuthorizable.getID().equals(resolver.getUserID())){              
              //since the current user is the member being removed,
              //we can grab admin session since user should be able to delete themselves from a group
              Session adminSession = getSession();
              try{
                UserManager adminManager = AccessControlUtil.getUserManager(adminSession);
                Group adminAuthGroup = (Group) adminManager.getAuthorizable(group.getID());
                if(adminAuthGroup != null){
                  adminAuthGroup.removeMember(memberAuthorizable);
                  changed = true;
                }

              }finally{
                ungetSession(adminSession);
              }
            }else{
              //current user is not the member being removed
              group.removeMember(memberAuthorizable);
              changed = true;
            }
          }
        }                 
      }
    

      Joinable groupJoin = getJoinable(group);

      // second add any members posted as ":member"
      String[] membersToAdd = request.getParameterValues(paramName);
      if (membersToAdd != null) {
        Group peerGroup = getPeerGroupOf(group, userManager);
        List<Authorizable> membersToRemoveFromPeer = new ArrayList<Authorizable>();
        for (String member : membersToAdd) {
          Authorizable memberAuthorizable = getAuthorizable(baseResource, member,
              userManager, resolver);
          if (memberAuthorizable != null) {
            if(!UserConstants.ANON_USERID.equals(resolver.getUserID()) 
                && Joinable.yes.equals(groupJoin) 
                && memberAuthorizable.getID().equals(resolver.getUserID())){          
              //we can grab admin session since group allows all users to join
              Session adminSession = getSession();
              try{
                UserManager adminManager = AccessControlUtil.getUserManager(adminSession);
                Group adminAuthGroup = (Group) adminManager.getAuthorizable(group.getID());
                if(adminAuthGroup != null){
                  adminAuthGroup.addMember(memberAuthorizable);
                  changed = true;
                }
              }finally{
                ungetSession(adminSession);
              }
            }else{
              //group is restricted, so use the current user's authorization
              //to add the member to the group:
              group.addMember(memberAuthorizable);
              changed = true;
            }
            if (peerGroup != null) {
              if (peerGroup.isMember(memberAuthorizable)) {
                membersToRemoveFromPeer.add(memberAuthorizable);
              }
            }
          }
        }
        if (peerGroup != null) {
          for (Authorizable member : membersToRemoveFromPeer) {
            peerGroup.removeMember(member);
          }
        }
      }
      
      if (changed) {
        // add an entry to the changes list to record the membership
        // change
        changes.add(Modification.onModified(groupPath + "/members"));
      }
    }
  }

  private Group getPeerGroupOf(Group group, UserManager userManager) throws RepositoryException {
    Group peerGroup = null;
    if (group.hasProperty(UserConstants.PROP_MANAGERS_GROUP)) {
      Value values[] = group.getProperty(UserConstants.PROP_MANAGERS_GROUP);
      String managersGroupId = values[0].getString();
      peerGroup = (Group) userManager.getAuthorizable(managersGroupId);
    } else {
      if (group.hasProperty(UserConstants.PROP_MANAGED_GROUP)) {
        Value values[] = group.getProperty(UserConstants.PROP_MANAGED_GROUP);
        String managedGroupId = values[0].getString();
        peerGroup = (Group) userManager.getAuthorizable(managedGroupId);
      }
    }
    return peerGroup;
  }

  /**
   * Gets the member, assuming its a principal name, failing that it assumes it a path to
   * the resource.
   *
   * @param member
   *          the token pointing to the member, either a name or a uri
   * @param userManager
   *          the user manager for this request.
   * @param resolver
   *          the resource resolver for this request.
   * @return the authorizable, or null if no authorizable was found.
   */
  private Authorizable getAuthorizable(Resource baseResource, String member,
      UserManager userManager, ResourceResolver resolver) {
    Authorizable memberAuthorizable = null;
    try {
      memberAuthorizable = userManager.getAuthorizable(member);
    } catch (RepositoryException e) {
      // if we can't find the members then it may be resolvable as a resource.
    }
    if (memberAuthorizable == null) {
      Resource res = resolver.getResource(baseResource, member);
      if (res != null) {
        memberAuthorizable = res.adaptTo(Authorizable.class);
      }
    }
    return memberAuthorizable;
  }

  /**
   * @param request
   *          the request
   * @param group
   *          the group
   * @param managers
   *          a list of principals who are allowed to admin the group.
   * @param changes
   *          changes made
   * @throws RepositoryException
   */
  protected void updateOwnership(SlingHttpServletRequest request, Group group,
      String[] managers, List<Modification> changes) throws RepositoryException {

    handleAuthorizablesOnProperty(request, group, UserConstants.PROP_GROUP_MANAGERS,
        SlingPostConstants.RP_PREFIX + "manager", managers);
    handleAuthorizablesOnProperty(request, group, UserConstants.PROP_GROUP_VIEWERS,
        SlingPostConstants.RP_PREFIX + "viewer", null);

  }

  /**
   * @param request
   *          The request that contains the authorizables.
   * @param group
   *          The group that should be modified.
   * @param propAuthorizables
   *          The name of the property on the group where the authorizable IDs should be
   *          added/deleted.
   * @param paramName
   *          The name of the parameter that contains the authorizable IDs. ie: :manager
   *          or :viewer. If :manager is specified, :manager@Delete will be used for
   *          deletes.
   * @param extraPrincipalsToAdd
   *          An array of authorizable IDs that should be added as well.
   * @throws RepositoryException
   */
  protected void handleAuthorizablesOnProperty(SlingHttpServletRequest request,
      Group group, String propAuthorizables, String paramName,
      String[] extraPrincipalsToAdd) throws RepositoryException {
    Set<String> principals = new HashSet<String>();
    if (group.hasProperty(propAuthorizables)) {
      Value[] existingPrincipals = group.getProperty(propAuthorizables);
      for (Value principal : existingPrincipals) {
        principals.add(principal.getString());
      }
    }

    boolean changed = false;

    // Remove all the managers that are in the :manager@Delete request parameter.
    String[] principalsToDelete = request.getParameterValues(paramName
        + SlingPostConstants.SUFFIX_DELETE);
    if (principalsToDelete != null) {
      for (String principal : principalsToDelete) {
        principals.remove(principal);
        changed = true;
      }
    }

    // Add the new ones (if any)
    String[] principalsToAdd = request.getParameterValues(paramName);
    if (principalsToAdd != null) {
      for (String principal : principalsToAdd) {
        principals.add(principal);
        changed = true;
      }
    }

    // Add the extra ones (if any.)
    if (extraPrincipalsToAdd != null) {
      for (String principal : extraPrincipalsToAdd) {
        principals.add(principal);
        changed = true;
      }
    }

    // Write the property.
    if (changed) {
      ValueFactory valueFactory = request.getResourceResolver().adaptTo(Session.class)
          .getValueFactory();
      Value[] newPrincipals = new Value[principals.size()];
      int i = 0;
      for (String principal : principals) {
        newPrincipals[i++] = valueFactory.createValue(principal);
      }
      group.setProperty(propAuthorizables, newPrincipals);
    }
  }

  /** Returns the JCR repository used by this service. */
  protected SlingRepository getRepository() {
    return repository;
  }

  /**
   * Returns an administrative session to the default workspace.
   */
  private Session getSession() throws RepositoryException {
    return getRepository().loginAdministrative(null);
  }

  /**
   * Return the administrative session and close it.
   */
  private void ungetSession(final Session session) {
    if (session != null) {
      try {
        session.logout();
      } catch (Throwable t) {
        LOGGER.error("Unable to log out of session: " + t.getMessage(), t);
      }
    }
  }
  
  /**
   * @param site
   * @return true if the authz group is joinable
   * @throws RepositoryException
   * @throws PathNotFoundException
   * @throws ValueFormatException
   */
  public Joinable getJoinable(Authorizable authorizable) {
    try {
      if (authorizable instanceof Group && authorizable.hasProperty(UserConstants.PROP_JOINABLE_GROUP)) {
        try {
          Value[] joinable = authorizable.getProperty(UserConstants.PROP_JOINABLE_GROUP);
          if (joinable != null && joinable.length > 0)
            return Joinable.valueOf(joinable[0].getString());
        } catch (IllegalArgumentException e) {
        }
      }
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
    }
    return Joinable.no;
  }

}
