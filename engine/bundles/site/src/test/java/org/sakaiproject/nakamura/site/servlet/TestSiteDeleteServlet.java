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
package org.sakaiproject.nakamura.site.servlet;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.site.SiteService;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * TODO Since the most interesting logic occurs in classes outside the servlet itself, this
 * unit test is not particularly informative.
 * USE INTEGRATION TESTS TO VERIFY BEHAVIOR.
 */
@RunWith(MockitoJUnitRunner.class)
public class TestSiteDeleteServlet {
  @Mock
  private JackrabbitSession session;
  @Mock
  private SlingHttpServletRequest request;
  @Mock
  private SlingHttpServletResponse response;
  @Mock
  private Resource resource;
  @Mock
  private SiteService siteService;
  private SiteDeleteServlet servlet;
  
  @Before
  public void setup() throws IOException {
    when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
    when(request.getResource()).thenReturn(resource);
    servlet = new SiteDeleteServlet();
    servlet.bindSiteService(siteService);
  }
  
  @After
  public void teardown() {
    servlet.unbindSiteService(siteService);
  }
  
  @Test
  public void testSiteRemove() throws ServletException, IOException, RepositoryException {
    Node site = mock(Node.class);
    when(site.getSession()).thenReturn(session);
    when(session.hasPendingChanges()).thenReturn(true);
    when(resource.adaptTo(Node.class)).thenReturn(site);
    when(siteService.isSite(any(Item.class))).thenReturn(true);
    servlet.doPost(request, response);
    verify(site).remove();
    verify(response).setStatus(HttpServletResponse.SC_OK);
  }
  
  @Test
  public void testSiteRemoveException() throws ServletException, IOException, RepositoryException {
    Node site = mock(Node.class);
    when(site.getSession()).thenReturn(session);
    when(session.hasPendingChanges()).thenReturn(true);
    doThrow(new RepositoryException()).when(session).save();
    when(resource.adaptTo(Node.class)).thenReturn(site);
    when(siteService.isSite(any(Item.class))).thenReturn(true);
    servlet.doPost(request, response);
    verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  public void testNotSite() throws ServletException, IOException {
    Node nonSite = mock(Node.class);
    when(resource.adaptTo(Node.class)).thenReturn(nonSite);
    when(siteService.isSite(any(Item.class))).thenReturn(false);
    servlet.doPost(request, response);
    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
  }

  @Test
  public void testNoSuchSite() throws ServletException, IOException {
    when(resource.adaptTo(Node.class)).thenReturn(null);
    servlet.doPost(request, response);
    verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
  }
}
