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
package org.sakaiproject.nakamura.docproxy;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.REPOSITORY_LOCATION;
import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.REPOSITORY_PROCESSOR;
import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.RT_EXTERNAL_REPOSITORY;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class DocumentProxyServletTest extends AbstractDocProxyServlet {

  private ExternalDocumentProxyServlet servlet;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    servlet = new ExternalDocumentProxyServlet();
    servlet.activate(componentContext);
    servlet.tracker = tracker;
  }

  @After
  public void tearDown() {
    servlet.deactivate(componentContext);
  }

  @Test
  public void testGet() throws ServletException, IOException, PathNotFoundException,
      RepositoryException, JSONException {
    Session session = createMock(Session.class);
    ResourceResolver resolver = createMock(ResourceResolver.class);
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);

    // Session
    expect(session.getItem("/docproxy/disk/README"))
        .andThrow(new PathNotFoundException());
    expect(session.getItem("/docproxy/disk")).andReturn(proxyNode);
    expect(resolver.adaptTo(Session.class)).andReturn(session);
    expect(session.getUserID()).andReturn("zach");

    // Request
    expect(request.getRequestURI()).andReturn("/docproxy/disk/README");
    expect(request.getResourceResolver()).andReturn(resolver);

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ServletOutputStream stream = new ServletOutputStream() {

      @Override
      public void write(int b) throws IOException {
        baos.write(b);
      }
    };
    expect(response.getOutputStream()).andReturn(stream);
    replay();

    servlet.doGet(request, response);

    String result = baos.toString("UTF-8");
    assertEquals("K2 docProxy test resource", result);
  }

  @Test
  public void testDocumentGet() throws ValueFormatException, VersionException,
      LockException, ConstraintViolationException, RepositoryException, IOException,
      ServletException {
    Session session = createMock(Session.class);
    ResourceResolver resolver = createMock(ResourceResolver.class);
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);

    Node documentNode = new MockNode("/docproxy/disk");
    documentNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY,
        RT_EXTERNAL_REPOSITORY);
    documentNode.setProperty(REPOSITORY_PROCESSOR, "disk");
    String readmePath = getClass().getClassLoader().getResource("README").getPath();
    currPath = readmePath.substring(0, readmePath.lastIndexOf("/"));
    documentNode.setProperty(REPOSITORY_LOCATION, currPath);

    // Session
    expect(session.getItem("/docproxy/disk/README")).andReturn(documentNode);
    expect(session.getItem("/docproxy/disk")).andReturn(proxyNode);
/*
    --- removed to match changed servlet

    expect(session.getNodeByIdentifier("proxyUUID")).andReturn(proxyNode);
*/
    expect(resolver.adaptTo(Session.class)).andReturn(session);
    expect(session.getUserID()).andReturn("zach");

    // Request
    expect(request.getRequestURI()).andReturn("/docproxy/disk/README");
    expect(request.getResourceResolver()).andReturn(resolver);

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ServletOutputStream stream = new ServletOutputStream() {

      @Override
      public void write(int b) throws IOException {
        baos.write(b);
      }
    };
    expect(response.getOutputStream()).andReturn(stream);
    replay();

    servlet.doGet(request, response);

    String result = baos.toString("UTF-8");
    assertEquals("K2 docProxy test resource", result);
  }

  @Test
  public void testNoProcessor() throws PathNotFoundException, RepositoryException,
      ServletException, IOException {
    Session session = createMock(Session.class);
    ResourceResolver resolver = createMock(ResourceResolver.class);
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);

    Node node = new MockNode("/docproxy/disk");
    node.setProperty(SLING_RESOURCE_TYPE_PROPERTY, RT_EXTERNAL_REPOSITORY);
    node.setProperty(REPOSITORY_PROCESSOR, "foo");
    // Session
    expect(session.getItem("/docproxy/disk/README"))
        .andThrow(new PathNotFoundException());
    expect(session.getItem("/docproxy/disk")).andReturn(node);
    expect(resolver.adaptTo(Session.class)).andReturn(session);

    // Request
    expect(request.getRequestURI()).andReturn("/docproxy/disk/README");
    expect(request.getResourceResolver()).andReturn(resolver);

    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown repository.");

    replay();

    servlet.doGet(request, response);
  }

}
