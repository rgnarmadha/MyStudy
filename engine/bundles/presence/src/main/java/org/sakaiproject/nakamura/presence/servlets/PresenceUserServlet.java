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
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.connections.ConnectionManager;
import org.sakaiproject.nakamura.api.connections.ConnectionState;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.presence.PresenceService;
import org.sakaiproject.nakamura.api.presence.PresenceUtils;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@SlingServlet(resourceTypes = { "sakai/presence" }, generateComponent = true, generateService = true, methods = { "GET" }, selectors = {"user"}, extensions = { "json" })
@Properties(value = {
    @Property(name = "service.description", value = { "Gets the presence for the current user and named user" }),
    @Property(name = "service.vendor", value = { "The Sakai Foundation" }) })
@ServiceDocumentation(name = "Presence User Servlet",
    description = "Gets presence for the current user and a named user including the public profile for the named contact.",
    shortDescription="Gets the presence for the current user and named user",
    bindings = @ServiceBinding(type = BindingType.TYPE,
        bindings = "sakai/presence",
        selectors = @ServiceSelector(name="user", description=" requires a selector of resource.user.json to get the presence for the user and one named user."),
        extensions = @ServiceExtension(name="json", description={
            "the presence information is returned as a json tree."
        })
    ),
    methods = {
         @ServiceMethod(name = "GET",
             description = {
                 "Gets the presence for the current user, and one named user, their presence and their profile. The servlet is bound " +
                 "to a node of type sakai/presence although at the moment, there does not appear to be any information used from that " +
                 "path.",
                 "<pre>" +
                 "curl http://ieb:password@localhost:8080/var/presence.contacts.json?userid=mark\n" +
                 "{\n" +
                 "  \"user\": \"ieb\"  \n" +
                 "  \"sakai:status\": \"online\",\n" +
                 "  \"sakai:location\": \"At Home\",\n" +
                 "  \"contacts\": [\n" +
                 "     {\n" +
                 "       \"user\": \"mark\"  \n" +
                 "       \"sakai:status\": \"online\",\n" +
                 "       \"sakai:location\": \"At Work\",\n" +
                 "       \"profile\": {\n" +
                 "         ... \n" +
                 "         <em>profile json tree</em>\n" +
                 "         ... \n" +
                 "       }\n" +
                 "     }\n" +
                 "  ]\n" +
                 "}\n" +
                 "</pre>"
         },
         parameters = {
             @ServiceParameter(name="userid", description={
                 "The userid that to include in the presence response."
             })
         },
        response = {
             @ServiceResponse(code=200,description="On sucess a a json tree of the presence for contacts."),
             @ServiceResponse(code=400,description="If the userid is not specified."),
             @ServiceResponse(code=401,description="The user is not logged in and the resource is protected"),
             @ServiceResponse(code=403,description="The user does not have permission to access the resource"),
           @ServiceResponse(code=404,description="The resource does not exist, or the target is not found"),
           @ServiceResponse(code=0,description="Any other status codes emmitted with have the meaning prescribed in the RFC")
         })
        })
public class PresenceUserServlet extends SlingSafeMethodsServlet {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(PresenceUserServlet.class);

  private static final long serialVersionUID = 11111111L;

  @Reference
  protected transient PresenceService presenceService;

  @Reference
  protected transient ConnectionManager connectionManager;

  @Reference
  protected transient ProfileService profileService;

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
    LOGGER.debug("GET to PresenceUserServlet (" + user + ")");
	String requestedUser = request.getParameter("userid");
	if (requestedUser == null){
		response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Userid must be specified to request a user's presence");
		return;
	}

	List<String> contacts = connectionManager.getConnectedUsers(user, ConnectionState.ACCEPTED);
	if (!contacts.contains(requestedUser)) {
	  response.sendError(HttpServletResponse.SC_FORBIDDEN,
        "Userid must be a contact.");
	  return;
	}

    try {

      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");

      Writer writer = response.getWriter();
      ExtendedJSONWriter output = new ExtendedJSONWriter(writer);
      session = request.getResourceResolver().adaptTo(Session.class);
      // start JSON object
      output.object();
      // put in the basics
      PresenceUtils.makePresenceJSON(output, requestedUser, presenceService, true);
      // add in the profile
      output.key("profile");
      UserManager um = AccessControlUtil.getUserManager(session);
      Authorizable au = um.getAuthorizable(requestedUser);
      ValueMap map = profileService.getProfileMap(au, session);
      output.valueMap(map);
      // finish it
      output.endObject();
    } catch (JSONException e) {
      LOGGER.error(e.getMessage(), e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    } catch (RepositoryException e) {
      LOGGER.error(e.getMessage(), e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }

    return;
  }

}
