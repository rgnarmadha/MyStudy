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

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import static org.mockito.Mockito.mock;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.user.UserConstants;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 *
 */
public class LinkOperationTest {

  private LinkOperation operation;
  private SlingRepository slingRepo;
  private Session session;

  @Before
  public void setUp() {
    operation = new LinkOperation();
    slingRepo = mock(SlingRepository.class);

    operation.slingRepository = slingRepo;
  }

  @Test
  public void testAnonRequest() throws RepositoryException {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    when(request.getRemoteUser()).thenReturn(UserConstants.ANON_USERID);
    HtmlResponse response = new HtmlResponse();
    operation.doRun(request, response, null);
    assertEquals(403, response.getStatusCode());
  }

  @SuppressWarnings(justification="its a mock", value={"UWF_UNWRITTEN_FIELD"})
  @Test
  public void testMissingResource() throws RepositoryException {
    // This shouldn't really happen, but hey!
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    HtmlResponse response = new HtmlResponse();

    ResourceResolver resolver = mock(ResourceResolver.class);
    when(resolver.adaptTo(Session.class)).thenReturn(session);

    Resource resource = mock(Resource.class);
    when(resource.adaptTo(Node.class)).thenReturn(null);
    when(request.getResource()).thenReturn(resource);
    when(request.getResourceResolver()).thenReturn(resolver);
    when(request.getRemoteUser()).thenReturn("john");

    operation.doRun(request, response, null);

    assertEquals(400, response.getStatusCode());
  }

  @Test
  public void testMissingUUID() throws RepositoryException {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    HtmlResponse response = new HtmlResponse();

    ResourceResolver resolver = mock(ResourceResolver.class);
    when(resolver.adaptTo(Session.class)).thenReturn(session);

    Resource resource = mock(Resource.class);
    Node node = new MockNode("/bla/bla");
    when(resource.adaptTo(Node.class)).thenReturn(node);

    when(request.getResource()).thenReturn(resource);
    when(request.getResourceResolver()).thenReturn(resolver);
    when(request.getRequestParameter("uuid")).thenReturn(null);
    when(request.getRemoteUser()).thenReturn("john");

    operation.doRun(request, response, null);

    assertEquals(400, response.getStatusCode());
    assertEquals("A link parameter has to be provided.", response.getStatusMessage());
  }

  @Test
  public void testProper() throws RepositoryException {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    HtmlResponse response = new HtmlResponse();

    ResourceResolver resolver = mock(ResourceResolver.class);
    when(resolver.adaptTo(Session.class)).thenReturn(session);

    Resource resource = mock(Resource.class);
    Node node = new MockNode("/bla/bla");
    when(resource.adaptTo(Node.class)).thenReturn(node);

    String uuid = "some-uuid";
    RequestParameter uuidParam = mock(RequestParameter.class);
    when(uuidParam.getString()).thenReturn(uuid);

    when(request.getResource()).thenReturn(resource);
    when(request.getResourceResolver()).thenReturn(resolver);
    when(request.getRequestParameter("uuid")).thenReturn(uuidParam);
    when(request.getRemoteUser()).thenReturn("john");

    operation.doRun(request, response, null);

    assertEquals(400, response.getStatusCode());
    assertEquals("A link parameter has to be provided.", response.getStatusMessage());
  }

}
