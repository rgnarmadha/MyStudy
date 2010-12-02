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
package org.sakaiproject.nakamura.files;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class JcrInternalFileHandlerTest {

  private JcrInternalFileHandler handler;

  @Before
  public void setUp() {
    handler = new JcrInternalFileHandler();
  }

  @Test
  public void testUUID() throws ServletException, IOException, ItemNotFoundException,
      RepositoryException {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);

    String to = UUID.randomUUID().toString();
    String file = "/path/to/actual/file";

    Session session = mock(Session.class);
    Node mockNode = new MockNode(file);
    when(session.getNodeByIdentifier(to)).thenReturn(mockNode);

    ResourceResolver resolver = mock(ResourceResolver.class);
    when(resolver.adaptTo(Session.class)).thenReturn(session);

    when(request.getResourceResolver()).thenReturn(resolver);

    handler.handleFile(request, response, to);

    verify(response).sendRedirect(file);
  }

  @Test
  public void testPath() throws ServletException, IOException, ItemNotFoundException,
      RepositoryException {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);

    String file = "/path/to/actual/file";
    String to = file;

    Session session = mock(Session.class);
    Node mockNode = new MockNode(file);
    when(session.getNodeByIdentifier(to)).thenReturn(mockNode);

    ResourceResolver resolver = mock(ResourceResolver.class);
    when(resolver.adaptTo(Session.class)).thenReturn(session);

    when(request.getResourceResolver()).thenReturn(resolver);

    handler.handleFile(request, response, to);

    verify(response).sendRedirect(file);
  }

  @Test
  public void testNotFound() throws ServletException, IOException, ItemNotFoundException,
      RepositoryException {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);

    String to = UUID.randomUUID().toString();

    Session session = mock(Session.class);
    when(session.getNodeByIdentifier(to)).thenThrow(new ItemNotFoundException());

    ResourceResolver resolver = mock(ResourceResolver.class);
    when(resolver.adaptTo(Session.class)).thenReturn(session);

    when(request.getResourceResolver()).thenReturn(resolver);

    handler.handleFile(request, response, to);

    verify(response).sendError(HttpServletResponse.SC_NOT_FOUND,
        "This file has been removed.");
  }
  
  @Test
  public void testException() throws ServletException, IOException, ItemNotFoundException, RepositoryException {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);
    String to = UUID.randomUUID().toString();
    ResourceResolver resolver = mock(ResourceResolver.class);
    Session session = mock(Session.class);
    when(session.getNodeByIdentifier(to)).thenThrow(new RepositoryException("test exception"));
    when(request.getResourceResolver()).thenReturn(resolver);
    
    handler.handleFile(request, response, to);
    verify(response).sendRedirect(to);
  }

}
