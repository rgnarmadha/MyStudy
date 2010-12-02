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

import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.when;

import static org.mockito.Mockito.mock;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.files.FilesConstants;

import java.io.IOException;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;

/**
 *
 */
public class LinkServletTest {

  private LinkServlet servlet;

  @Before
  public void setUp() {
    servlet = new LinkServlet();
  }

  @Test
  public void testNonLink() throws RepositoryException, ServletException, IOException {

    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);

    String to = UUID.randomUUID().toString();
    String file = "/path/to/actual/file";

    Node linkNode = new MockNode("/path/to/link.doc");
    linkNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, FilesConstants.RT_SAKAI_LINK);
    linkNode.setProperty(FilesConstants.SAKAI_LINK, to);

    Session session = mock(Session.class);
    Node mockNode = new MockNode(file);
    when(session.getNodeByIdentifier(to)).thenReturn(mockNode);

    ResourceResolver resolver = mock(ResourceResolver.class);
    when(resolver.adaptTo(Session.class)).thenReturn(session);

    Resource resource = mock(Resource.class);
    when(resource.adaptTo(Node.class)).thenReturn(linkNode);

    when(request.getResource()).thenReturn(resource);
    when(request.getResourceResolver()).thenReturn(resolver);

    servlet.doGet(request, response);
    verify(response).sendRedirect(file);

  }

  @Test
  public void testFailure() throws RepositoryException, ServletException, IOException {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);
    Node linkNode = mock(Node.class);
    when(linkNode.hasProperty(FilesConstants.SAKAI_LINK)).thenThrow(
        new RepositoryException());

    Resource resource = mock(Resource.class);
    when(resource.adaptTo(Node.class)).thenReturn(linkNode);
    when(request.getResource()).thenReturn(resource);

    servlet.doGet(request, response);

    verify(response).sendError(500, "Unable to handle linked file.");
  }

}
