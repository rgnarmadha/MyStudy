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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestPathInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.connections.ConnectionException;
import org.sakaiproject.nakamura.api.connections.ConnectionManager;
import org.sakaiproject.nakamura.api.connections.ConnectionOperation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class ConnectionServletTest {

  @Mock
  private SlingHttpServletResponse response;
  @Mock
  private SlingHttpServletRequest request;
  private ConnectionServlet servlet;
  @Mock
  private ConnectionManager connectionManager;
  @Mock
  private EventAdmin eventAdmin;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    
    servlet = new ConnectionServlet();
    servlet.bindConnectionManager(connectionManager);
    servlet.eventAdmin = eventAdmin;

    when(request.getRemoteUser()).thenReturn("alice");
  }

  @After
  public void tearDown() {
    servlet.unbindConnectionManager(connectionManager);
  }

  @Test
  public void testMissingUserParameter() throws IOException {
    setTargetUserId(null, request);
    servlet.doPost(request, response);
    verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST,
        "targetUserId not found in the request, cannot continue without it being set.");
  }

  @Test
  public void testSuccesfullInvite() throws IOException, ConnectionException {
    setTargetUserId("bob", request);
    setSelector("invite", request);
    when(request.getParameterMap()).thenReturn(null);
    when(
        connectionManager.connect(null, null, "alice", "bob", ConnectionOperation.invite))
        .thenThrow(new ConnectionException(200, "done"));
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter w = new PrintWriter(baos);
    when(response.getWriter()).thenReturn(w);
    servlet.doPost(request, response);
    w.flush();
    String s = baos.toString("UTF-8");
    assertEquals(
        "<html><head></head><body><h1>Connection Status</h1><p>done</p></body></html>", s);
  }

  @Test
  public void testInvalidOperation() throws IOException {
    setTargetUserId("bob", request);
    setSelector("foo", request);
    servlet.doPost(request, response);
    verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST,
        "Invalid operation selector");
  }

  @Test
  public void testFailure() throws ConnectionException, IOException {
    setTargetUserId("bob", request);
    setSelector("invite", request);
    when(request.getParameterMap()).thenReturn(null);
    when(
        connectionManager.connect(null, null, "alice", "bob", ConnectionOperation.invite))
        .thenThrow(new ConnectionException(400, "failed"));

    servlet.doPost(request, response);
    verify(response).sendError(400, "failed");
  }

  /**
   * Mocks the selector string
   * 
   * @param selector
   * @param request
   */
  private void setSelector(final String selector, SlingHttpServletRequest request) {
    RequestPathInfo info = new RequestPathInfo() {

      public String getSuffix() {
        // TODO Auto-generated method stub
        return null;
      }

      public String[] getSelectors() {
        return new String[] { selector };
      }

      public String getSelectorString() {
        return selector;
      }

      public String getResourcePath() {
        // TODO Auto-generated method stub
        return null;
      }

      public String getExtension() {
        // TODO Auto-generated method stub
        return null;
      }
    };

    when(request.getRequestPathInfo()).thenReturn(info);
  }

  private void setTargetUserId(String target, SlingHttpServletRequest request) throws UnsupportedEncodingException {

    RequestParameter param;
    if (target == null) {
      param = null;
    } else {
      param = mock(RequestParameter.class);
      when(param.getString("UTF-8")).thenReturn(target);
    }
    when(request.getRequestParameter("targetUserId")).thenReturn(param);
  }

}
