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

import static org.easymock.EasyMock.expect;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sakaiproject.nakamura.api.connections.ConnectionManager;
import org.sakaiproject.nakamura.api.connections.ConnectionState;
import org.sakaiproject.nakamura.api.presence.PresenceService;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.presence.PresenceServiceImplTest;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class PresenceUserServletTest extends AbstractEasyMockTest {
  private static final String CURRENT_USER = "jack";
  private PresenceService presenceService;
  private PresenceUserServlet servlet;
  private ConnectionManager connectionManager;
  private ProfileService profileService;

  @Before
  public void setUp() throws Exception {
    super.setUp();

    PresenceServiceImplTest test = new PresenceServiceImplTest();
    test.setUp();
    presenceService = test.getPresenceService();
    profileService = Mockito.mock(ProfileService.class);
    Mockito.when(
        profileService.getProfileMap(Mockito.any(Authorizable.class), Mockito
            .any(Session.class))).thenReturn(ValueMap.EMPTY);

    servlet = new PresenceUserServlet();
    servlet.presenceService = presenceService;
    servlet.connectionManager = connectionManager;
    servlet.profileService = profileService;
  }

  @After
  public void tearDown() throws Exception {
    servlet.presenceService = presenceService;
    servlet.connectionManager = connectionManager;
  }

  @Test
  public void testAnon() throws ServletException, IOException {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    expect(request.getRemoteUser()).andReturn(UserConstants.ANON_USERID);
    expect(request.getResourceResolver()).andReturn(resourceResolver);
    expect(resourceResolver.adaptTo(Session.class)).andReturn(null);
    response.sendError(401, "User must be logged in to check their status");
    replay();
    servlet.doGet(request, response);
  }

  @Test
  public void testBadRequest() throws ServletException, IOException,
      JSONException {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);

    expect(request.getRemoteUser()).andReturn(CURRENT_USER);
    expect(request.getResourceResolver()).andReturn(resourceResolver);
    expect(resourceResolver.adaptTo(Session.class)).andReturn(null);
    expect(request.getParameter("userid")).andReturn(null);
    response.sendError(HttpServletResponse.SC_BAD_REQUEST,
        "Userid must be specified to request a user's presence");
    replay();

    servlet.doGet(request, response);
  }

  @Test
  public void testRegularUser() throws ServletException, IOException,
      JSONException, PathNotFoundException, RepositoryException {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    String status = "busy";
    // Pick a uuid we have as a friend.
    String contact = "user-10";
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    JackrabbitSession session = createMock(JackrabbitSession.class);
    Node profileNode = createMock(Node.class);
    PropertyIterator propertyIterator = createMock(PropertyIterator.class);

    Authorizable au = createAuthorizable(contact, false, true);
    UserManager um = createUserManager(null, true, au);
    expect(session.getUserManager()).andReturn(um).anyTimes();
    expect(propertyIterator.hasNext()).andReturn(false);
    expect(profileNode.getProperties()).andReturn(propertyIterator);
    expect(profileNode.getPath()).andReturn("/profile/node/path").anyTimes();
    expect(profileNode.getName()).andReturn("profile_node_name").anyTimes();
    expect(resourceResolver.adaptTo(Session.class)).andReturn(session).times(2);
    expect(request.getResourceResolver()).andReturn(resourceResolver).times(2);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter printWriter = new PrintWriter(baos);

    presenceService.setStatus(contact, status);

    expect(request.getRemoteUser()).andReturn(CURRENT_USER);
    expect(session.getUserID()).andReturn(CURRENT_USER);
    expect(request.getParameter("userid")).andReturn(contact);
    bindConnectionManager();
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    expect(response.getWriter()).andReturn(printWriter);
    replay();

    servlet.doGet(request, response);
    printWriter.flush();
    String json = baos.toString();
    JSONObject o = new JSONObject(json);
    Assert.assertEquals(status, o.get(PresenceService.PRESENCE_STATUS_PROP));
  }

  @Test
  public void testNonContactUser() throws ServletException, IOException {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    Session session = createMock(Session.class);
    String status = "busy";
    // Pick a uuid we do NOT have as a friend.
    String contact = "user-51";
    presenceService.setStatus(contact, status);
    expect(request.getRemoteUser()).andReturn(CURRENT_USER);
    expect(request.getResourceResolver()).andReturn(resourceResolver);
    expect(resourceResolver.adaptTo(Session.class)).andReturn(session);
    expect(session.getUserID()).andReturn(CURRENT_USER);
    expect(request.getParameter("userid")).andReturn(contact);
    bindConnectionManager();
    response.sendError(HttpServletResponse.SC_FORBIDDEN,
        "Userid must be a contact.");

    replay();
    servlet.doGet(request, response);

  }

  public void bindConnectionManager() {
    connectionManager = createMock(ConnectionManager.class);
    List<String> friends = new ArrayList<String>();
    for (int i = 0; i < 50; i++) {
      friends.add("user-" + i);
    }
    expect(connectionManager.getConnectedUsers(CURRENT_USER, ConnectionState.ACCEPTED)).andReturn(
        friends);
    servlet.connectionManager = connectionManager;
  }

}
