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
package org.sakaiproject.nakamura.connections.servlets;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.connections.ConnectionConstants;
import org.sakaiproject.nakamura.api.connections.ConnectionException;
import org.sakaiproject.nakamura.api.connections.ConnectionManager;
import org.sakaiproject.nakamura.api.connections.ConnectionOperation;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.osgi.EventUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.http.HttpServletResponse;

/**
 * Servlet interface to the personal connections/contacts service.
 */
@ServiceDocumentation(name="Personal Connection Servlet",
  description="Manage personal connections and contacts. " +
    "Maps to node of resourceType sakai/contactstore at the URL /_user/contacts. " +
    "Each new contact results in two new nodes of resourceType sakai/contact, one for the inviting user and one for the invited user. "+
    "These contacts can be retrieved by GET requests which specify a connection-status: "+
    "/_user/contacts/find?state=ACCEPTED, etc.",
  shortDescription="Manage personal connections/contacts",
  bindings=@ServiceBinding(type=BindingType.PATH,bindings="/_user/contacts/OTHER_USER",
      selectors={
      @ServiceSelector(name="invite",description="Invite other user to connect"),
      @ServiceSelector(name="accept",description="Accept invitation from other user"),
      @ServiceSelector(name="reject",description="Refuse invitation from other user"),
      @ServiceSelector(name="ignore",description="Ignore invitation from other user"),
      @ServiceSelector(name="block",description="Ignore this and any future invitations from other user"),
      @ServiceSelector(name="remove",description="Remove invitation or connection, allowing future connections"),
      @ServiceSelector(name="cancel",description="Cancel pending invitation to other user")
  },
  extensions={
    @ServiceExtension(name="html", description="All POST operations produce HTML")
  }),
  methods=@ServiceMethod(name="POST",
    description={"Manage a personal contact (a connection with another user), specifying an operation as a selector. ",
      "Examples:<br>" +
      "<pre>curl -u from_user:fromPwd -F toRelationships=Supervisor -F fromRelationships=Supervised " +
      "http://localhost:8080/_user/contacts/to_user.invite.html</pre>" +
      "<pre>curl -X POST -u to_user:toPwd http://localhost:8080/_user/contacts/from_user.accept.html</pre>"
      },
    parameters={
      @ServiceParameter(name="toRelationships", description="The type of connection from the inviting user's point of view (only for invite)"),
      @ServiceParameter(name="fromRelationships", description="The type of connection from the invited user's point of view (only for invite)"),
      @ServiceParameter(name="sakai:types", description="Relationship types without regard to point-of-view " +
          "(affects the current user's view of the connection on any POST; affects the other user's view only for invite)"),
      @ServiceParameter(name="",description="Additional parameters become connection node properties (optional)")
    },
    response={
      @ServiceResponse(code=200,description="Success."),
      @ServiceResponse(code=400,description="Failure due to illegal operation request."),
      @ServiceResponse(code=404,description="Failure due to unknown user."),
      @ServiceResponse(code=409,description="There was a data conflict that cannot be resolved without user input (Simultaneaus requests.)")
    }
  )
)
@SlingServlet(resourceTypes="sakai/contactstore",methods={"POST"}, 
    selectors={"invite", "accept", "reject", "ignore", "block", "remove", "cancel"})
@Properties(value = {
    @Property(name = "service.description", value = "Provides support for connection stores."),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
public class ConnectionServlet extends SlingAllMethodsServlet {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ConnectionServlet.class);
  private static final long serialVersionUID = 1112996718559864951L;

  private static final String TARGET_USERID = "targetUserId";

  @Reference
  protected transient ConnectionManager connectionManager;

  @Reference
  protected transient EventAdmin eventAdmin;

  protected void bindConnectionManager(ConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
  }

  protected void unbindConnectionManager(ConnectionManager connectionManager) {
    this.connectionManager = null;
  }


  /**
   * {@inheritDoc}
   * 
   * @throws IOException
   * 
   * @see org.sakaiproject.nakamura.resource.AbstractVirtualPathServlet#preDispatch(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse,
   *      org.apache.sling.api.resource.Resource, org.apache.sling.api.resource.Resource)
   */
  @SuppressWarnings("unchecked")
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws IOException {
    
    RequestParameter userParam = request.getRequestParameter(TARGET_USERID);
    if (userParam == null || userParam.getString("UTF-8").equals("")) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "targetUserId not found in the request, cannot continue without it being set.");
      return;
    }

    // current user
    String user = request.getRemoteUser();
    // User to connect to
    String targetUserId = userParam.getString("UTF-8");
    // Get the connection operation from the selector.
    String selector = request.getRequestPathInfo().getSelectorString();
    ConnectionOperation operation = ConnectionOperation.noop;
    try {
      operation = ConnectionOperation.valueOf(selector);
    } catch (IllegalArgumentException e) {
      operation = ConnectionOperation.noop;
    }

    // Nearly impossible to get a noop, but we'll check it anyway..
    if (operation == ConnectionOperation.noop) {
      response
          .sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid operation selector");
      return;
    }
    try {
      // Do the connection.
      LOGGER.debug("Connection {} {} ",new Object[]{user,targetUserId});
      connectionManager.connect(request.getParameterMap(), request.getResource(), user, targetUserId, operation);
    } catch (ConnectionException e) {
      if ( e.getCode() == 200 ) {
        PrintWriter writer = response.getWriter();
        writer.write("<html><head></head><body><h1>Connection Status</h1><p>");
        writer.write(e.getMessage());
        writer.write("</p></body></html>");
      } else {
        LOGGER.info("Connection exception: {}", e.getMessage());
        LOGGER.debug("Connection exception: {}", e);
        response.sendError(e.getCode(), e.getMessage());
      }
    }

    // Send an OSGi event. The value of the selector is the last part of the event topic.
    final Dictionary<String, String> properties = new Hashtable<String, String>();
    properties.put(UserConstants.EVENT_PROP_USERID, request.getRemoteUser());
    properties.put("target", userParam.getString("UTF-8"));
    String topic = ConnectionConstants.EVENT_TOPIC_BASE + operation.toString();
    EventUtils.sendOsgiEvent(properties, topic, eventAdmin);
  }
}
