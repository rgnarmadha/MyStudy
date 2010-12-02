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
package org.sakaiproject.nakamura.files.servlets;

import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintWriter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

public class CanModifyServletTest {
  private CanModifyServlet servlet;
  private SlingHttpServletRequest request;
  private SlingHttpServletResponse response;
  private PrintWriter writer;
  private Session session;
  private ResourceResolver resolver;
  private RequestParameter verbose;
  private Resource resource;
  private Node node;
  private AccessControlManager accessControlManager;
  private Privilege read;
  private Privilege write;
  private Privilege remove;
  private Privilege[] privileges;
  private RequestPathInfo pathInfo;
  private String[] selectors;

  @Before
  public void setUp() throws UnsupportedRepositoryOperationException,
      RepositoryException, IOException {
    String path = "/p/qrstuv";
    servlet = new CanModifyServlet();
    request = mock(SlingHttpServletRequest.class);
    response = mock(SlingHttpServletResponse.class);
    session = mock(Session.class);
    resolver = mock(ResourceResolver.class);
    when(resolver.adaptTo(Session.class)).thenReturn(session);
    when(request.getResourceResolver()).thenReturn(resolver);
    verbose = mock(RequestParameter.class);
    when(verbose.getString()).thenReturn("true");
    when(request.getRequestParameter("verbose")).thenReturn(verbose);

    resource = mock(Resource.class);
    when(request.getResource()).thenReturn(resource);

    node = mock(Node.class);
    when(node.getPath()).thenReturn(path);
    when(resource.adaptTo(Node.class)).thenReturn(node);

    accessControlManager = mock(AccessControlManager.class);
    when(AccessControlUtil.getAccessControlManager(session)).thenReturn(
        accessControlManager);
    read = mock(Privilege.class);
    when(read.getName()).thenReturn("jcr:read");
    when(accessControlManager.privilegeFromName(Privilege.JCR_READ)).thenReturn(read);
    write = mock(Privilege.class);
    when(write.getName()).thenReturn("jcr:write");
    when(accessControlManager.privilegeFromName(Privilege.JCR_WRITE)).thenReturn(write);
    remove = mock(Privilege.class);
    when(remove.getName()).thenReturn("jcr:removeNode");
    when(accessControlManager.privilegeFromName(Privilege.JCR_REMOVE_NODE)).thenReturn(
        remove);
    when(accessControlManager.hasPrivileges(any(String.class), any(Privilege[].class)))
        .thenReturn(true);
    privileges = new Privilege[] { read, write, remove };
    when(accessControlManager.getPrivileges(any(String.class))).thenReturn(privileges);

    pathInfo = mock(RequestPathInfo.class);
    when(request.getRequestPathInfo()).thenReturn(pathInfo);
    selectors = new String[] { "tidy" };
    when(pathInfo.getSelectors()).thenReturn(selectors);

    writer = mock(PrintWriter.class);
    when(response.getWriter()).thenReturn(writer);

    when(pathInfo.getResourcePath()).thenReturn(path);
  }

  @Test
  public void testDoGet() throws ServletException, IOException,
      UnsupportedRepositoryOperationException, RepositoryException {
    servlet.doGet(request, response);
    verify(response).setContentType("application/json");
    verify(writer, atLeastOnce()).write(anyString());
    verify(response).setStatus(HttpServletResponse.SC_OK);
  }

  @Test
  public void testDoPost() throws ServletException, IOException,
      UnsupportedRepositoryOperationException, RepositoryException {
    servlet.doPost(request, response);
    verify(response).setContentType("application/json");
    verify(writer, atLeastOnce()).write(anyString());
    verify(response).setStatus(HttpServletResponse.SC_OK);
  }

  @Test
  public void testNotFound() throws ServletException, IOException,
      UnsupportedRepositoryOperationException, RepositoryException {
    when(resource.adaptTo(Node.class)).thenReturn(null);
    servlet.doGet(request, response);
    verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
  }

  @Test
  public void testDoGetTerse() throws ServletException, IOException,
      UnsupportedRepositoryOperationException, RepositoryException {
    when(verbose.getString()).thenReturn("false");
    servlet.doGet(request, response);
    verify(response).setContentType("application/json");
    verify(writer, atLeastOnce()).write(anyString());
    verify(response).setStatus(HttpServletResponse.SC_OK);
  }

  @Test
  public void testDoFalse() throws ServletException, IOException,
      UnsupportedRepositoryOperationException, RepositoryException {
    when(verbose.getString()).thenReturn("false");
    when(accessControlManager.hasPrivileges(any(String.class), any(Privilege[].class)))
        .thenReturn(false);
    servlet.doGet(request, response);
    verify(response).setContentType("application/json");
    verify(writer, atLeastOnce()).write(anyString());
    verify(response).setStatus(HttpServletResponse.SC_OK);
  }

  @Test
  public void testRuntimeError() throws ServletException, IOException,
      UnsupportedRepositoryOperationException, RepositoryException {
    when(node.getPath()).thenThrow(
        new Error("bogus Throwable to test runtime error handling"));
    try {
      servlet.doGet(request, response);
    } catch (Throwable e) {
      assertNull("No exception should be thrown", e);
    }
    verify(response).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
        anyString());
  }

}
