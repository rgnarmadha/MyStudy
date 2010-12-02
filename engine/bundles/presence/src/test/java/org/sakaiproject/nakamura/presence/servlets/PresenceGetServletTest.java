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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.presence.PresenceService;
import org.sakaiproject.nakamura.presence.PresenceServiceImplTest;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.jcr.Session;
import javax.servlet.ServletException;

/**
 *
 */
public class PresenceGetServletTest extends AbstractEasyMockTest {

  private PresenceService presenceService;
  private PresenceGetServlet servlet;

  @Before
  public void setUp() throws Exception {
    super.setUp();

    PresenceServiceImplTest test = new PresenceServiceImplTest();
    test.setUp();
    presenceService = test.getPresenceService();
    servlet = new PresenceGetServlet();
    servlet.presenceService = presenceService;
  }

  @After
  public void tearDown() throws Exception {
    servlet.presenceService = presenceService;
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
  public void testRegularUser() throws ServletException, IOException,
      JSONException {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    Session session = createMock(Session.class);
    String uuid = "jack";
    String status = "busy";
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter printWriter = new PrintWriter(baos);

    presenceService.setStatus(uuid, status);

    expect(request.getRemoteUser()).andReturn(uuid);
    expect(request.getResourceResolver()).andReturn(resourceResolver);
    expect(resourceResolver.adaptTo(Session.class)).andReturn(session);
    expect(session.getUserID()).andReturn(uuid);
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
}
