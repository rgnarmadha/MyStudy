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

import junit.framework.Assert;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sakaiproject.nakamura.api.connections.ConnectionManager;
import org.sakaiproject.nakamura.api.connections.ConnectionState;
import org.sakaiproject.nakamura.api.presence.PresenceService;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.presence.PresenceServiceImplTest;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;
import javax.servlet.ServletException;

/**
 *
 */
public class PresenceContactsServletTest extends AbstractEasyMockTest {

  private static final String CURRENT_USER = "jack";
  private PresenceService presenceService;
  private PresenceContactsServlet servlet;
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

    servlet = new PresenceContactsServlet();
    servlet.presenceService = presenceService;
    servlet.profileService = profileService;
  }

  @Test
  public void testAnon() throws ServletException, IOException {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    Session session = createMock(Session.class);
    
    expect(request.getRemoteUser()).andReturn(null);
    expect(request.getResourceResolver()).andReturn(resourceResolver);
    expect(resourceResolver.adaptTo(Session.class)).andReturn(session);
    expect(session.getUserID()).andReturn(null);
    response.sendError(401, "User must be logged in to check their status");
    replay();
    PresenceGetServlet servlet = new PresenceGetServlet();
    servlet.doGet(request, response);
  }

  @Test
  public void testRegularUser() throws Exception {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    JackrabbitSession session = createMock(JackrabbitSession.class);

    
    expect(request.getRemoteUser()).andReturn(CURRENT_USER);
    expect(request.getResourceResolver()).andReturn(resourceResolver);
    expect(resourceResolver.adaptTo(Session.class)).andReturn(session);
    expect(session.getUserID()).andReturn(CURRENT_USER);
    
    
    
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter printWriter = new PrintWriter(baos);

    expect(resourceResolver.adaptTo(Session.class)).andReturn(session);
    expect(request.getResourceResolver()).andReturn(resourceResolver);

    expect(response.getWriter()).andReturn(printWriter);
    List<String> contacts = new ArrayList<String>();
    connectionManager = createMock(ConnectionManager.class);

    List<Authorizable> authorizables = new ArrayList<Authorizable>();
    for (int i = 0; i < 50; i++) {
      String uuid = "user-" + i;
      contacts.add(uuid);
      presenceService.setStatus(uuid, "busy");
      Node profileNode = createMock(Node.class);
      PropertyIterator propertyIterator = createMock(PropertyIterator.class);
      expect(propertyIterator.hasNext()).andReturn(false);
      expect(profileNode.getProperties()).andReturn(propertyIterator);
      expect(profileNode.getPath()).andReturn("/profile"+i+"/nodepath").anyTimes();
      expect(profileNode.getName()).andReturn("profile"+i+"nodename").anyTimes();
      Authorizable au = createAuthorizable(uuid, false, true);
      authorizables.add(au);
    }

    Authorizable[] auths = new Authorizable[authorizables.size()];
    UserManager um = createUserManager(null, true, authorizables.toArray(auths));
    expect(session.getUserManager()).andReturn(um).anyTimes();
    expect(
        connectionManager.getConnectedUsers(CURRENT_USER,
            ConnectionState.ACCEPTED)).andReturn(contacts);

    servlet.presenceService = presenceService;
    servlet.connectionManager = connectionManager;
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    replay();
    servlet.doGet(request, response);

    printWriter.flush();

    JSONObject o = new JSONObject(baos.toString("UTF-8"));
    Assert.assertEquals(50, o.getJSONArray("contacts").length());
    for (int i = 0; i < 50; i++) {
      Assert.assertEquals("busy", o.getJSONArray("contacts").getJSONObject(i)
          .getString(PresenceService.PRESENCE_STATUS_PROP));
    }
  }

}
