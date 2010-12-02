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

import static org.sakaiproject.nakamura.api.site.SiteConstants.PARAM_ITEMS;
import static org.sakaiproject.nakamura.api.site.SiteConstants.PARAM_SORT;
import static org.sakaiproject.nakamura.api.site.SiteConstants.PARAM_START;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.site.AuthorizableKey;
import org.sakaiproject.nakamura.api.site.GroupKey;
import org.sakaiproject.nakamura.api.site.Sort;
import org.sakaiproject.nakamura.api.site.UserKey;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>SiteServiceGetServlet</code>
 */
@Component(immediate = true, label = "%site.membersServlet.label", description = "%site.membersServlet.desc")
@SlingServlet(resourceTypes = "sakai/site", methods = "GET", selectors = "members", extensions = { "json" }, generateComponent = false)
@ServiceDocumentation(name = "Site Members Servlet", description = {
    "Will build the membership tree for a site.",
    "If a group is encountered and the maximum level has not been reached yet, it will be exploded.",
    "Just as with the JsonRenderer, you are able to specifiy the amount of levels you want to fetch. This can be done by specifying either a number or the string infinity" }, shortDescription = "Get the members of a site.", bindings = @ServiceBinding(type = BindingType.TYPE, bindings = { "sakai/site" }, selectors = @ServiceSelector(name = "members", description = "Initiates Joining workflow")), methods = @ServiceMethod(name = "GET", description = {
    "Gets the members of a site in json format paged and sorted",
    "Examples<br>",
    "<pre>curl http://user:pass@localhost:8080/sites/physics.members.json</pre><br />",
    "<pre>curl http://user:pass@localhost:8080/sites/physics.2.members.json</pre><br />",
    "<pre>curl http://user:pass@localhost:8080/sites/physics.infinity.members.json</pre><br />" }, parameters = {
    @ServiceParameter(name = "page", description = "The page number to list."),
    @ServiceParameter(name = "items", description = "The number of items per page."),
    @ServiceParameter(name = "sort", description = "one or more sort properties, processed in order.")

}, response = {
    @ServiceResponse(code = 200, description = "The body will contain a sorted, pages list of members in json format."),
    @ServiceResponse(code = 204, description = "When A site is not found"),
    @ServiceResponse(code = 400, description = { "If the location does not represent a site." }),
    @ServiceResponse(code = 403, description = "Current user is not allowed to list members."),
    @ServiceResponse(code = 404, description = "Resource was not found."),
    @ServiceResponse(code = 500, description = "Failure with HTML explanation.") }))
public class SiteMembersServlet extends AbstractSiteServlet {

  private static final long serialVersionUID = 5992872360072546374L;

  private static final Logger LOGGER = LoggerFactory.getLogger(SiteMembersServlet.class);

  @Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  @Property(value = "Gets lists of members for a site")
  static final String SERVICE_DESCRIPTION = "service.description";

  private static final String INFINITY = "infinity";

  @Reference
  private ProfileService profileService;

  @Override
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(justification = "Exceptions are caught to ensure that the correct status code gets sent.", value = { "REC_CATCH_EXCEPTION" })
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    LOGGER.info("Got get to SiteServiceGetServlet");
    Node site = request.getResource().adaptTo(Node.class);
    if (site == null) {
      response.sendError(HttpServletResponse.SC_NO_CONTENT, "Couldn't find site node");
      return;
    }
    if (!getSiteService().isSite(site)) {
      String loc = request.getContextPath();
      try {
        loc = site.getPath();
      } catch (RepositoryException e) {
        // NOTHING to do here but keep going
      }
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Location (" + loc
          + ") does not represent site");
      return;
    }
    RequestParameter startParam = request.getRequestParameter(PARAM_START);
    RequestParameter itemsParam = request.getRequestParameter(PARAM_ITEMS);
    RequestParameter[] sortParam = request.getRequestParameters(PARAM_SORT);
    int start = 0;
    int items = 25;
    Sort[] sort = null;
    if (startParam != null) {
      try {
        start = Integer.parseInt(startParam.getString());
      } catch (NumberFormatException e) {
        LOGGER.warn("Cant parse {} as  {} ", PARAM_START, startParam.getString());
      }
    }
    if (itemsParam != null) {
      try {
        items = Integer.parseInt(itemsParam.getString());
      } catch (NumberFormatException e) {
        LOGGER.warn("Cant parse {} as  {} ", PARAM_ITEMS, startParam.getString());
      }
    }
    if (sortParam != null) {
      List<Sort> sorts = new ArrayList<Sort>();
      for (RequestParameter p : sortParam) {
        try {
          sorts.add(new Sort(p.getString()));
        } catch (IllegalArgumentException ie) {
          LOGGER.warn("Invalid sort parameter: " + p.getString());
        }
      }
      sort = sorts.toArray(new Sort[] {});
    }

    // The amount of levels that need to be extracted.
    // Copied from the JsonRendererServlet.
    int maxLevels = 0;
    final String[] selectors = request.getRequestPathInfo().getSelectors();
    if (selectors != null && selectors.length > 0) {
      final String level = selectors[selectors.length - 1];
      if (INFINITY.equals(level)) {
        maxLevels = -1;
      } else {
        try {
          maxLevels = Integer.parseInt(level);
        } catch (NumberFormatException nfe) {
          maxLevels = 0;
        }
      }
    }

    response.setContentType("application/json");
    response.setCharacterEncoding("utf-8");

    try {
      LOGGER.debug("Finding members for: {} ", site.getPath());
      try {

        AbstractCollection<AuthorizableKey> memberTree = getSiteService().getMembers(site, start, items, sort, maxLevels);

        Session session = site.getSession();

        JSONWriter w = new JSONWriter(response.getWriter());
        w.array();
        Iterator<AuthorizableKey> it = memberTree.iterator();
        while (it.hasNext()) {
          AuthorizableKey au = it.next();
          outputMember(au, w, session);
        }
        w.endArray();

      } catch (JSONException e) {
        LOGGER.error(e.getMessage(), e);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      } catch (RepositoryException e) {
        LOGGER.error(e.getMessage(), e);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      }
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
    return;
  }

  /**
   * @param au
   * @param w
   * @throws RepositoryException
   * @throws JSONException
   */
  protected void outputMember(AuthorizableKey au, JSONWriter w, Session session)
      throws RepositoryException, JSONException {
    if (au instanceof UserKey) {
      UserKey member = (UserKey) au;
      User u = member.getUser();
      w.object();
      w.key("type");
      w.value("user");
      ProfileService ps;
      String profilePath = profileService.getProfilePath(u);
      Node node = session.getNode(profilePath);
      ExtendedJSONWriter.writeNodeContentsToWriter(w, node);
      w.endObject();
    } else {
      w.object();
      w.key("type");
      w.value("group");
      w.key("profile");
      w.object();
      Group g = ((GroupKey) au).getGroup();
      String profilePath = profileService.getProfilePath(g);
      Node node = session.getNode(profilePath);
      ExtendedJSONWriter.writeNodeContentsToWriter(w, node);
      w.endObject();
      w.key("members");
      List<AuthorizableKey> children = ((GroupKey) au).getChildren();
      w.array();
      for (AuthorizableKey child : children) {
        outputMember(child, w, session);
      }
      w.endArray();
      w.endObject();
    }
  }

}
