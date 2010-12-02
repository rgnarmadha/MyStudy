/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.personal;

import static javax.jcr.security.Privilege.JCR_ALL;
import static javax.jcr.security.Privilege.JCR_READ;
import static javax.jcr.security.Privilege.JCR_WRITE;
import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.sakaiproject.nakamura.api.personal.PersonalConstants.VISIBILITY_LOGGED_IN;
import static org.sakaiproject.nakamura.api.personal.PersonalConstants.VISIBILITY_PRIVATE;
import static org.sakaiproject.nakamura.api.personal.PersonalConstants.VISIBILITY_PUBLIC;
import static org.sakaiproject.nakamura.api.user.UserConstants.GROUP_HOME_RESOURCE_TYPE;
import static org.sakaiproject.nakamura.api.user.UserConstants.PROP_AUTHORIZABLE_PATH;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_HOME_RESOURCE_TYPE;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.contentloader.ContentImporter;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.jcr.JCRConstants;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.user.AuthorizableEvent.Operation;
import org.sakaiproject.nakamura.api.user.AuthorizableEventUtil;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import javax.jcr.version.VersionException;

/**
 * This PostProcessor listens to post operations on User objects and processes the
 * changes.
 *
 */
@Component(immediate = true, metatype = true)
@Service(value = AuthorizablePostProcessor.class)
@Properties(value = {
    @org.apache.felix.scr.annotations.Property(name = "service.vendor", value = "The Sakai Foundation"),
    @org.apache.felix.scr.annotations.Property(name = "service.description", value = "Post Processes User and Group operations"),
    @org.apache.felix.scr.annotations.Property(name = "service.ranking", intValue=0)})
public class PersonalAuthorizablePostProcessor implements AuthorizablePostProcessor {

  @org.apache.felix.scr.annotations.Property(description = "The default access settings for the home of a new user or group.",
    value = VISIBILITY_PUBLIC,
    options = {
      @PropertyOption(name = VISIBILITY_PRIVATE, value = "The home is private."),
      @PropertyOption(name = VISIBILITY_LOGGED_IN, value = "The home is blocked to anonymous users; all logged-in users can see it."),
      @PropertyOption(name = VISIBILITY_PUBLIC, value = "The home is completely public.")
    }
  )
  static final String VISIBILITY_PREFERENCE = "org.sakaiproject.nakamura.personal.visibility.preference";
  static final String VISIBILITY_PREFERENCE_DEFAULT = VISIBILITY_PUBLIC;

  public static final String PROFILE_IMPORT_TEMPLATE_DEFAULT = "{'basic':{'elements':{'firstName':{'value':'@@firstName@@'},'lastName':{'value':'@@lastName@@'},'email':{'value':'@@email@@'}},'access':'everybody'}}";
  @org.apache.felix.scr.annotations.Property
  static final String PROFILE_IMPORT_TEMPLATE = "sakai.user.profile.template.default";
  private String defaultProfileTemplate;

  private ArrayList<String> profileParams = new ArrayList<String>();

  @Reference
  private ProfileService profileService;

  @Reference
  private EventAdmin eventAdmin;

  @Reference(policy=ReferencePolicy.DYNAMIC, cardinality=ReferenceCardinality.OPTIONAL_UNARY)
  protected ContentImporter contentImporter;

  private String visibilityPreference;

  private static final Logger LOGGER = LoggerFactory
      .getLogger(PersonalAuthorizablePostProcessor.class);

  @Activate @Modified
  protected void modified(Map<?, ?> props) {
    visibilityPreference = OsgiUtil.toString(props.get(VISIBILITY_PREFERENCE),
        VISIBILITY_PREFERENCE_DEFAULT);
    defaultProfileTemplate = OsgiUtil.toString(props.get(PROFILE_IMPORT_TEMPLATE),
        PROFILE_IMPORT_TEMPLATE_DEFAULT);

    int startPos = defaultProfileTemplate.indexOf("@@");
    while (startPos > -1) {
      int endPos = defaultProfileTemplate.indexOf("@@", startPos + 2);
      if (endPos > -1) {
        String param = defaultProfileTemplate.substring(startPos + 2, endPos);
        profileParams.add(param);

        endPos = defaultProfileTemplate.indexOf("@@", endPos + 2);
      }
      startPos = endPos;
    }
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor#process(org.apache.jackrabbit.api.security.user.Authorizable, javax.jcr.Session, org.apache.sling.servlets.post.Modification, java.util.Map)
   */
  public void process(Authorizable authorizable, Session session, Modification change,
      Map<String, Object[]> parameters) throws Exception {
    if (!ModificationType.DELETE.equals(change.getType())) {
      LOGGER.debug("Processing  {} ", authorizable.getID());
      try {
        if (ModificationType.CREATE.equals(change.getType())) {
          createHomeFolder(session, authorizable, change, parameters);
        } else {
          updateHomeFolder(session, authorizable, change, parameters);
        }
        fireEvent(session, authorizable.getID(), change);
        LOGGER.debug("DoneProcessing  {} ", authorizable.getID());
      } catch (Exception ex) {
        LOGGER.error("Post Processing failed " + ex.getMessage(), ex);
      }
    }
  }

  /**
   * @param authorizable
   * @param changes
   * @throws RepositoryException
   * @throws ConstraintViolationException
   * @throws LockException
   * @throws VersionException
   * @throws PathNotFoundException
   */
  private void updateProfileProperties(Session session, Node profileNode,
      Authorizable authorizable, Modification change, Map<String, Object[]> parameters)
      throws RepositoryException {
    if (profileNode == null) {
      return;
    }

    // The current session does not necessarily have write access to
    // the Profile.
    if (isAbleToModify(session, profileNode.getPath())) {
      // If the client sent a parameter specifying new Profile content,
      // apply it now.
      String defaultProfile = processProfileParameters(defaultProfileTemplate,
          authorizable, parameters);
      ProfileImporter.importFromParameters(profileNode, parameters, contentImporter,
          session, defaultProfile);

      // build a blacklist set of properties that should be kept private
      Set<String> privateProperties = new HashSet<String>();
      if (profileNode.hasProperty(UserConstants.PRIVATE_PROPERTIES)) {
        Value[] pp = profileNode.getProperty(UserConstants.PRIVATE_PROPERTIES).getValues();
        for (Value v : pp) {
          privateProperties.add(v.getString());
        }
      }
      // copy the non blacklist set of properties into the users profile.
      if (authorizable != null) {
        // explicitly add protected properties form the user authorizable
        if (!authorizable.isGroup() && !profileNode.hasProperty("rep:userId")) {
          profileNode.setProperty("rep:userId", authorizable.getID());
        }
        Iterator<?> inames = authorizable.getPropertyNames();
        while (inames.hasNext()) {
          String propertyName = (String) inames.next();
          // No need to copy in jcr:* properties, otherwise we would copy over the uuid
          // which could lead to a lot of confusion.
          if (!propertyName.startsWith("jcr:") && !propertyName.startsWith("rep:")) {
            if (!privateProperties.contains(propertyName)) {
              Value[] v = authorizable.getProperty(propertyName);
              if (!(profileNode.hasProperty(propertyName) && profileNode.getProperty(
                  propertyName).getDefinition().isProtected())) {
                if (v.length == 1) {
                  profileNode.setProperty(propertyName, v[0]);
                } else {
                  profileNode.setProperty(propertyName, v);
                }
              }
            }
          } else {
            LOGGER.debug("Not Updating {}", propertyName);
          }
        }
      }
    }
  }

  private String processProfileParameters(final String defaultProfileTemplate,
      final Authorizable auth, final Map<String, Object[]> parameters)
      throws RepositoryException {
    String retval = defaultProfileTemplate;
    for (String param : profileParams) {
      String val = "unknown";
      if (parameters.containsKey(param)) {
        val = (String) parameters.get(param)[0];
      } else if (auth.hasProperty(param)) {
        val = auth.getProperty(param)[0].getString();
      }
      retval = StringUtils.replace(retval, "@@" + param + "@@", val);
    }
    return retval;
  }

  /**
   * Creates the home folder for a {@link User user} or a {@link Group group}. It will
   * also create all the subfolders such as private, public, ..
   *
   * @param session
   * @param authorizable
   * @param isGroup
   * @param change
   * @throws RepositoryException
   */
  private void createHomeFolder(Session session, Authorizable authorizable,
      Modification change, Map<String, Object[]> parameters) throws RepositoryException {
    String homeFolderPath = profileService.getHomePath(authorizable);

    Node homeNode = JcrUtils.deepGetOrCreateNode(session, homeFolderPath);
    if (homeNode.isNew()) {
      LOGGER.debug("Created Home Node for {} at   {} user was {} ", new Object[] {
          authorizable.getID(), homeNode, session.getUserID() });
    } else {
      LOGGER.debug("Existing Home Node for {} at   {} user was {} ", new Object[] {
          authorizable.getID(), homeNode, session.getUserID() });
    }

    if ( !UserConstants.ANON_USERID.equals(authorizable.getID()) ) {
      initializeAccess(homeNode, session, authorizable);
    }

    refreshOwnership(session, authorizable, homeFolderPath);

    // add things to home
    decorateHome(homeNode, authorizable);

    // Create the public, private, authprofile
    createPrivate(session, authorizable);
    createPublic(session, authorizable);
    Node profileNode = createProfile(session, authorizable);

    // Update the values on the profile node.
    updateProfileProperties(session, profileNode, authorizable, change, parameters);

    if (authorizable.isGroup()) {
      // setup a joinrequests node for the group
      Value[] path = authorizable.getProperty(PROP_AUTHORIZABLE_PATH);
      if (path != null && path.length > 0) {
        String pathString = "/_group" + path[0].getString() + "/joinrequests";
        Node messageStore = JcrUtils.deepGetOrCreateNode(session, pathString);
        messageStore.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
            "sakai/joinrequests");
      }
    }
  }

  private void decorateHome(Node homeNode, Authorizable authorizable)
      throws RepositoryException {
    // set the home node resource type
    if (authorizable.isGroup()) {
      homeNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, GROUP_HOME_RESOURCE_TYPE);
    } else {
      homeNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, USER_HOME_RESOURCE_TYPE);
    }

    // set whether the home node should be excluded from searches
    if (authorizable.hasProperty(JCRConstants.SEARCH_EXCLUDE_TREE)) {
      homeNode.setProperty(JCRConstants.SEARCH_EXCLUDE_TREE,
          authorizable.getProperty(JCRConstants.SEARCH_EXCLUDE_TREE)[0]);
    }
  }

  /**
   * @param principalManager
   * @param managerSettings
   * @return
   * @throws RepositoryException
   */
  private Principal[] valuesToPrincipal(Value[] values, Principal[] defaultValue,
      PrincipalManager principalManager) throws RepositoryException {
    // An explicitly empty list of group viewers or managers does not mean the
    // same thing as having no group viewers or managers property, and so
    // a zero-length array should still override the defaults.
    if (values != null) {
      Principal[] valueAsStrings = new Principal[values.length];
      for (int i = 0; i < values.length; i++) {
        valueAsStrings[i] = principalManager.getPrincipal(values[i].getString());
        if ( valueAsStrings[i] == null ) {
          LOGGER.warn("Principal {} cant be resolved, will be ignored ",values[i].getString());
        }
      }
      return valueAsStrings;
    } else {
      return defaultValue;
    }
  }

  /**
   * @param request
   * @param authorizable
   * @return
   * @throws RepositoryException
   */
  private Node createProfile(Session session, Authorizable authorizable)
      throws RepositoryException {
    String path = profileService.getProfilePath(authorizable);
    Node profileNode = null;
    if (!isPostProcessingDone(session, authorizable)) {
      String type = nodeTypeForAuthorizable(authorizable.isGroup());
      LOGGER.debug("Creating or resetting Profile Node {} for authorizable {} ", path,
          authorizable.getID());
      profileNode = JcrUtils.deepGetOrCreateNode(session, path);
      profileNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, type);
      // Make sure we can place references to this profile node in the future.
      // This will make it easier to search on it later on.
      if (profileNode.canAddMixin(JcrConstants.MIX_REFERENCEABLE)) {
        profileNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
      }
    } else {
      profileNode = session.getNode(path);
    }
    return profileNode;
  }

  /**
   * Creates the private folder in the user his home space.
   * TODO As of 2010-09-28 the "private" node is no longer used by any Nakamura
   * component. Can this code be eliminated?
   *
   * @param session
   *          The session to create the node
   * @param athorizable
   *          The Authorizable to create it for
   * @return The {@link Node node} that represents the private folder.
   * @throws RepositoryException
   */
  private Node createPrivate(Session session, Authorizable authorizable)
      throws RepositoryException {
    String privatePath = profileService.getPrivatePath(authorizable);
    if (session.itemExists(privatePath)) {
      return session.getNode(privatePath);
    }
    LOGGER.debug("creating or replacing ACLs for private at {} ", privatePath);
    Node privateNode = JcrUtils.deepGetOrCreateNode(session, privatePath);

    LOGGER.debug("Done creating private at {} ", privatePath);
    return privateNode;
  }

  /**
   * Set access controls on the new User or Group node according to the profile
   * preference configuration property.
   *
   * @param node
   * @param session
   * @param authorizable
   * @throws RepositoryException
   */
  private void initializeAccess(Node node, Session session, Authorizable authorizable) throws RepositoryException {
    String nodePath = node.getPath();
    PrincipalManager principalManager = AccessControlUtil.getPrincipalManager(session);
    Principal everyone = principalManager.getEveryone();
    Principal anon = new Principal() {
      public String getName() {
        return UserConstants.ANON_USERID;
      }
    };
    // KERN-886 : Depending on the profile preference we set some ACL's on the profile.
    if ( UserConstants.ANON_USERID.equals(authorizable.getID()) ) {
      AccessControlUtil.replaceAccessControlEntry(session, nodePath, anon,
          new String[] { JCR_READ }, null, null, null);
      AccessControlUtil.replaceAccessControlEntry(session, nodePath, everyone,
          new String[] { JCR_READ }, null, null, null);
    } else if (VISIBILITY_PUBLIC.equals(visibilityPreference)) {
      AccessControlUtil.replaceAccessControlEntry(session, nodePath, anon,
          new String[] { JCR_READ }, null, null, null);
      AccessControlUtil.replaceAccessControlEntry(session, nodePath, everyone,
          new String[] { JCR_READ }, null, null, null);
    } else if (VISIBILITY_LOGGED_IN.equals(visibilityPreference)) {
      AccessControlUtil.replaceAccessControlEntry(session, nodePath, anon, null,
          new String[] { JCR_READ }, null, null);
      AccessControlUtil.replaceAccessControlEntry(session, nodePath, everyone,
          new String[] { JCR_READ }, null, null, null);
    } else if (VISIBILITY_PRIVATE.equals(visibilityPreference)) {
      AccessControlUtil.replaceAccessControlEntry(session, nodePath, anon, null,
          new String[] { JCR_READ }, null, null);
      AccessControlUtil.replaceAccessControlEntry(session, nodePath, everyone, null,
          new String[] { JCR_READ }, null, null);
    }
  }

  /**
   * Creates the public folder in the user his home space.
   *
   * @param session
   *          The session to create the node
   * @param athorizable
   *          The Authorizable to create it for
   * @return The {@link Node node} that represents the public folder.
   * @throws RepositoryException
   */
  private Node createPublic(Session session, Authorizable athorizable)
      throws RepositoryException {
    String publicPath = profileService.getPublicPath(athorizable);
    if (session.nodeExists(publicPath)) {
      // No more work needed at present.
      return session.getNode(publicPath);
    }
    LOGGER.debug("Creating Public  for {} at   {} ", athorizable.getID(), publicPath);
    Node publicNode = JcrUtils.deepGetOrCreateNode(session, publicPath);
    return publicNode;
  }

  private String nodeTypeForAuthorizable(boolean isGroup) {
    if (isGroup) {
      return UserConstants.GROUP_PROFILE_RESOURCE_TYPE;
    } else {
      return UserConstants.USER_PROFILE_RESOURCE_TYPE;
    }
  }

  // event processing
  // -----------------------------------------------------------------------------

  /**
   * Fire events, into OSGi, one synchronous one asynchronous.
   *
   * @param operation
   *          the operation being performed.
   * @param session
   *          the session performing operation.
   * @param request
   *          the request that triggered the operation.
   * @param authorizable
   *          the authorizable that is the target of the operation.
   * @param changes
   *          a list of {@link Modification} caused by the operation.
   */
  private void fireEvent(Session session, String principalName, Modification change) {
    try {
      String user = session.getUserID();
      String path = change.getDestination();
      if (path == null) {
        path = change.getSource();
      }
      if (AuthorizableEventUtil.isAuthorizableModification(change)) {
        LOGGER.debug("Got Authorizable modification: " + change);
        switch (change.getType()) {
          case COPY:
          case CREATE:
          case DELETE:
          case MOVE:
            LOGGER.debug("Ignoring unknown modification type: " + change.getType());
            break;
          case MODIFY:
            eventAdmin.postEvent(AuthorizableEventUtil.newGroupEvent(user, change));
            break;
          }
        } else if (path.endsWith(principalName)) {
          switch (change.getType()) {
          case COPY:
            eventAdmin.postEvent(AuthorizableEventUtil.newAuthorizableEvent(
                Operation.update, user, principalName, change));
            break;
          case CREATE:
            eventAdmin.postEvent(AuthorizableEventUtil.newAuthorizableEvent(
                Operation.create, user, principalName, change));
            break;
          case DELETE:
            eventAdmin.postEvent(AuthorizableEventUtil.newAuthorizableEvent(
                Operation.delete, user, principalName, change));
            break;
          case MODIFY:
            eventAdmin.postEvent(AuthorizableEventUtil.newAuthorizableEvent(
                Operation.update, user, principalName, change));
            break;
          case MOVE:
            eventAdmin.postEvent(AuthorizableEventUtil.newAuthorizableEvent(
                Operation.update, user, principalName, change));
            break;
          }
        }
    } catch (Throwable t) {
      LOGGER.warn("Failed to fire event", t);
    }
  }

  /**
   * @param eventAdmin
   *          the new EventAdmin service to bind to this service.
   */
  protected void bindEventAdmin(EventAdmin eventAdmin) {
    this.eventAdmin = eventAdmin;
  }

  /**
   * @param eventAdmin
   *          the EventAdminService to be unbound from this service.
   */
  protected void unbindEventAdmin(EventAdmin eventAdmin) {
    this.eventAdmin = null;
  }

  /**
   * Decide whether post-processing this user or group would be redundant because it has
   * already been done. The current logic uses the existence of a profile node of the
   * correct type as a marker.
   *
   * @param session
   * @param authorizable
   * @return true if there is evidence that post-processing has already occurred for this
   *         user or group
   * @throws RepositoryException
   */
  private boolean isPostProcessingDone(Session session, Authorizable authorizable)
      throws RepositoryException {
    boolean isProfileCreated = false;
    Node node = getProfileNode(session, authorizable);
    if (node != null) {
      String type = nodeTypeForAuthorizable(authorizable.isGroup());
      if (node.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)) {
        if (node.getProperty(SLING_RESOURCE_TYPE_PROPERTY).getString().equals(type)) {
          isProfileCreated = true;
        }
      }
    }
    return isProfileCreated;
  }

  private Node getProfileNode(Session session, Authorizable authorizable)
      throws RepositoryException {
    Node profileNode;
    String path = profileService.getProfilePath(authorizable);
    if (session.nodeExists(path)) {
      profileNode = session.getNode(path);
    } else {
      profileNode = null;
    }
    return profileNode;
  }

  private void updateHomeFolder(Session session, Authorizable authorizable,
      Modification change, Map<String, Object[]> parameters) throws RepositoryException {
    Node profileNode = getProfileNode(session, authorizable);
    if (profileNode != null) {
      // Mirror the current state of the Authorizable's visibility and
      // management controls, if the current session has the right to do
      // so.
      // TODO Replace these implicit side-effects with something more controllable
      // by the client.
      refreshOwnership(session, authorizable, profileService.getHomePath(authorizable));
      if (!parameters.containsKey(":sakai:update-profile")
          || !"false".equals(parameters.get(":sakai:update-profile")[0])) {
        updateProfileProperties(session, getProfileNode(session, authorizable),
            authorizable, change, parameters);
      }
    }
  }

  /**
   * If the current session has sufficient rights, synchronize home folder
   * access to match the current accessibility of the Jackrabbit User or
   * Group. Currently this is done for every update, overwriting any ACLs
   * which might have been explicitly set on the home node.
   *
   * @param session
   * @param authorizable
   * @param homeFolderPath
   * @throws RepositoryException
   */
  private void refreshOwnership(Session session, Authorizable authorizable,
      String homeFolderPath) throws RepositoryException {
    if (isAbleToControlAccess(session, homeFolderPath)) {
      PrincipalManager principalManager = AccessControlUtil.getPrincipalManager(session);

      Value[] managerSettings = authorizable.getProperty(UserConstants.PROP_GROUP_MANAGERS);
      Value[] viewerSettings = authorizable.getProperty(UserConstants.PROP_GROUP_VIEWERS);

      // If the Authorizable has a managers list, everyone on that list gets write access.
      // Otherwise, the Authorizable itself is the owner.
      Principal[] managers = valuesToPrincipal(managerSettings,
          new Principal[] { authorizable.getPrincipal() }, principalManager);

      // Do not automatically give read-access to anonymous and everyone, since that
      // forces User Home folders to be public and overwrites configuration settings.
      Principal[] viewers = valuesToPrincipal(viewerSettings, new Principal[] { },
          principalManager);

      for (Principal manager : managers) {
        if ( manager != null && !UserConstants.ANON_USERID.equals(manager.getName()) ) {
          LOGGER.debug("User {} is attempting to make {} a manager ", session.getUserID(),
            manager.getName());
          AccessControlUtil.replaceAccessControlEntry(session, homeFolderPath, manager,
            new String[] { JCR_ALL }, null, null, null);
        }
      }
      for (Principal viewer : viewers) {
        if ( viewer != null && !UserConstants.ANON_USERID.equals(viewer.getName())) {
          LOGGER.debug("User {} is attempting to make {} a viewer ", session.getUserID(),
            viewer.getName());
          AccessControlUtil.replaceAccessControlEntry(session, homeFolderPath, viewer,
            new String[] { JCR_READ }, new String[] { JCR_WRITE }, null, null);
        }
      }
      LOGGER.debug("Set ACL on Node for {} at   {} ", authorizable.getID(), homeFolderPath);
    }
  }

  private boolean isAbleToControlAccess(Session session, String homeFolderPath) throws RepositoryException {
    AccessControlManager accessControlManager = AccessControlUtil.getAccessControlManager(session);
    Privilege[] modifyAclPrivileges = { accessControlManager.privilegeFromName(Privilege.JCR_MODIFY_ACCESS_CONTROL) };
    return accessControlManager.hasPrivileges(homeFolderPath, modifyAclPrivileges);
  }

  private boolean isAbleToModify(Session session, String path) throws RepositoryException {
    AccessControlManager accessControlManager = AccessControlUtil.getAccessControlManager(session);
    Privilege[] modifyPrivileges = {
        accessControlManager.privilegeFromName(Privilege.JCR_MODIFY_PROPERTIES),
        accessControlManager.privilegeFromName(Privilege.JCR_ADD_CHILD_NODES),
        accessControlManager.privilegeFromName(Privilege.JCR_REMOVE_CHILD_NODES)
        };
    return accessControlManager.hasPrivileges(path, modifyPrivileges);
  }
}
