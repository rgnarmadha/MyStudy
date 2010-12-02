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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.presence.PresenceService;
import org.sakaiproject.nakamura.api.presence.PresenceStatus;
import org.sakaiproject.nakamura.presence.PresenceServiceImplTest;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import javax.jcr.Session;
import javax.servlet.ServletException;

/**
 *
 */
public class PresenceControlServletTest extends AbstractEasyMockTest {
  private static final String CURRENT_USER = "jack";
  private PresenceService presenceService;
  private PresenceControlServlet servlet;

  @Before
  public void setUp() throws Exception {
    super.setUp();

    PresenceServiceImplTest test = new PresenceServiceImplTest();
    test.setUp();
    presenceService = test.getPresenceService();

    servlet = new PresenceControlServlet();
    servlet.presenceService = presenceService;
  }

  @After
  public void tearDown() throws Exception {
    servlet.presenceService = null;
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
    response.sendError(401,
        "User must be logged in to ping their status and set location");
    replay();
    servlet.doPost(request, response);
  }

  @Test
  public void testLocation() throws ServletException, IOException,
      JSONException {
    presenceService.setStatus(CURRENT_USER, "tired");
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    Session session = createMock(Session.class);
    
    expect(request.getRemoteUser()).andReturn(CURRENT_USER);
    expect(request.getResourceResolver()).andReturn(resourceResolver);
    expect(resourceResolver.adaptTo(Session.class)).andReturn(session);
    expect(session.getUserID()).andReturn(CURRENT_USER);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter printWriter = new PrintWriter(baos);

    addParameterToRequest(request, PresenceService.PRESENCE_CLEAR, null);
    addParameterToRequest(request, PresenceService.PRESENCE_LOCATION_PROP,
        "loc");
    addParameterToRequest(request, PresenceService.PRESENCE_STATUS_PROP, "busy");
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    expect(response.getWriter()).andReturn(printWriter);
    replay();
    servlet.doPost(request, response);

    printWriter.flush();

    JSONObject o = new JSONObject(baos.toString("UTF-8"));
    Assert.assertEquals("loc", o.get("location"));
    Assert.assertEquals("busy", o.get("status"));
    Assert.assertEquals(presenceService.getStatus(CURRENT_USER), "busy");
  }

  @Test
  public void testClear() throws ServletException, IOException, JSONException {
    presenceService.setStatus(CURRENT_USER, "tired");
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    Session session = createMock(Session.class);
    
    expect(request.getRemoteUser()).andReturn(CURRENT_USER);
    expect(request.getResourceResolver()).andReturn(resourceResolver);
    expect(resourceResolver.adaptTo(Session.class)).andReturn(session);
    expect(session.getUserID()).andReturn(CURRENT_USER);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter printWriter = new PrintWriter(baos);

    // Set the clear parameter, doesn't matter what we set it to.
    addParameterToRequest(request, PresenceService.PRESENCE_CLEAR, "foo");
    addParameterToRequest(request, PresenceService.PRESENCE_LOCATION_PROP,
        "foo");
    addParameterToRequest(request, PresenceService.PRESENCE_STATUS_PROP, "foo");
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    expect(response.getWriter()).andReturn(printWriter);
    replay();
    servlet.doPost(request, response);

    printWriter.flush();

    JSONObject o = new JSONObject(baos.toString("UTF-8"));
    Assert.assertEquals("1", o.get("deleted"));
    Assert.assertEquals(presenceService.getStatus(CURRENT_USER),
        PresenceStatus.offline.name());

  }

  protected void addParameterToRequest(SlingHttpServletRequest request,
      String key, String value) throws UnsupportedEncodingException {
    RequestParameter param = createMock(RequestParameter.class);
    expect(param.getString("UTF-8")).andReturn(value).anyTimes();
    expect(request.getRequestParameter(key)).andReturn(param).anyTimes();
  }
}
