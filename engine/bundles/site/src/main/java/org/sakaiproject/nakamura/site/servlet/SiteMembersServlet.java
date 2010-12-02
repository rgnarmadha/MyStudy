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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.api.site.Sort;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>SiteServiceGetServlet</code>
 */
@Component(immediate = true, label = "%site.membersServlet.label", description = "%site.membersServlet.desc")
@SlingServlet(resourceTypes = "sakai/site", methods = "GET", selectors = "members", generateComponent = false)
@ServiceDocumentation(name="Site Members Servlet",
    description=" Gets the members of a site, both groups and users",
    shortDescription="Get the members of a site.",
    bindings=@ServiceBinding(type=BindingType.TYPE,bindings={"sakai/site"},
        selectors=@ServiceSelector(name="members", description="Initiates Joining workflow")),
    methods=@ServiceMethod(name="GET",
        description={"Gets the members of a site in json format paged and sorted",
            "Example<br>" +
            "<pre>curl http://user:pass@localhost:8080/sites/physics101/year3.members</pre>"
        },
        parameters={
          @ServiceParameter(name="page", description="The page number to list."),
          @ServiceParameter(name="items", description="The number of items per page."),
          @ServiceParameter(name="sort", description="one or more sort properties, processed in order.")
        
        },
        response={
          @ServiceResponse(code=200,description="The body will contain a sorted, pages list of members in json format."),
          @ServiceResponse(code=204,description="When A site is not found"),
          @ServiceResponse(code=400,description={
              "If the location does not represent a site."
          }),
          @ServiceResponse(code=403,description="Current user is not allowed to list members."),
          @ServiceResponse(code=404,description="Resource was not found."),
          @ServiceResponse(code=500,description="Failure with HTML explanation.")}
    )) 
public class SiteMembersServlet extends AbstractSiteServlet {

  private static final Logger LOGGER = LoggerFactory.getLogger(SiteMembersServlet.class);
  private static final long serialVersionUID = 4874392318687088747L;

  @Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  @Property(value = "Gets lists of members for a site")
  static final String SERVICE_DESCRIPTION = "service.description";

  @Override
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(justification="Exceptions are caught to ensure that the correct status code gets sent.", value={"REC_CATCH_EXCEPTION"})
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
    RequestParameter startParam = request.getRequestParameter(SiteService.PARAM_START);
    RequestParameter itemsParam = request.getRequestParameter(SiteService.PARAM_ITEMS);
    RequestParameter[] sortParam = request.getRequestParameters(SiteService.PARAM_SORT);
    int start = 0;
    int items = 25;
    Sort[] sort = null;
    if (startParam != null) {
      try {
        start = Integer.parseInt(startParam.getString());
      } catch (NumberFormatException e) {
        LOGGER.warn("Cant parse {} as  {} ", SiteService.PARAM_START, startParam
            .getString());
      }
    }
    if (itemsParam != null) {
      try {
        items = Integer.parseInt(itemsParam.getString());
      } catch (NumberFormatException e) {
        LOGGER.warn("Cant parse {} as  {} ", SiteService.PARAM_ITEMS, startParam
            .getString());
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

    try {
      LOGGER.debug("Finding members for: {} ", site.getPath());
      AbstractCollection<User> users = getSiteService().getMembers(site, start, items, sort);
      Iterator<User> members = users.iterator();
      // LOGGER.info("Found members: ", members.hasNext());

      // get the list of group ids in this site
      Set<String> siteGroupIds = new HashSet<String>();
      Value[] vs = JcrUtils.getValues(site, SiteService.AUTHORIZABLE);
      for (Value value : vs) {
        if (value != null) {
          siteGroupIds.add(value.getString());
        }
      }

      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");

      try {
        ExtendedJSONWriter output = new ExtendedJSONWriter(response.getWriter());
        output.object();
        output.key("items");
        output.value(items);
        output.key("total");
        output.value(users.size());
        output.key("results");
        output.array();
        for (; members.hasNext();) {
          User u = members.next();
          Resource resource = request.getResourceResolver().resolve(
              PersonalUtils.getProfilePath(u));
          ValueMap map = null;
          if ( resource != null ) {
            map = resource.adaptTo(ValueMap.class);
          }
          if ( map == null ) {
            Map<String, Object> m = new HashMap<String, Object>();
            for ( Iterator<String> names = u.getPropertyNames(); names.hasNext(); ) {
              String n = names.next();
              Value[] v = u.getProperty(n);
              if ( v.length == 1 ) {
                m.put(n, v[0].getString());
              } else if ( v.length > 1 ) {
                String[] s = new String[v.length];
                for ( int i = 0; i < v.length; i++ ) {
                  s[i] = v[i].getString();
                }
                m.put(n, s);
              }
            }
            m.put("rep:userId", u.getID());
            map = new ValueMapDecorator(m);
          }
          
          
          // add in the listing of member group names -
          // http://jira.sakaiproject.org/browse/KERN-276
          Set<String> groupIds = null;
          Iterator<Group> groupsIterator = u.memberOf();
          if (groupsIterator != null && groupsIterator.hasNext()) {
            groupIds = new HashSet<String>();
            for (Iterator<Group> iterator = groupsIterator; iterator.hasNext();) {
              Group group = iterator.next();
              groupIds.add(group.getID());
            }
          }

          // create the JSON object
          output.object();
          if ( map != null ) {
            output.valueMapInternals(map);
          }
          // add in the extra fields if there are any
          if (groupIds != null && !groupIds.isEmpty()) {
            // filter the group ids so only the ones which are part of this site are shown
            for (Iterator<String> iterator = groupIds.iterator(); iterator.hasNext();) {
              String groupId = iterator.next();
              if (groupId.startsWith("g-")) {
                // only filtering group names
                if (!siteGroupIds.contains(groupId)) {
                  iterator.remove();
                }
              }
            }
            // now output the array of group ids
            output.key(SiteService.MEMBER_GROUPS);
            output.array();
            for (String name : groupIds) {
              output.value(name);
            }
            output.endArray();
          }
          output.endObject();
        }
        output.endArray();
        output.endObject();
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

}
