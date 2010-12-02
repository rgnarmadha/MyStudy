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
package org.sakaiproject.nakamura.site.servlet;

import static javax.jcr.security.Privilege.JCR_ALL;
import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.sakaiproject.nakamura.api.site.SiteConstants.AUTHORIZABLES_SITE_IS_MAINTAINER;
import static org.sakaiproject.nakamura.api.site.SiteConstants.AUTHORIZABLES_SITE_NODENAME;
import static org.sakaiproject.nakamura.api.site.SiteConstants.AUTHORIZABLES_SITE_NODENAME_SINGLE;
import static org.sakaiproject.nakamura.api.site.SiteConstants.AUTHORIZABLES_SITE_PRINCIPAL_NAME;
import static org.sakaiproject.nakamura.api.site.SiteConstants.PARAM_SITE_PATH;
import static org.sakaiproject.nakamura.api.site.SiteConstants.RT_SITE_AUTHORIZABLE;
import static org.sakaiproject.nakamura.api.site.SiteConstants.SAKAI_IS_SITE_TEMPLATE;
import static org.sakaiproject.nakamura.api.site.SiteConstants.SAKAI_SITE_TEMPLATE;
import static org.sakaiproject.nakamura.api.site.SiteConstants.SITES;
import static org.sakaiproject.nakamura.api.site.SiteConstants.SITES_CONTAINER_RESOURCE_TYPE;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.jackrabbit.usermanager.impl.resource.AuthorizableResourceProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.site.create.ACE;
import org.sakaiproject.nakamura.site.create.GroupToCreate;
import org.sakaiproject.nakamura.site.create.SiteTemplateBuilder;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.sakaiproject.nakamura.util.StringUtils;
import org.sakaiproject.nakamura.version.VersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@SlingServlet(methods = { "POST" }, resourceTypes = { "sling/servlet/default",
    SITES_CONTAINER_RESOURCE_TYPE }, selectors = { "template" }, generateComponent = true, generateService = true)
public class CreateSiteServlet extends SlingAllMethodsServlet {

  private static final long serialVersionUID = 6687687185254684084L;
  private static final Logger LOGGER = LoggerFactory.getLogger(CreateSiteServlet.class);

  @Reference
  protected transient SiteService siteService;
  @Reference
  protected transient VersionService versionService;
  @Reference
  protected transient SlingRepository slingRepository;
  @Reference
  private AuthorizablePostProcessService postProcessService;

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    Node templateNode = null;
    JSONObject siteJSON = null;
    ResourceResolver resolver = request.getResourceResolver();
    Session session = resolver.adaptTo(Session.class);
    String currentUser = session.getUserID();

    String templatePath = null;

    // Do the necessary checks to see if this request is valid.
    if (UserConstants.ANON_USERID.equals(currentUser)) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    // Check path values.
    String sitePath = getSitePath(request, response);
    if (sitePath == null) {
      return;
    }
    LOGGER.debug("The sitePath is: {}", sitePath);

    try {
      // If we base this site on a template, make sure it exists.
      RequestParameter siteTemplateParam = request
          .getRequestParameter(SAKAI_SITE_TEMPLATE);
      if (siteTemplateParam != null) {
        templatePath = siteTemplateParam.getString();
        if (!session.itemExists(templatePath)) {
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The parameter "
              + SAKAI_SITE_TEMPLATE + " must be set to a site template");
          return;
        }
        // make sure it is a template site.
        templateNode = (Node) session.getItem(templatePath);
        if (!siteService.isSiteTemplate(templateNode)) {
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The parameter "
              + SAKAI_SITE_TEMPLATE + " must be set to a site which has the "
              + SAKAI_IS_SITE_TEMPLATE + " set.");
          return;
        }
      }

      RequestParameter siteParam = request.getRequestParameter("site");
      siteJSON = new JSONObject(siteParam.getString("UTF-8"));

    } catch (JSONException e) {
      LOGGER.error("Provided JSON object was invalid.", e);
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "The provided json was invalid.");
      return;
    } catch (RepositoryException e) {
      LOGGER.error("Caught a RepositoryException when trying to create a site.", e);
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "The path to the template is invalid.");
      return;
    }

    // Perform the actual creation.
    try {
      if (templatePath != null) {
        createSiteFromTemplate(session, sitePath, templateNode, siteJSON, resolver);
      }

    } catch (Exception e) {
      LOGGER.error("Could not create site via templating engine.", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    }
  }

  /**
   * Creates a site based on a template structure and a JSONObject provided by the UI.
   * 
   * @param session
   *          A session that allows us to create sites.
   * @param sitePath
   *          The path where the site should be created.
   * @param templateNode
   *          The node that represents the template structure.
   * @param siteJSON
   *          The JSON object that contains the data to be filled in at various points in
   *          the template node structure.
   * @param resolver
   *          A resource resolver to do queries against.
   * @return The site node.
   * @throws RepositoryException
   *           Failed to create the site.
   */
  protected Node createSiteFromTemplate(Session session, String sitePath,
      Node templateNode, JSONObject siteJSON, ResourceResolver resolver)
      throws RepositoryException {
    SiteTemplateBuilder builder = new SiteTemplateBuilder(templateNode, siteJSON);

    Session adminSession = null;
    Node siteNode = null;
    try {
      // Login trough the admin session to create the site node and the groups.
      adminSession = slingRepository.loginAdministrative(null);

      // Create the sitenode.
      siteNode = JcrUtils.deepGetOrCreateNode(adminSession, sitePath);

      // We give the current user JCR_ALL on the sitenode so he/she can create the
      // necessary structure.
      // We will remove the ACL later, the user just has to add him or herself to the
      // managers group if he wants access.
      UserManager um = AccessControlUtil.getUserManager(session);
      Principal p = um.getAuthorizable(session.getUserID()).getPrincipal();
      String[] privs = new String[] { JCR_ALL };
      AccessControlUtil.replaceAccessControlEntry(adminSession, sitePath, p, privs, null,
          null, null);

      // Create the groups.
      createGroups(builder, adminSession, siteNode);

      // Save the admin changes so that they can be picked up by our other session.
      adminSession.save();

      // We refresh the current session so that the changes that the admin made (creating
      // site node, setting JCR_ALL permission) are available to us.
      // After this point the admin session should NOT be used for anything else besides
      // removing the JCR_ALL permission.
      session.refresh(true);
      // Grab the site node trough our own session.
      siteNode = session.getNode(sitePath);

      // We get the site node trough our own session, so we can modify it.
      // siteNode = session.getNode(sitePath);
      // Create the site structure.
      createSiteStructure(builder, siteNode);

      // Save everything we've done.
      if (session.hasPendingChanges()) {
        session.save();
      }

      // Remove the current user his JCR_ALL privileges.
      // If the user is not in the manager group, he will NOT be able to edit the site
      // anymore!
      AccessControlUtil.replaceAccessControlEntry(adminSession, sitePath, p, null, null,
          privs, null);

      if (adminSession.hasPendingChanges()) {
        adminSession.save();
      }

      return siteNode;
    } finally {
      if (adminSession != null)
        adminSession.logout();
    }
  }

  /**
   * Creates the structure for the site.
   * 
   * @param builder
   *          The TemplateBuilder that contains the Map of nodes that needs to be created.
   * @param siteNode
   *          The Node that represents the site.
   * @param adminSession
   *          The admin session, this will only be used for setting ACLs.
   * @throws RepositoryException
   */
  protected void createSiteStructure(SiteTemplateBuilder builder, Node siteNode)
      throws RepositoryException {
    Map<String, Object> structure = builder.getSiteMap();
    handleNode(structure, siteNode);
  }

  /**
   * Converts a map into a node structure.
   * 
   * @param structure
   *          A Map that represents the node structure. The values can be of the following
   *          types: Value, Value[], Map<String, Object>
   * @param node
   *          The node where the map should be applied on.
   * @throws RepositoryException
   */
  @SuppressWarnings("unchecked")
  protected void handleNode(Map<String, Object> structure, Node node)
      throws RepositoryException {
    Session session = node.getSession();
    // Handle the ACEs before we do anything else.
    if (structure.containsKey("rep:policy")) {
      List<ACE> lst = (List<ACE>) structure.get("rep:policy");
      for (ACE ace : lst) {
        AccessControlUtil.replaceAccessControlEntry(session, node.getPath(), ace
            .getPrincipal(), ace.getGrantedPrivileges(), ace.getDeniedPrivileges(), null,
            null);
      }
    }
    if (structure.containsKey(JCR_MIXINTYPES)) {
      Value[] mixins = (Value[]) structure.get(JCR_MIXINTYPES);
      for (Value v : mixins) {
        String mixin = v.getString();
        if (node.canAddMixin(mixin)) {
          node.addMixin(v.getString());
        }
      }
    }

    for (Entry<String, Object> entry : structure.entrySet()) {

      String key = entry.getKey();
      if (key.equals(JCR_PRIMARYTYPE) || key.equals(JCR_MIXINTYPES)) {
        continue;
      }

      // Handle the properties ..
      if (entry.getValue() instanceof Value) {
        node.setProperty(key, (Value) entry.getValue());
      } else if (entry.getValue() instanceof Value[]) {
        node.setProperty(key, (Value[]) entry.getValue());
      }

      // Handle the child nodes.
      else if (entry.getValue() instanceof Map<?, ?>) {
        Map<String, Object> map = (Map<String, Object>) entry.getValue();
        String nt = "nt:unstructured";
        if (map.containsKey(JCR_PRIMARYTYPE)) {
          nt = ((Value) map.get(JCR_PRIMARYTYPE)).getString();
        }
        Node childNode = node.addNode(key, nt);
        handleNode(map, childNode);
      }
    }
  }

  /**
   * Creates the groups in the system.
   * 
   * @param builder
   * @param adminSession
   * @throws RepositoryException
   */
  protected void createGroups(SiteTemplateBuilder builder, Session adminSession,
      Node siteNode) throws RepositoryException {

    Node groupNodes = org.apache.jackrabbit.commons.JcrUtils.getOrAddNode(siteNode,
        AUTHORIZABLES_SITE_NODENAME);

    UserManager um = AccessControlUtil.getUserManager(adminSession);
    List<GroupToCreate> groups = builder.getGroups();
    int i = 0;
    for (GroupToCreate g : groups) {
      // Maybe the group already exists.
      // If it does, we use that one.
      Group group = getOrCreateGroup(g, um, siteNode, adminSession);

      // Set any additional properties
      Map<String, Object> map = g.getProperties();
      for (Entry<String, Object> entry : map.entrySet()) {
        if (entry.getValue() instanceof Value) {
          group.setProperty(entry.getKey(), (Value) entry.getValue());
        } else if (entry.getValue() instanceof Value[]) {
          group.setProperty(entry.getKey(), (Value[]) entry.getValue());
        }
      }

      // add the group node to the site groupNode.
      boolean isMaintainer = false;
      if (map.containsKey(AUTHORIZABLES_SITE_IS_MAINTAINER)) {
        Value v = (Value) map.get(AUTHORIZABLES_SITE_IS_MAINTAINER);
        isMaintainer = v.getBoolean();
      }

      Node groupNode = org.apache.jackrabbit.commons.JcrUtils.getOrAddNode(groupNodes,
          AUTHORIZABLES_SITE_NODENAME_SINGLE + i);

      groupNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, RT_SITE_AUTHORIZABLE);
      groupNode
          .setProperty(AUTHORIZABLES_SITE_PRINCIPAL_NAME, g.getPrincipal().getName());
      groupNode.setProperty(AUTHORIZABLES_SITE_IS_MAINTAINER, isMaintainer);
      i++;
    }
  }

  /**
   * Will lookup the group that is linked to the {@link Principal principal} provided in
   * the {@link GroupToCreate GroupToCreate} object. If it exist it will return that group
   * (unmodified!), if it doesn't it will create the new group and set the members and
   * managers. It will also set the site UUID as a sakai:sites property on the group node.
   * 
   * @param g
   *          The {@link GroupToCreate GroupToCreate} object that contains all the values
   *          to create a proper group.
   * @param um
   *          The user manager to retrieve authorizables and create a group.
   * @param site
   *          The node that represents our site.
   * @param session
   *          A session to create a group.
   * @return The group
   * @throws RepositoryException
   */
  protected Group getOrCreateGroup(GroupToCreate g, UserManager um, Node site,
      Session session) throws RepositoryException {
    Group group = (Group) um.getAuthorizable(g.getPrincipal());
    String siteID = site.getIdentifier();
    // Create the authorizable.
    // If the authorizable doesn't exist we do NOT modify it.
    if (group == null) {
      group = um.createGroup(g.getPrincipal());
      try {
        // Add all the members.
        addMembersToGroup(group, g.getManagers(), um);
        addMembersToGroup(group, g.getViewers(), um);

        // Set the properties for this group.
        group.setProperty(UserConstants.PROP_GROUP_MANAGERS, g.getManagers());
        group.setProperty(UserConstants.PROP_GROUP_VIEWERS, g.getViewers());
        group.setProperty(SITES, (Value) JcrUtils.createValue(siteID, session));
        String groupPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX
            + group.getID();
        
        postProcessService.process(group, session, ModificationType.CREATE);
      } catch (Exception e) {
        LOGGER.warn("Failed to process the group creation.", e);
      }
    }
    return group;
  }

  /**
   * @param group
   *          The group where the list of members should be added to.
   * @param members
   *          An array of {@link Value values} where a .getString() returns the principal
   *          string id.
   * @param um
   *          The UserManager that can be used to resolved strings to {@link Authorizable
   *          authorizables}.
   * @throws RepositoryException
   */
  protected void addMembersToGroup(Group group, Value[] members, UserManager um)
      throws RepositoryException {
    for (Value v : members) {
      Authorizable au = um.getAuthorizable(v.getString());
      if (au != null) {
        group.addMember(au);
      }
    }
  }

  /**
   * Parse the request to get the destination of the new or moved site.
   * 
   * @param request
   *          The request that contains the parameters for this create action.
   * @param response
   *          The response to write to if something was malformed or incorrect.
   * @return null if an error needs to be returned to the user
   * @throws IOException
   */
  protected String getSitePath(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws IOException {
    String resourceType = request.getResource().getResourceType();
    String sitePath = request.getRequestPathInfo().getResourcePath();
    // If the current target URL is a parent node for sites, construct the final
    // site path from it and the ":sitepath" parameter.
    if (SITES_CONTAINER_RESOURCE_TYPE.equals(resourceType)) {
      RequestParameter relativePathParam = request.getRequestParameter(PARAM_SITE_PATH);
      if (relativePathParam == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The parameter "
            + PARAM_SITE_PATH + " must be set to a relative path ");
        return null;
      }
      String relativePath = relativePathParam.getString();
      if (StringUtils.isEmpty(relativePath)) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The parameter "
            + PARAM_SITE_PATH + " must be set to a relative path ");
        return null;
      }
      if (sitePath.startsWith("/")) {
        sitePath = sitePath + relativePath;
      } else {
        sitePath = sitePath + "/" + relativePath;
      }
    }
    return sitePath;
  }

}
