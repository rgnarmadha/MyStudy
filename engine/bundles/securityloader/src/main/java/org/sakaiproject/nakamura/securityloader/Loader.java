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
package org.sakaiproject.nakamura.securityloader;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.osgi.framework.Bundle;
import org.sakaiproject.nakamura.api.user.AuthorizableEvent.Operation;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;


/**
 *
 */
@SuppressWarnings(justification="Circular dependency noted ", value={"CD_CIRCULAR_DEPENDENCY"})
public class Loader implements SecurityLoader {

  private static final Logger LOGGER = LoggerFactory.getLogger(Loader.class);
  private List<Bundle> delayedBundles;
  private SecurityLoaderService jcrContentHelper;
  private AuthorizablePostProcessService authorizablePostProcessService;

  public static final String SYSTEM_USER_MANAGER_PATH = "/system/userManager";

  public static final String SYSTEM_USER_MANAGER_USER_PATH = SYSTEM_USER_MANAGER_PATH
      + "/user";

  public static final String SYSTEM_USER_MANAGER_GROUP_PATH = SYSTEM_USER_MANAGER_PATH
      + "/group";

  public static final String SYSTEM_USER_MANAGER_USER_PREFIX = SYSTEM_USER_MANAGER_USER_PATH
      + "/";

  public static final String SYSTEM_USER_MANAGER_GROUP_PREFIX = SYSTEM_USER_MANAGER_GROUP_PATH
      + "/";

  private static final String MEMBERS = "members";
  private static final String PASSWORD = "password";
  private static final String NAME = "name";
  private static final String ACCESSLIST = "acl";
  private static final String PRINCIPALS = "principals";
  private static final String GROUP = "isgroup";
  private static final String PRINCIPAL = "principal";
  private static final String PATH = "path";

  /**
   * @param securityLoaderService
   */
  public Loader(SecurityLoaderService jcrContentHelper, AuthorizablePostProcessService authorizablePostProcessService) {
    this.jcrContentHelper = jcrContentHelper;
    this.delayedBundles = new LinkedList<Bundle>();
    this.authorizablePostProcessService = authorizablePostProcessService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.securityloader.SecurityLoader#dispose()
   */
  public void dispose() {
    if (delayedBundles != null) {
      delayedBundles.clear();
      delayedBundles = null;
    }
    jcrContentHelper = null;
  }

  /**
   * {@inheritDoc}
   *
   * @throws IOException
   * @throws JSONException
   *
   * @see org.sakaiproject.nakamura.securityloader.SecurityLoader#registerBundle(org.osgi.framework.Bundle,
   *      boolean)
   */
  public void registerBundle(Session session, Bundle bundle, boolean isUpdate)
      throws JSONException, IOException {
    // if this is an update, we have to uninstall the old content first
    if (isUpdate) {
      this.unregisterBundle(session, bundle);
    }

    LOGGER.info("Trying to Load security from bundle {}.", bundle.getSymbolicName());
    if (registerBundleInternal(session, bundle, false, isUpdate)) {

      // handle delayed bundles, might help now
      int currentSize = -1;
      // dont loop forever
      for (int i = delayedBundles.size(); i > 0 && currentSize != delayedBundles.size()
          && !delayedBundles.isEmpty(); i--) {

        for (Iterator<Bundle> di = delayedBundles.iterator(); di.hasNext();) {

          Bundle delayed = di.next();
          LOGGER.debug("Trying to Load security from delayed bundle {}.", delayed.getSymbolicName());
          if (registerBundleInternal(session, delayed, true, false)) {
            di.remove();
          }

        }

        currentSize = delayedBundles.size();
      }

    } else if (!isUpdate) {
      LOGGER.info("Delayed loading of security for {}.", bundle.getSymbolicName());
      // add to delayed bundles - if this is not an update!
      delayedBundles.add(bundle);
    }

  }

  /**
   * @param bundle
   * @param b
   * @param isUpdate
   * @return
   * @throws IOException
   * @throws JSONException
   */
  private boolean registerBundleInternal(Session session, Bundle bundle, boolean isRetry,
      boolean isUpdate) throws JSONException, IOException {

    // check if bundle has initial content
    final Iterator<PathEntry> pathIter = PathEntry.getContentPaths(bundle);
    if (pathIter == null) {
      LOGGER.debug("Bundle {} has no security setup", bundle.getSymbolicName());
      return true;
    }

    try {

      // check if the content has already been loaded
      final Map<String, Object> bundleContentInfo = jcrContentHelper
          .getBundleContentInfo(session, bundle, true);

      // if we don't get an info, someone else is currently loading
      if (bundleContentInfo == null) {
        return false;
      }

      boolean success = false;
      List<String> createdNodes = null;
      try {

        final boolean contentAlreadyLoaded = ((Boolean) bundleContentInfo
            .get(SecurityLoaderService.PROPERTY_SECURITY_LOADED)).booleanValue();

        if (!isUpdate && contentAlreadyLoaded) {

          LOGGER.debug("Content of security bundle already loaded {}.", bundle.getSymbolicName());

        } else {

          createdNodes = install(session, bundle, pathIter, contentAlreadyLoaded);

          if (isRetry) {
            // log success of retry
            LOGGER.debug("Retrytring to load security content for bundle {} succeeded.",
                bundle.getSymbolicName());
          }

        }
        LOGGER.debug("Loaded security content for bundle {}.",
            bundle.getSymbolicName());

        success = true;
        return true;

      } finally {
        jcrContentHelper.unlockBundleContentInfo(session, bundle, success, createdNodes);
      }

    } catch (RepositoryException re) {
      // if we are retrying we already logged this message once, so we
      // won't log it again
      if (!isRetry || LOGGER.isInfoEnabled() ) {
        LOGGER.error("Cannot load security content for bundle " + bundle.getSymbolicName()
            + " : " + re.getMessage(), re);
      }
    }

    LOGGER.debug("Failed to load security content for bundle {}.",
        bundle.getSymbolicName());
    return false;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.securityloader.SecurityLoader#unregisterBundle(org.osgi.framework.Bundle)
   */
  public void unregisterBundle(Session session, Bundle bundle) {

    if (delayedBundles.contains(bundle)) {

      delayedBundles.remove(bundle);

    } else {
      try {
        final Map<String, Object> bundleContentInfo = jcrContentHelper
            .getBundleContentInfo(session, bundle, false);

        // if we don't get an info, someone else is currently loading or unloading
        // or the bundle is already uninstalled
        if (bundleContentInfo == null) {
          return;
        }

        try {
          uninstall(session, bundle, (String[]) bundleContentInfo
              .get(SecurityLoaderService.PROPERTY_SECURITY_PATHS));
          jcrContentHelper.contentIsUninstalled(session, bundle);
        } finally {
          jcrContentHelper.unlockBundleContentInfo(session, bundle, false, null);

        }
      } catch (RepositoryException re) {
        LOGGER.error("Cannot remove security content for bundle "
            + bundle.getSymbolicName() + " : " + re.getMessage(), re);
      }
    }
  }

  private void uninstall(final Session session, final Bundle bundle,
      final String[] uninstallPaths) {
    try {
      LOGGER.debug("Uninstalling initial security from bundle {}", bundle
          .getSymbolicName());
      if (uninstallPaths != null && uninstallPaths.length > 0) {
        for (final String path : uninstallPaths) {
          if (session.itemExists(path)) {
            session.getItem(path).remove();
          }
        }
        // persist modifications now
        session.save();
      }

      LOGGER.debug("Done uninstalling initial security from bundle {}", bundle
          .getSymbolicName());
    } catch (RepositoryException re) {
      LOGGER.error("Unable to uninstall security content from bundle "
          + bundle.getSymbolicName(), re);
    } finally {
      try {
        if (session.hasPendingChanges()) {
          session.refresh(false);
        }
      } catch (RepositoryException re) {
        LOGGER.warn("Failure to rollback uninstaling security content for bundle {}",
            bundle.getSymbolicName(), re);
      }
    }
  }

  /**
   * Install the content from the bundle.
   *
   * @return If the content should be removed on uninstall, a list of top nodes
   * @throws IOException
   * @throws JSONException
   */
  private List<String> install(final Session session, final Bundle bundle,
      final Iterator<PathEntry> pathIter, final boolean contentAlreadyLoaded)
      throws RepositoryException, JSONException, IOException {
    final List<String> createdNodes = new ArrayList<String>();

    LOGGER.debug("Installing initial security from bundle {}", bundle.getSymbolicName());
    try {

      while (pathIter.hasNext()) {
        final PathEntry entry = pathIter.next();
        if (!contentAlreadyLoaded || entry.isOverwrite()) {

          final Node targetNode = getTargetNode(session, entry.getTarget());
LOGGER.info("Got Target Node as "+targetNode);
          if (targetNode != null) {
            installFromPath(session, bundle, entry.getPath(), entry, targetNode, entry
                .isUninstall() ? createdNodes : null);
          }
        }
      }

      // now optimize created nodes list
      Collections.sort(createdNodes);
      if (createdNodes.size() > 1) {
        final Iterator<String> i = createdNodes.iterator();
        String previous = i.next() + '/';
        while (i.hasNext()) {
          final String current = i.next();
          if (current.startsWith(previous)) {
            i.remove();
          } else {
            previous = current + '/';
          }
        }
      }

      // persist modifications now
      session.refresh(true);
      session.save();

    } finally {
      try {
        if (session.hasPendingChanges()) {
          session.refresh(false);
        }
      } catch (RepositoryException re) {
        LOGGER.warn("Failure to rollback partial security content for bundle {}", bundle
            .getSymbolicName(), re);
      }
    }
    LOGGER.debug("Done installing security content from bundle {}", bundle
        .getSymbolicName());

    return createdNodes;
  }

  /**
   * Load the descriptor and process information
   *
   * @param bundle
   * @param path
   * @param entry
   * @param targetNode
   * @param list
   * @throws JSONException
   * @throws RepositoryException
   * @throws IOException
   */
  private void installFromPath(Session session, Bundle bundle, String path,
      PathEntry entry, Node targetNode, List<String> list) throws JSONException,
      IOException, RepositoryException {
    LOGGER.debug("Processing security content entry {}", entry);
    URL file = bundle.getEntry(path);
    JSONObject aclSetup = parse(file);

    // acl setup now contains the json to load.
    JSONArray principals = aclSetup.getJSONArray(PRINCIPALS);
    for (int i = 0; i < principals.length(); i++) {
      JSONObject principal = principals.getJSONObject(i);
      if (principal.getBoolean(GROUP)) {
        createGroup(session, principal);
      } else {
        createUser(session, principal);
      }
    }
    if ( session.hasPendingChanges() ) {
      session.save();
    }

    JSONArray acls = aclSetup.getJSONArray(ACCESSLIST);
    for (int i = 0; i < acls.length(); i++) {
      JSONObject acl = acls.getJSONObject(i);
      createAcl(session, targetNode, acl);
    }
  }

  /**
   * @param session
   * @param targetNode
   * @param acl
   * @throws JSONException
   * @throws RepositoryException
   * @throws UnsupportedRepositoryOperationException
   * @throws AccessDeniedException
   */
  private void createAcl(Session session, Node targetNode, JSONObject acl) throws JSONException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {

    String principalId = acl.getString(PRINCIPAL);
    if (principalId == null) {
      LOGGER.warn("No Principal specified, ignoring :" + acl);
      return;
    }
    PrincipalManager principalManager = AccessControlUtil.getPrincipalManager(session);
    Principal principal;
    principal = principalManager.getPrincipal(principalId);

    String path = acl.getString(PATH);
    String targetPath = targetNode.getPath();

    LOGGER.debug("Base Path {} Source Path {} ", targetPath, path);

    String resourcePath = path;
    if ( !"/".equals(targetPath) )  {
      resourcePath = targetPath + resourcePath;
    }
    LOGGER.debug("Resource Path {} ", resourcePath);

    // create the path, if it doesnt exist.
    // this may cause problems if we are putting files with acls
    jcrContentHelper.createRepositoryPath(session, resourcePath);

    List<String> grantedPrivilegeNames = new ArrayList<String>();
    List<String> deniedPrivilegeNames = new ArrayList<String>();
    Iterator<String> parameterNames = acl.keys();
    while (parameterNames.hasNext()) {
      String paramName = parameterNames.next();
      if (paramName.startsWith("privilege@")) {
        String parameterValue = acl.getString(paramName);
        if (parameterValue != null && parameterValue.length() > 0) {
          if ("granted".equals(parameterValue)) {
            String privilegeName = paramName.substring(10);
            grantedPrivilegeNames.add(privilegeName);
          } else if ("denied".equals(parameterValue)) {
            String privilegeName = paramName.substring(10);
            deniedPrivilegeNames.add(privilegeName);
          }
        }
      }
    }


    AccessControlManager accessControlManager = AccessControlUtil
        .getAccessControlManager(session);
    AccessControlList updatedAcl = null;

    AccessControlPolicyIterator applicablePolicies = accessControlManager
        .getApplicablePolicies(resourcePath);
    while (applicablePolicies.hasNext()) {
      AccessControlPolicy policy = applicablePolicies.nextAccessControlPolicy();
      if (policy instanceof AccessControlList) {
        updatedAcl = (AccessControlList) policy;
        break;
      }
    }

    if (updatedAcl == null) {
      AccessControlPolicy[] policies = accessControlManager.getPolicies(resourcePath);
      for ( AccessControlPolicy policy : policies ) {
        if (policy instanceof AccessControlList) {
          updatedAcl = (AccessControlList) policy;
          break;
        }
      }
    }

    if (updatedAcl == null) {
      throw new RepositoryException("Unable to find an access conrol policy to update.");
    }

    StringBuilder oldPrivileges = null;
    StringBuilder newPrivileges = new StringBuilder();

    if (LOGGER.isInfoEnabled()) {
      oldPrivileges = new StringBuilder();
    }

    // keep track of the existing Aces for the target principal
    AccessControlEntry[] accessControlEntries = updatedAcl.getAccessControlEntries();
    List<AccessControlEntry> oldAces = new ArrayList<AccessControlEntry>();
    for (AccessControlEntry ace : accessControlEntries) {
      if (principalId.equals(ace.getPrincipal().getName())) {
        LOGGER.debug("Found Existing ACE for principal {} on resource: ",
              new Object[] {principalId, resourcePath});
        oldAces.add(ace);

        if (oldPrivileges != null) {
          // collect the information for debug logging
          boolean isAllow = AccessControlUtil.isAllow(ace);
          Privilege[] privileges = ace.getPrivileges();
          for (Privilege privilege : privileges) {
            if (oldPrivileges.length() > 0) {
              oldPrivileges.append(", "); // separate entries by commas
            }
            if (isAllow) {
              oldPrivileges.append("granted=");
            } else {
              oldPrivileges.append("denied=");
            }
            oldPrivileges.append(privilege.getName());
          }
        }
      }
    }

    // remove the old aces
    if (!oldAces.isEmpty()) {
      for (AccessControlEntry ace : oldAces) {
        updatedAcl.removeAccessControlEntry(ace);
      }
    }

    // add a fresh ACE with the granted privileges
    List<Privilege> grantedPrivilegeList = new ArrayList<Privilege>();
    for (String name : grantedPrivilegeNames) {
      if (name.length() == 0) {
        continue; // empty, skip it.
      }
      Privilege privilege = accessControlManager.privilegeFromName(name);
      grantedPrivilegeList.add(privilege);

      if (newPrivileges.length() > 0) {
        newPrivileges.append(", "); // separate entries by commas
      }
      newPrivileges.append("granted=");
      newPrivileges.append(privilege.getName());
    }
    if (grantedPrivilegeList.size() > 0) {
      updatedAcl.addAccessControlEntry(principal, grantedPrivilegeList
          .toArray(new Privilege[grantedPrivilegeList.size()]));
    }

    // process any denied privileges
    // add a fresh ACE with the denied privileges
    List<Privilege> deniedPrivilegeList = new ArrayList<Privilege>();
    for (String name : deniedPrivilegeNames) {
      if (name.length() == 0) {
        continue; // empty, skip it.
      }
      Privilege privilege = accessControlManager.privilegeFromName(name);
      deniedPrivilegeList.add(privilege);

      if (newPrivileges.length() > 0) {
        newPrivileges.append(", "); // separate entries by commas
      }
      newPrivileges.append("denied=");
      newPrivileges.append(privilege.getName());
    }
    if (deniedPrivilegeList.size() > 0) {
      AccessControlUtil.addEntry(updatedAcl, principal, deniedPrivilegeList
          .toArray(new Privilege[deniedPrivilegeList.size()]), false);
    }

    accessControlManager.setPolicy(resourcePath, updatedAcl);
    if (session.hasPendingChanges()) {
      session.save();
    }

    jcrContentHelper.fireEvent(resourcePath, newPrivileges.toString());
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Updated ACE for principalId {} for resource {} from {} to {}",
          new Object[] {principal.getName(), resourcePath, oldPrivileges.toString(),
              newPrivileges.toString()});
    }

  }

  /**
   * @param session
   * @param principal
   * @throws RepositoryException
   * @throws UnsupportedRepositoryOperationException
   * @throws AccessDeniedException
   * @throws JSONException
   */
  private void createUser(Session session, JSONObject principal)
      throws AccessDeniedException, UnsupportedRepositoryOperationException,
      RepositoryException, JSONException {
    UserManager userManager = AccessControlUtil.getUserManager(session);
    final String principalName = principal.getString(NAME);
    if (principalName == null || principalName.trim().length() == 0) {
      LOGGER.warn("Ignored Entry " + principal);
      return;
    }
    Authorizable authorizable = userManager.getAuthorizable(principalName);

    if (authorizable == null) {
      List<Modification> changes = new ArrayList<Modification>();
      User user = userManager.createUser(principalName, jcrContentHelper
          .digestPassword(principal.getString(PASSWORD)));
      String userPath = SYSTEM_USER_MANAGER_USER_PREFIX
          + user.getID();

      changes.add(Modification.onCreated(userPath));

      // write content from form
      writeContent(session, user, principal, changes);

      // Process new user record for general use in Sakai.
      try {
        // TODO The loader should pass non-persisted JSON parameters
        // for further processing.
        authorizablePostProcessService.process(user, session, ModificationType.CREATE);
      } catch (Exception e) {
        LOGGER.warn("Unable to postprocess new user " + user.getID(), e);
      }

      jcrContentHelper.fireEvent(Operation.create, session, user, changes);
    } else {
      LOGGER.debug("Principal "+principalName+" exists, no action required");
      //
    }

  }

  /**
   * @param session
   * @param principal
   * @throws RepositoryException
   * @throws UnsupportedRepositoryOperationException
   * @throws AccessDeniedException
   * @throws JSONException
   */
  private void createGroup(Session session, JSONObject principal)
      throws AccessDeniedException, UnsupportedRepositoryOperationException,
      RepositoryException, JSONException {
    UserManager userManager = AccessControlUtil.getUserManager(session);
    final String principalName = principal.getString(NAME);
    if (principalName == null || principalName.trim().length() == 0) {
      LOGGER.warn("Ignored Entry " + principalName);
      return;
    }
    Authorizable authorizable = userManager.getAuthorizable(principalName);
    if (authorizable == null) {

      List<Modification> changes = new ArrayList<Modification>();
      Group group = userManager.createGroup(new Principal() {
        public String getName() {
          return principalName;
        }
      });
      String groupPath = SYSTEM_USER_MANAGER_GROUP_PREFIX
          + group.getID();
      changes.add(Modification.onCreated(groupPath));
      // write content from form
      writeContent(session, group, principal, changes);

      // update the group memberships
      updateGroupMembership(userManager, group, principal.getJSONArray(MEMBERS), changes);

      jcrContentHelper.fireEvent(Operation.create, session, group, changes); // create
    } else {
      // update
      // ignore.
    }
  }

  /**
   * Update the group membership based on the ":member" request parameters. If the
   * ":member" value ends with @Delete it is removed from the group membership, otherwise
   * it is added to the group membership.
   *
   * @param request
   * @param authorizable
   * @throws RepositoryException
   * @throws JSONException
   */
  protected void updateGroupMembership(UserManager userManager, Group authorizable,
      JSONArray members, List<Modification> changes) throws RepositoryException,
      JSONException {
    if (authorizable.isGroup()) {
      Group group = ((Group) authorizable);
      String groupPath = SYSTEM_USER_MANAGER_GROUP_PREFIX
          + group.getID();

      boolean changed = false;
      if (members != null) {
        for (int i = 0; i < members.length(); i++) {
          String member = members.getString(i);
          Authorizable memberAuthorizable = userManager.getAuthorizable(member);
          if (memberAuthorizable != null) {
            group.addMember(memberAuthorizable);
            changed = true;
          }
        }
      }

      if (changed) {
        // add an entry to the changes list to record the membership change
        changes.add(Modification.onModified(groupPath + "/members"));
      }
    }
  }

  /**
   * Writes back the content
   *
   * @throws RepositoryException
   *           if a repository error occurs
   * @throws JSONException
   * @throws ServletException
   *           if an internal error occurs
   */
  protected void writeContent(Session session, Authorizable authorizable,
      JSONObject properties, List<Modification> changes) throws RepositoryException,
      JSONException {

    for (Iterator<String> ki = properties.keys(); ki.hasNext();) {
      String name = ki.next();
      if (name != null) {
        // skip jcr special properties
        if (name.equals("jcr:primaryType") || name.equals("jcr:mixinTypes")) {
          continue;
        }
        if (authorizable.isGroup()) {
          if (name.equals("groupId")) {
            // skip these
            continue;
          }
        } else {
          if (name.equals("userId") || name.equals("pwd") || name.equals("pwdConfirm")
              || name.equals(MEMBERS)) {
            // skip these
            continue;
          }
        }
        setPropertyAsIs(session, authorizable, name, properties.getString(name), changes);
      }
    }
  }



  /**
   * @see org.apache.sling.jcr.contentloader.internal.ContentReader#parse(java.net.URL,
   *      org.apache.sling.jcr.contentloader.internal.ContentCreator)
   */
  public JSONObject parse(URL url) throws IOException, RepositoryException {
    InputStream ins = null;
    try {
      ins = url.openStream();
      return parse(ins);
    } finally {
      if (ins != null) {
        try {
          ins.close();
        } catch (IOException ignore) {
        }
      }
    }
  }

  public JSONObject parse(InputStream ins) throws IOException, RepositoryException {
    try {
      String jsonString = toString(ins).trim();
      if (!jsonString.startsWith("{")) {
        jsonString = "{" + jsonString + "}";
      }

      return new JSONObject(jsonString);
    } catch (JSONException je) {
      throw (IOException) new IOException(je.getMessage()).initCause(je);
    }
  }

  private String toString(InputStream ins) throws IOException {
    if (!ins.markSupported()) {
      ins = new BufferedInputStream(ins);
    }

    String encoding;
    ins.mark(5);
    int c = ins.read();
    if (c == '#') {
      // character encoding following
      StringBuffer buf = new StringBuffer();
      for (c = ins.read(); !Character.isWhitespace((char) c); c = ins.read()) {
        buf.append((char) c);
      }
      encoding = buf.toString();
    } else {
      ins.reset();
      encoding = "UTF-8";
    }

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    byte[] buf = new byte[1024];
    int rd;
    while ((rd = ins.read(buf)) >= 0) {
      bos.write(buf, 0, rd);
    }
    bos.close(); // just to comply with the contract

    return new String(bos.toByteArray(), encoding);
  }

  private Node getTargetNode(Session session, String path) throws RepositoryException {
    LOGGER.info("Getting node "+path);

    // not specyfied path directive
    if (path == null)
      return session.getRootNode();

    int firstSlash = path.indexOf("/");

    // it's a relative path
    if (firstSlash != 0)
      path = "/" + path;

    if (!session.itemExists(path)) {
      Node currentNode = session.getRootNode();
      final StringTokenizer st = new StringTokenizer(path.substring(1), "/");
      while (st.hasMoreTokens()) {
        final String name = st.nextToken();
        if (!currentNode.hasNode(name)) {
          currentNode.addNode(name, "sling:Folder");
        }
        currentNode = currentNode.getNode(name);
      }
      return currentNode;
    }
    Item item = session.getItem(path);
    return (item.isNode()) ? (Node) item : null;
  }



  /**
   * set property without processing, except for type hints
   *
   * @param parent
   *          the parent node
   * @param prop
   *          the request property
   * @throws RepositoryException
   *           if a repository error occurs.
   */
  private void setPropertyAsIs(Session session, Authorizable parent, String name,
      String value, List<Modification> changes) throws RepositoryException {

    String parentPath;
    if (parent.isGroup()) {
      parentPath = SYSTEM_USER_MANAGER_GROUP_PREFIX
          + parent.getID();
    } else {
      parentPath = SYSTEM_USER_MANAGER_USER_PREFIX
          + parent.getID();
    }

    List<Value> values = new ArrayList<Value>();
    if (parent.hasProperty(name)) {
      Value[] valueArray = parent.getProperty(name);
      for (Value v : valueArray) {
        values.add(v);
      }
    }
    values.add(session.getValueFactory().createValue(value));
    parent.setProperty(name, values.toArray(new Value[0]));
    changes.add(Modification.onModified(parentPath + "/" + name));

  }

}
