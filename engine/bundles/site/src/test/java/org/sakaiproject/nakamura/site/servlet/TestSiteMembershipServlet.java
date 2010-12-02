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

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.apache.jackrabbit.api.security.user.Group;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.commons.testing.jcr.MockValue;
import org.junit.Test;
import org.sakaiproject.nakamura.api.site.SiteService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.ServletException;

public class TestSiteMembershipServlet extends AbstractSiteServiceServletTest {

  private static final String TEST_USER = "testuser";
  private static final String TEST_SITE_GROUP = "some_site_group";
  private static final String TEST_SITE_PATH = "/some/test/site";
  private static final String TEST_SITE_UUID = "50362f56-094f-4e2d-be9f-6eebb2643d30";

  private SlingHttpServletRequest request;

  @Test
  public void testUserNoMemberships() throws ServletException, IOException,
      RepositoryException, JSONException {
    request = createMock(SlingHttpServletRequest.class);
    createDummyUser(TEST_USER);
    expect(request.getRemoteUser()).andReturn(TEST_USER);
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    expect(request.getResourceResolver()).andReturn(resourceResolver);
    expect(resourceResolver.adaptTo(Session.class)).andReturn(session);
    JSONArray sites = makeGetRequestReturningJSONresults();
    assertEquals("Expected no sites back", 0, sites.length());
  }

  @Test
  public void testUserWithMemberships() throws ServletException, IOException,
      RepositoryException, JSONException {
    Group siteGroup = createDummySiteGroup(TEST_SITE_GROUP, TEST_SITE_UUID);
    List<Group> siteGroups = new ArrayList<Group>();
    siteGroups.add(siteGroup);
    createDummyUserWithGroups(TEST_USER, siteGroups);
    request = createMock(SlingHttpServletRequest.class);
    expect(request.getRemoteUser()).andReturn(TEST_USER);
    ResourceResolver resolver = createMock(ResourceResolver.class);
    expect(request.getResourceResolver()).andReturn(resolver).anyTimes();
    expect(resolver.adaptTo(Session.class)).andReturn(session);
    MockNode siteNode = new MockNode(TEST_SITE_PATH);
    expect(session.getNodeByIdentifier(TEST_SITE_UUID)).andReturn(siteNode);
    JSONArray sites = makeGetRequestReturningJSONresults();
    assertEquals("Expected 1 site back", 1, sites.length());
    JSONObject site = (JSONObject) sites.get(0);
    assertEquals("Expected siteref to match path", TEST_SITE_PATH, site
        .get("siteref"));
  }

  private Group createDummySiteGroup(String groupName, String siteUuid)
      throws RepositoryException {
    Group result = createDummyGroup(groupName);
    expect(result.hasProperty(SiteService.SITES)).andReturn(true);
    expect(result.getProperty(SiteService.SITES)).andReturn(
        new Value[] { new MockValue(siteUuid) });
    return result;
  }

  protected void makeRequest() throws ServletException, IOException {
    preRequest();
    SiteMembershipServlet servlet = new SiteMembershipServlet();
    servlet.bindSiteService(siteService);
    servlet.doGet(request, response);
    servlet.unbindSiteService(siteService);
    postRequest();
  }

}
