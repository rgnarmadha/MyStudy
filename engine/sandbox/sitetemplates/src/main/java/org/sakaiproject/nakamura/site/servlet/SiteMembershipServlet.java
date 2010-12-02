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
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.jcr.resource.JcrPropertyMap;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.site.SiteException;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>SiteMembershipServlet</code>
 */
@Component(immediate = true, label = "%site.membershipServlet.label", description = "%site.membershipServlet.desc")
@SlingServlet(paths = "/system/sling/membership", methods = "GET", generateComponent = false)
@ServiceDocumentation(name = "Site Membership Servlet", description = " Get the membership for the current user in json format.", shortDescription = "Get the site membership for the current user.", bindings = @ServiceBinding(type = BindingType.PATH, bindings = { "/system/sling/membership" }), methods = @ServiceMethod(name = "GET", description = {
    "Get the site membership for the current user, serialized in json format",
    "Example<br>"
        + "<pre>curl http://user:pass@localhost:8080//system/sling/membership</pre>" }, response = {
    @ServiceResponse(code = 200, description = "The body will contain json for the membership of the user."),
    @ServiceResponse(code = 500, description = "Failure with HTML explanation.") }))
public class SiteMembershipServlet extends AbstractSiteServlet {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(SiteMembershipServlet.class);
  private static final long serialVersionUID = 4874392318687088747L;

  @Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  @Property(value = "Gets a list of sites that the user is a member of")
  static final String SERVICE_DESCRIPTION = "service.description";

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    try {
      String u = request.getRemoteUser();
      Session session = request.getResourceResolver().adaptTo(Session.class);
      Map<String, List<Group>> membership = getSiteService().getMembership(session, u);

      ExtendedJSONWriter output = new ExtendedJSONWriter(response.getWriter());
      output.array();
      for (Entry<String, List<Group>> site : membership.entrySet()) {
        Node siteNode = session.getNodeByIdentifier(site.getKey());
        output.object();

        output.key("groups");

        output.array();
        for (Group g : site.getValue()) {
          output.value(g.getID());
        }
        output.endArray();

        output.key("siteref");
        output.value(siteNode.getPath());

        output.key("site");
        output.valueMap(new JcrPropertyMap(siteNode));

        output.endObject();
      }
      output.endArray();
    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    } catch (SiteException e) {
      LOGGER.warn(e.getMessage(), e);
      response.sendError(e.getStatusCode(), e.getMessage());
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
    return;
  }

}
