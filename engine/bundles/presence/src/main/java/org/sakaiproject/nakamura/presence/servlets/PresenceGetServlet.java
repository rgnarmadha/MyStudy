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

package org.sakaiproject.nakamura.presence.servlets;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.presence.PresenceService;
import org.sakaiproject.nakamura.api.presence.PresenceUtils;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;

import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@SlingServlet(resourceTypes = { "sakai/presence" }, generateComponent = true, generateService = true, methods = { "GET" }, extensions = { "json" })
@Properties(value = {
    @Property(name = "service.description", value = { "Gets the presence for the current user only." }),
    @Property(name = "service.vendor", value = { "The Sakai Foundation" }) })
@ServiceDocumentation(name = "Presence Servlet",
    description = "Gets presence for the current user only.",
    shortDescription="Gets the presence for the current user only.",
    bindings = @ServiceBinding(type = BindingType.TYPE,
        bindings = "sakai/presence",
        extensions = @ServiceExtension(name="json", description={
            "the presence information is returned as a json tree."
        })
    ),
    methods = {
         @ServiceMethod(name = "GET",
             description = {
                 "Gets the presence for the current user. The servlet is bound " +
                 "to a node of type sakai/presence although at the moment, there does not appear to be any information used from that " +
                 "path.",
                 "<pre>" +
                 "curl http://ieb:password@localhost:8080/var/presence.json\n" +
                 "{\n" +
                 "  \"user\": \"ieb\"  \n" +
                 "  \"sakai:status\": \"online\",\n" +
                 "  \"sakai:location\": \"At Home\",\n" +
                 "}\n" +
                 "</pre>"
         },
        response = {
             @ServiceResponse(code=200,description="On sucess a a json tree of the presence of the current user."),
             @ServiceResponse(code=401,description="The user is not logged in and the resource is protected"),
             @ServiceResponse(code=403,description="The user does not have permission to access the resource"),
           @ServiceResponse(code=404,description="The resource does not exist, or the target is not found"),
           @ServiceResponse(code=0,description="Any other status codes emmitted with have the meaning prescribed in the RFC")
         })
        })
public class PresenceGetServlet extends SlingSafeMethodsServlet {

  /**
   *
   */
  private static final long serialVersionUID = 919177623558565626L;


  private static final Logger LOGGER = LoggerFactory.getLogger(PresenceGetServlet.class);

  @Reference
  protected transient PresenceService presenceService;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    // get current user
    String user = request.getRemoteUser();
    Session session = request.getResourceResolver().adaptTo(Session.class);
    if ( session != null ) {
      user = session.getUserID();
    }
    if (user == null || UserConstants.ANON_USERID.equals(user) ) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
          "User must be logged in to check their status");
      return;
    }
    LOGGER.debug("GET to PresenceServlet (" + user + ")");

    try {

      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");

      Writer writer = response.getWriter();
      PresenceUtils.makePresenceJSON(writer, user, presenceService);
    } catch (JSONException e) {
      LOGGER.error(e.getMessage(), e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }

    return;
  }

}
