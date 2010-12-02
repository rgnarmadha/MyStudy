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


package org.sakaiproject.nakamura.files.pool;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;



public class ContentPoolFilterTest {

  @Mock
  private SlingHttpServletRequest request;
  @Mock
  private SlingHttpServletResponse response;
  @Mock
  private FilterChain chain;
  @Mock
  private Resource resource;
  @Mock
  private ResourceResolver resourceResolver;
  @Mock
  private Session session;
  @Mock
  private Node node;
  
  public ContentPoolFilterTest() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testFilterNull() throws IOException, ServletException {
    ContentPoolFilter filter = new ContentPoolFilter();
    filter.doFilter(request, response, chain);
    Mockito.verify(chain).doFilter(request, response);
  }
  
  @Test
  public void testFilterPath() throws IOException, ServletException {
    ContentPoolFilter filter = new ContentPoolFilter();
 
    Mockito.when(request.getResource()).thenReturn(resource);
    Mockito.when(resource.getPath()).thenReturn("/p/3223525234");
    filter.doFilter(request, response, chain);
    Mockito.verify(chain).doFilter(request, response);
    
  }

  @Test
  public void testFilterPathAllowed() throws IOException, ServletException {
    ContentPoolFilter filter = new ContentPoolFilter();
 
    Mockito.when(request.getResource()).thenReturn(resource);
    Mockito.when(resource.getPath()).thenReturn("/_x/3223525234");
    filter.doFilter(request, response, chain);
    Mockito.verify(chain).doFilter(request, response);
    
  }

  @Test
  public void testFilterPathDenied() throws IOException, ServletException {
    ContentPoolFilter filter = new ContentPoolFilter();
 
    Mockito.when(request.getResource()).thenReturn(resource);
    Mockito.when(resource.getPath()).thenReturn("/_p/3223525234");
    ResourceMetadata resourceMetadata = new ResourceMetadata();
    Mockito.when(resource.getResourceMetadata()).thenReturn(resourceMetadata);
    Mockito.when(resource.getResourceResolver()).thenReturn(resourceResolver);
    Mockito.when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
    Mockito.when(session.getUserID()).thenReturn("ieb");
    Mockito.when(resource.adaptTo(Node.class)).thenReturn(node);
    
    filter.doFilter(request, response, chain);
    Mockito.verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Resource is protected");
    Mockito.verifyZeroInteractions(chain);
    
  }

  @Test
  public void testFilterPathAdminAllowed() throws IOException, ServletException {
    ContentPoolFilter filter = new ContentPoolFilter();
 
    Mockito.when(request.getResource()).thenReturn(resource);
    Mockito.when(resource.getPath()).thenReturn("/_p/3223525234");
    ResourceMetadata resourceMetadata = new ResourceMetadata();
    Mockito.when(resource.getResourceMetadata()).thenReturn(resourceMetadata);
    Mockito.when(resource.getResourceResolver()).thenReturn(resourceResolver);
    Mockito.when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
    Mockito.when(session.getUserID()).thenReturn("admin");
    
    filter.doFilter(request, response, chain);
    Mockito.verify(chain).doFilter(request, response);
    
  }

  @Test
  public void testFilterPathWebDavAllowed() throws IOException, ServletException {
    ContentPoolFilter filter = new ContentPoolFilter();
 
    Mockito.when(request.getResource()).thenReturn(resource);
    Mockito.when(resource.getPath()).thenReturn("/_p/3223525234");
    ResourceMetadata resourceMetadata = new ResourceMetadata();
    Mockito.when(resource.getResourceMetadata()).thenReturn(resourceMetadata);
    Mockito.when(resource.getResourceResolver()).thenReturn(resourceResolver);
    Mockito.when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
    Mockito.when(session.getUserID()).thenReturn("ieb");
    Mockito.when(resource.adaptTo(Node.class)).thenReturn(null);
    
    filter.doFilter(request, response, chain);
    Mockito.verify(chain).doFilter(request, response);
    
  }

}
