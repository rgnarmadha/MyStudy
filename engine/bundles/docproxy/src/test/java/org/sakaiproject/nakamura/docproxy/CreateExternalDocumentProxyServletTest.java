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

import static org.easymock.EasyMock.expect;
import static org.sakaiproject.nakamura.docproxy.CreateExternalDocumentProxyServlet.PARAM_FILEBODY;
import static org.sakaiproject.nakamura.docproxy.CreateExternalDocumentProxyServlet.PARAM_FILENAME;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class CreateExternalDocumentProxyServletTest extends AbstractDocProxyServlet {

  private CreateExternalDocumentProxyServlet servlet;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    servlet = new CreateExternalDocumentProxyServlet();
    servlet.activate(componentContext);
    servlet.tracker = tracker;
  }

  @After
  public void tearDown() {
    servlet.deactivate(componentContext);
  }

  @Test
  public void testDoPost() throws ServletException, IOException, PathNotFoundException,
      RepositoryException {
    Session session = createMock(Session.class);
    ResourceResolver resolver = createMock(ResourceResolver.class);
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    Resource resource = createMock(Resource.class);

    expect(resource.adaptTo(Node.class)).andReturn(proxyNode);
    expect(request.getResource()).andReturn(resource);

    // Session
    expect(session.getItem("/docproxy/disk/README"))
        .andThrow(new PathNotFoundException());
    expect(session.getItem("/docproxy/disk")).andReturn(proxyNode);
    expect(resolver.adaptTo(Session.class)).andReturn(session);
    expect(request.getRemoteUser()).andReturn("admin");
    InputStream stream = getClass().getClassLoader().getResourceAsStream("README");

    // Parameters
    addStringRequestParameter(request, PARAM_FILENAME, "");
    addFileUploadRequestParameter(request, PARAM_FILEBODY, stream, 0, "sometest");

    replay();

    servlet.doPost(request, response);
  }

  @Test
  public void testMissingParameter() throws ServletException, IOException {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);

    expect(request.getRemoteUser()).andReturn("admin");
    expect(request.getRequestParameter(PARAM_FILENAME)).andReturn(null);
    expect(request.getRequestParameter(PARAM_FILEBODY)).andReturn(null);

    response.sendError(HttpServletResponse.SC_BAD_REQUEST,
        "Not all required parameters were supplied.");
    replay();
    servlet.doPost(request, response);
  }

  @Test
  public void testAnonPost() throws IOException, PathNotFoundException,
      RepositoryException, ServletException {
    Session session = createMock(Session.class);
    ResourceResolver resolver = createMock(ResourceResolver.class);
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);

    // Session
    expect(session.getItem("/docproxy/disk/README"))
        .andThrow(new PathNotFoundException());
    expect(session.getItem("/docproxy/disk")).andReturn(proxyNode);
    expect(resolver.adaptTo(Session.class)).andReturn(session);
    expect(request.getRequestURI()).andReturn("/docproxy/disk/README.metadata.json");
    expect(request.getResourceResolver()).andReturn(resolver);

    expect(request.getRemoteUser()).andReturn("anonymous");
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
        "Anonymous users can't post anything.");

    replay();
    servlet.doPost(request, response);
  }
}
