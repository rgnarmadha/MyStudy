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
package org.sakaiproject.nakamura.basiclti;

import static javax.jcr.security.Privilege.JCR_ALL;
import static org.apache.sling.jcr.base.util.AccessControlUtil.replaceAccessControlEntry;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.LTI_ADMIN_NODE_NAME;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.TOPIC_BASICLTI_ADDED;
import static org.sakaiproject.nakamura.basiclti.BasicLTIServletUtils.getInvalidUserPrivileges;
import static org.sakaiproject.nakamura.basiclti.BasicLTIServletUtils.isAdminUser;
import static org.sakaiproject.nakamura.basiclti.BasicLTIServletUtils.removeProperty;
import static org.sakaiproject.nakamura.basiclti.BasicLTIServletUtils.sensitiveKeys;
import static org.sakaiproject.nakamura.basiclti.BasicLTIServletUtils.unsupportedKeys;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.AbstractSlingPostOperation;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostOperation;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.sakaiproject.nakamura.util.osgi.EventUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import javax.jcr.version.VersionException;

@ServiceDocumentation(
  name = "Basic LTI Post Operation",
  description = "Sets up a node to be used as configuration for interation with Basic LTI",
  methods = {
    @ServiceMethod(name = "POST",
      parameters = {
        @ServiceParameter(name = ":opertion=basiclti", description = "The operation to specify when posting to trigger this operation.")
      },
      description = {
        "Adds any provided properties to the noded being posted to for use in BasicLTI integration. Properties ending with @Delete are removed."
      }
    )
  }
)
@Component(immediate = true)
@Service(value = SlingPostOperation.class)
@Properties(value = {
    @Property(name = "sling.post.operation", value = "basiclti"),
    @Property(name = "service.description", value = "Creates a sakai/basiclti settings node."),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
public class BasicLTIPostOperation extends AbstractSlingPostOperation {
  private static final Logger LOG = LoggerFactory.getLogger(BasicLTIPostOperation.class);
  /**
   * Dependency injected from OSGI container.
   */
  @Reference
  private transient SlingRepository slingRepository;

  @Reference
  protected transient EventAdmin eventAdmin;

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.servlets.post.AbstractSlingPostOperation#doRun(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.servlets.HtmlResponse, java.util.List)
   */
  @Override
  protected void doRun(SlingHttpServletRequest request, HtmlResponse response,
      List<Modification> changes) throws RepositoryException {
    final Session session = request.getResourceResolver().adaptTo(Session.class);
    Node node = request.getResource().adaptTo(Node.class);
    if (node == null) { // create the node
      final String path = request.getResource().getPath();
      node = JcrUtils.deepGetOrCreateNode(session, path);
    }
    try {
      final Map<String, String> sensitiveData = new HashMap<String, String>(
          sensitiveKeys.size());
      // loop through request parameters
      final RequestParameterMap requestParameterMap = request.getRequestParameterMap();
      for (final Entry<String, RequestParameter[]> entry : requestParameterMap.entrySet()) {
        final String key = entry.getKey();
        if (key.endsWith("@TypeHint")) {
          continue;
        }
        final RequestParameter[] requestParameterArray = entry.getValue();
        if (requestParameterArray == null || requestParameterArray.length == 0) {
          // removeProperty(node, key);
        } else {
          if (requestParameterArray.length > 1) {
            throw new RepositoryException("Multi-valued parameters are not supported");
          } else {
            final String value = requestParameterArray[0].getString("UTF-8");
            if ("".equals(value)) {
              removeProperty(node, key);
            } else { // has a valid value
              if (sensitiveKeys.contains(key)) {
                sensitiveData.put(key, value);
              } else {
                if (!unsupportedKeys.contains(key)) {
                  final String typeHint = key + "@TypeHint";
                  if (requestParameterMap.containsKey(typeHint)
                      && "Boolean".equals(requestParameterMap.get(typeHint)[0]
                          .getString())) {
                    node.setProperty(key, Boolean.valueOf(value));
                  } else {
                    node.setProperty(key, value);
                  }
                }
              }
            }
          }
        }
      } // end request parameters loop
      // safety precaution - just to be safe
      for (String skey : sensitiveKeys) {
        removeProperty(node, skey);
      }
      if (session.hasPendingChanges()) {
        session.save();
      }
      createSensitiveNode(node, session, sensitiveData);

      // Send an OSGi event.
      Dictionary<String, String> properties = new Hashtable<String, String>();
      properties.put(UserConstants.EVENT_PROP_USERID, request.getRemoteUser());
      EventUtils.sendOsgiEvent(properties, TOPIC_BASICLTI_ADDED, eventAdmin);
    } catch (Throwable e) {
      throw new RepositoryException(e);
    }
  }

  private void createSensitiveNode(final Node parent, final Session userSession,
      Map<String, String> sensitiveData) throws ItemExistsException,
      PathNotFoundException, VersionException, ConstraintViolationException,
      LockException, RepositoryException {
    if (parent == null) {
      throw new IllegalArgumentException("Node parent==null");
    }
    // if (!"sakai/basiclti".equals(parent.getProperty("sling:resourceType"))) {
    // throw new
    // IllegalArgumentException("sling:resourceType != sakai/basiclti");
    // }
    if (userSession == null) {
      throw new IllegalArgumentException("userSession == null");
    }
    if (sensitiveData == null || sensitiveData.isEmpty()) {
      // do nothing - virtual tool use case
      return;
    }
    final String adminNodePath = parent.getPath() + "/" + LTI_ADMIN_NODE_NAME;
    // now let's elevate Privileges and do some admin modifications
    Session adminSession = null;
    try {
      adminSession = slingRepository.loginAdministrative(null);
      final Node adminNode = JcrUtils.deepGetOrCreateNode(adminSession, adminNodePath);
      for (final Entry<String, String> entry : sensitiveData.entrySet()) {
        adminNode.setProperty(entry.getKey(), entry.getValue());
      }
      // ensure only admins can read the node
      accessControlSensitiveNode(adminNodePath, adminSession, userSession.getUserID());
      if (adminSession.hasPendingChanges()) {
        adminSession.save();
      }
    } finally {
      if (adminSession != null) {
        adminSession.logout();
      }
    } // end admin elevation
    // sanity check to verify user does not have permissions to sensitive node
    boolean invalidPrivileges = false;
    if (!isAdminUser(userSession)) { // i.e. normal user
      try {
        final AccessControlManager acm = AccessControlUtil
            .getAccessControlManager(userSession);
        Privilege[] userPrivs = acm.getPrivileges(adminNodePath);
        if (userPrivs != null && userPrivs.length > 0) {
          Set<Privilege> invalidUserPrivileges = getInvalidUserPrivileges(acm);
          for (Privilege privilege : userPrivs) {
            if (invalidUserPrivileges.contains(privilege)) {
              invalidPrivileges = true;
              break;
            }
          }
        }
      } catch (PathNotFoundException e) { // This is to be expected
        LOG.debug("The node does not exist or the user does not have permission(?): {}",
            adminNodePath);
      }
    }
    if (invalidPrivileges) {
      LOG.error("{} has invalid privileges: {}", userSession.getUserID(), adminNodePath);
      throw new Error(userSession.getUserID() + " has invalid privileges: "
          + adminNodePath);
    }
  }

  /**
   * Apply the necessary access control entries so that only admin users can read/write
   * the sensitive node.
   *
   * @param sensitiveNodePath
   * @param adminSession
   * @throws AccessDeniedException
   * @throws UnsupportedRepositoryOperationException
   * @throws RepositoryException
   */
  private void accessControlSensitiveNode(final String sensitiveNodePath,
      final Session adminSession, String currentUserId) throws AccessDeniedException,
      UnsupportedRepositoryOperationException, RepositoryException {
    final UserManager userManager = AccessControlUtil.getUserManager(adminSession);
    final PrincipalManager principalManager = AccessControlUtil
        .getPrincipalManager(adminSession);
    final Authorizable currentUser = userManager.getAuthorizable(currentUserId);

    Principal anon = new Principal() {
      public String getName() {
        return UserConstants.ANON_USERID;
      }
    };
    Principal everyone = principalManager.getEveryone();

    replaceAccessControlEntry(adminSession, sensitiveNodePath, anon, null,
        new String[] { JCR_ALL }, null, null);
    replaceAccessControlEntry(adminSession, sensitiveNodePath, everyone, null,
        new String[] { JCR_ALL }, null, null);
    replaceAccessControlEntry(adminSession, sensitiveNodePath,
        currentUser.getPrincipal(), null, new String[] { JCR_ALL }, null, null);
  }

  /**
   * @param slingRepository
   */
  protected void bindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = slingRepository;
  }

  /**
   * @param slingRepository
   */
  protected void unbindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = null;
  }
}
