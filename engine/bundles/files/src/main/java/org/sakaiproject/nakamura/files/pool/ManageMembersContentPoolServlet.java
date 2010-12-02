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
package org.sakaiproject.nakamura.files.pool;

import static javax.jcr.security.Privilege.JCR_ALL;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_MEMBERS_NODE;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_USER_MANAGER;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_USER_RT;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_USER_VIEWER;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import javax.servlet.ServletException;

@ServiceDocumentation(
  name = "Manage Members Content Pool Servlet",
  description = "List and manage the managers and viewers for a file in the content pool.",
  bindings = {
    @ServiceBinding(type = BindingType.TYPE,
      bindings = {
        "sakai/pooled-content"
      },
      selectors = {
        @ServiceSelector(name = "members", description = "Binds to the selector members."),
        @ServiceSelector(name = "detailed", description = "(optional) Provides more detailed profile information."),
        @ServiceSelector(name = "tidy", description = "(optional) Provideds 'tidy' (formatted) JSON output.")
      }
    )
  },
  methods = {
    @ServiceMethod(name = "GET", description = "Retrieves a list of members.",
      response = {
        @ServiceResponse(code = 200, description = "All processing finished successfully.  Output is in the JSON format."),
        @ServiceResponse(code = 500, description = "Any exceptions encountered during processing.")
      }
    ),
    @ServiceMethod(name = "POST", description = "Manipulate the member list for a file.",
      parameters = {
        @ServiceParameter(name = ":manager", description = "Set the managers on the ACL of a file."),
        @ServiceParameter(name = ":viewer", description = "Set the viewers on the ACL of a file.")
      },
      response = {
        @ServiceResponse(code = 200, description = "All processing finished successfully."),
        @ServiceResponse(code = 401, description = "POST by anonymous user."),
        @ServiceResponse(code = 500, description = "Any exceptions encountered during processing.")
      }
    )
  }
)
@SlingServlet(methods = { "GET", "POST" }, resourceTypes = { "sakai/pooled-content" }, selectors = { "members" })
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Manages the Managers and Viewers for pooled content.") })
public class ManageMembersContentPoolServlet extends AbstractContentPoolServlet {

  private static final long serialVersionUID = 3385014961034481906L;
  private static final Logger LOGGER = LoggerFactory
      .getLogger(ManageMembersContentPoolServlet.class);

  @Reference
  protected transient SlingRepository slingRepository;

  @Reference
  protected transient ProfileService profileService;

  /**
   * Retrieves the list of members.
   *
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      // Get hold of the actual file.
      Node node = request.getResource().adaptTo(Node.class);
      Session session = node.getSession();

      // Search queries don't report errors when they can't reach protected
      // nodes. Try to fetch the members node so as to generate an exception
      // when the current user is not allowed to see the manager and viewer
      // lists.
      session.getNode(getMembersPath(node.getPath()));

      // Get hold of the members node that is under the file.
      // This node contains a list of managers and viewers.
      Map<String, Boolean> users = getMembers(node);

      UserManager um = AccessControlUtil.getUserManager(session);

      boolean detailed = false;
      boolean tidy = false;
      for (String selector : request.getRequestPathInfo().getSelectors()) {
        if ("detailed".equals(selector)) {
          detailed = true;
        } else if ("tidy".equals(selector)) {
          tidy = true;
        }
      }

      // Loop over the sets and output it.
      ExtendedJSONWriter writer = new ExtendedJSONWriter(response.getWriter());
      writer.setTidy(tidy);
      writer.object();
      writer.key("managers");
      writer.array();
      for (Entry<String, Boolean> entry : users.entrySet()) {
        if (entry.getValue()) {
          writeProfileMap(session, um, writer, entry, detailed);
        }
      }
      writer.endArray();
      writer.key("viewers");
      writer.array();
      for (Entry<String, Boolean> entry : users.entrySet()) {
        if (!entry.getValue()) {
          writeProfileMap(session, um, writer, entry, detailed);
        }
      }
      writer.endArray();
      writer.endObject();
    } catch ( PathNotFoundException e ) {
      response.sendError(SC_INTERNAL_SERVER_ERROR, "Could not lookup ACL list.");
      LOGGER.warn(e.getMessage());
    } catch (RepositoryException e) {
      response.sendError(SC_INTERNAL_SERVER_ERROR, "Could not lookup ACL list.");
      LOGGER.error(e.getMessage(), e);
    } catch (JSONException e) {
      response.sendError(SC_INTERNAL_SERVER_ERROR, "Failed to generate proper JSON.");
      LOGGER.error(e.getMessage(), e);
    }

  }

  private void writeProfileMap(Session session, UserManager um,
      ExtendedJSONWriter writer, Entry<String, Boolean> entry, boolean detailed)
      throws RepositoryException, JSONException {
    Authorizable au = um.getAuthorizable(entry.getKey());
    if (au != null) {
      ValueMap profileMap = null;
      if (detailed) {
        profileMap = profileService.getProfileMap(au, session);
      } else {
        profileMap = profileService.getCompactProfileMap(au, session);
      }
      if (profileMap != null) {
        writer.valueMap(profileMap);
      }
    } else {
      writer.object();
      writer.key("userid");
      writer.value(entry.getKey());
      writer.endObject();
    }
  }

  /**
   * Manipulate the member list for this file.
   *
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    // Anonymous users cannot do anything.
    // This is just a safety check really, they SHOULD NOT even be able to get to this
    // point.
    if (UserConstants.ANON_USERID.equals(request.getRemoteUser())) {
      response.sendError(SC_UNAUTHORIZED, "Anonymous users cannot manipulate content.");
      return;
    }

    Session adminSession = null;
    try {
      // Get the node.
      Node node = request.getResource().adaptTo(Node.class);
      Session session = node.getSession();
      String nodePath = node.getPath();

      // Make sure the current user is allowed to see the manager and viewer
      // lists. If not, they are not allowed to change the lists. We check
      // by trying to fetch the members node. If this fails, an exception will
      // be thrown and processing will end.
      String membersPath = getMembersPath(nodePath);
      session.getNode(membersPath);

      // We need an admin session because we might only have READ access on this node.
      // Yes, that is sufficient to share a file with somebody else.
      // We also re-fetch the node because we need to make some changes to the underlying
      // structure.
      // Only the admin has WRITE on that structure.
      adminSession = slingRepository.loginAdministrative(null);
      node = adminSession.getNode(nodePath);

      // If the current user has the ability to read the members node (that is, they are
      // either a viewer or a manager), then they can modify the list of viewers.
      updateMembers(request, adminSession, nodePath, ":viewer",
          POOLED_CONTENT_USER_VIEWER);

      // If the current user has manager-level access rights, then they can
      // modify the list of managers.
      AccessControlManager acm = AccessControlUtil.getAccessControlManager(session);
      boolean isManagerEditor = acm.hasPrivileges(nodePath,
          new Privilege[] { acm.privilegeFromName(JCR_ALL) });
      if (isManagerEditor) {
        updateMembers(request, adminSession, nodePath, ":manager",
            POOLED_CONTENT_USER_MANAGER);
      }

      // Persist any changes.
      if (adminSession.hasPendingChanges()) {
        adminSession.save();
      }
      response.setStatus(SC_OK);
    } catch (RepositoryException e) {
      LOGGER
          .error("Could not set some permissions on [{}] Cause:{}",request.getPathInfo(), e.getMessage());
      LOGGER
      .debug("Cause: ", e);
      response.sendError(SC_INTERNAL_SERVER_ERROR, "Could not set permissions.");
    } finally {
      if (adminSession != null) {
        adminSession.logout();
      }
    }
  }

  /**
   * Update the content node's list of viewers or managers based on the specified request parameter
   * key and member type property.
   *
   * @param request
   *          The request that contains the request parameters.
   * @param session
   *          A session that can change permissions on the specified path.
   * @param filePath
   *          The path for which the permissions should be changed.
   * @param parameterKey
   *          The key that should be used to look for the request parameters. A key of
   *          'manager' will result in 2 parameters to be looked up.
   *          <ul>
   *          <li>manager : A multi-valued request parameter that contains the IDs of the
   *          principals that should be granted the specified privileges</li>
   *          <li>manager@Delete : A multi-valued request parameter that contains the IDs
   *          of the principals whose privileges should be revoked.</li>
   *          </ul>
   * @param memberType
   *          The member node property that indicates the type of access: viewer or manager.
   * @throws RepositoryException
   */
  private void updateMembers(SlingHttpServletRequest request, Session session,
      String filePath, String parameterKey, String memberType) throws RepositoryException {
    // Get all the IDs of the authorizables that should be added and removed from the
    // request.
    String[] toAdd = request.getParameterValues(parameterKey);
    Set<Authorizable> toAddSet = new HashSet<Authorizable>();
    String[] toDelete = request.getParameterValues(parameterKey + "@Delete");
    Set<Authorizable> toDeleteSet = new HashSet<Authorizable>();

    // Resolve the IDs to authorizables.
    UserManager um = AccessControlUtil.getUserManager(session);
    resolveNames(um, toAdd, toAddSet);
    resolveNames(um, toDelete, toDeleteSet);

    // Add the specified members.
    for (Authorizable au : toAddSet) {
      addMember(session, filePath, au, memberType);
    }

    // Remove the specified members.
    for (Authorizable au : toDeleteSet) {
      removeMember(session, filePath, au, memberType);
    }
  }

  /**
   * Resolves each string in the array of names and adds them to the set of authorizables.
   * Authorizables that cannot be found, will not be added to the set.
   *
   * @param um
   *          A UserManager that can be used to find authorizables.
   * @param names
   *          An array of strings that contain the names.
   * @param authorizables
   *          A Set of Authorizables where each principal can be added to.
   * @throws RepositoryException
   */
  private void resolveNames(UserManager um, String[] names,
      Set<Authorizable> authorizables) throws RepositoryException {
    if (names != null && names.length > 0) {
      for (String principalName : names) {
        if (!StringUtils.isEmpty(principalName)) {
          Authorizable au = um.getAuthorizable(principalName);
          if (au != null) {
            authorizables.add(au);
          }
        }
      }
    }
  }

  /**
   * Gets all the "members" for a file.
   *
   * @param node
   *          The node that represents the file.
   * @return A map where each key is a userid, the value is a boolean that states if it is
   *         a manager or not.
   * @throws RepositoryException
   */
  private Map<String, Boolean> getMembers(Node node) throws RepositoryException {
    Session session = node.getSession();
    Map<String, Boolean> users = new HashMap<String, Boolean>();

    // Perform a query that gets all the "member" nodes.
    String path = ISO9075.encodePath(node.getPath());
    StringBuilder sb = new StringBuilder("/jcr:root/");
    sb.append(path).append(POOLED_CONTENT_MEMBERS_NODE).append("//*[@").append(SLING_RESOURCE_TYPE_PROPERTY);
    sb.append("='").append(POOLED_CONTENT_USER_RT).append("']");
    QueryManager qm = session.getWorkspace().getQueryManager();
    Query q = qm.createQuery(sb.toString(), "xpath");
    QueryResult qr = q.execute();
    NodeIterator iterator = qr.getNodes();

    // Loop over the "member" nodes.
    while (iterator.hasNext()) {
      Node memberNode = iterator.nextNode();
      if (memberNode.hasProperty(POOLED_CONTENT_USER_MANAGER)) {
        users.put(memberNode.getName(), true);
      } else if (memberNode.hasProperty(POOLED_CONTENT_USER_VIEWER)) {
        users.put(memberNode.getName(), false);
      }
    }

    return users;
  }
}
