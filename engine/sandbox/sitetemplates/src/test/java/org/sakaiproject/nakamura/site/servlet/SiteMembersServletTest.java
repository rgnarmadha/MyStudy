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

import static org.sakaiproject.nakamura.api.site.SiteConstants.PARAM_SORT;

import static org.mockito.Mockito.mock;

import static org.mockito.Mockito.when;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.sakaiproject.nakamura.site.ACMEGroupStructure;
import org.sakaiproject.nakamura.site.SiteServiceImpl;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

/**
 *
 */
public class SiteMembersServletTest extends AbstractSiteServletTest {

  private SiteMembersServlet servlet;
  private Session adminSession;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    adminSession = loginAdministrative();
  }

  public void disabledTestGetMembers() throws Exception {
    long time = System.currentTimeMillis();
    ACMEGroupStructure acme = createAcmeStructure("" + time);
    Node siteNode = createGoodSite(adminSession);

    // The managers will be maintainers of the site node.
    // The others will be viewers.
    List<Authorizable> siteManagers = new ArrayList<Authorizable>();
    siteManagers.add(acme.acmeManagers);
    setManagers(siteNode, siteManagers);
    addAuthorizable(siteNode, acme.acmeLabs, false);

    // Retrieve the site node trough a Developer.
    Session session = login(acme.userDeveloper);
    siteNode = session.getNode(siteNode.getPath());

    Resource siteResource = mock(Resource.class);
    when(siteResource.adaptTo(Node.class)).thenReturn(siteNode);
    when(request.getResource()).thenReturn(siteResource);

    // Mock the selectors
    RequestPathInfo pathInfo = mock(RequestPathInfo.class);
    when(pathInfo.getSelectors()).thenReturn(new String[] { "members", "-1" });
    when(request.getRequestPathInfo()).thenReturn(pathInfo);

    JSONArray arr = makeGetRequestReturningJSON();
    // Should contain 7 users
    assertEquals(7, arr.length());
  }

  public void disabledTestGetSortedMembers() throws Exception {
    long time = System.currentTimeMillis();
    ACMEGroupStructure acme = createAcmeStructure("" + time);
    Node siteNode = createGoodSite(adminSession);

    // The managers will be maintainers of the site node.
    // The others will be viewers.
    // We will add a user with the name 'Zorro', he should appear first in the list.
    User userZorro = createUser(adminSession, "zorro-" + time);
    List<Authorizable> siteManagers = new ArrayList<Authorizable>();
    siteManagers.add(acme.acmeManagers);
    setManagers(siteNode, siteManagers);
    addAuthorizable(siteNode, acme.acmeLabs, false);
    addAuthorizable(siteNode, userZorro, false);

    // Retrieve the site node trough a Developer.
    Session session = login(acme.userDeveloper);
    siteNode = session.getNode(siteNode.getPath());

    Resource siteResource = mock(Resource.class);
    when(siteResource.adaptTo(Node.class)).thenReturn(siteNode);
    when(request.getResource()).thenReturn(siteResource);

    // Mock the selector
    RequestPathInfo pathInfo = mock(RequestPathInfo.class);
    when(pathInfo.getSelectors()).thenReturn(new String[] { "members", "-1" });
    when(request.getRequestPathInfo()).thenReturn(pathInfo);

    // Mock the request parameters
    RequestParameter sortParam = mock(RequestParameter.class);
    when(sortParam.getString()).thenReturn("firstName,desc");
    when(request.getRequestParameters(PARAM_SORT)).thenReturn(
        new RequestParameter[] { sortParam });

    JSONArray arr = makeGetRequestReturningJSON();
    
    SiteServiceImpl siteService = new SiteServiceImpl();
    int members = siteService.getMemberCount(siteNode);
    
    // Should contain 8 users
    assertEquals(8, arr.length());
    assertEquals(8, members);
    JSONObject jsonZorro = arr.getJSONObject(0);
    assertEquals(userZorro.getID(), jsonZorro.get("name"));
    
    boolean isMember = siteService.isMember(siteNode, userZorro);
    assertTrue(isMember);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.site.servlet.AbstractSiteServletTest#makeRequest()
   */
  @Override
  public void makeRequest() throws Exception {
    servlet = new SiteMembersServlet();
    servlet.bindSiteService(siteService);
    servlet.doGet(request, response);
    servlet.unbindSiteService(siteService);
  }

}
