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

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.testing.jcr.MockProperty;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.junit.Test;
import org.sakaiproject.nakamura.api.site.SiteService;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractSiteNodeTest extends AbstractSiteServiceServletTest {

  protected String SITE_PATH = "/test/site/path";

  protected Resource resource;
  protected Node node;

  protected AccessControlManager accessControlManager;

  protected void setSiteGroups(String[] groups) throws RepositoryException {
    expect(node.hasProperty(eq(SiteService.AUTHORIZABLE))).andReturn(true).anyTimes();
    MockProperty authProperty = new MockProperty(SiteService.AUTHORIZABLE);
    authProperty.setValue(groups);
    expect(node.getProperty(eq(SiteService.AUTHORIZABLE))).andReturn(authProperty).anyTimes();
  }

  protected void goodResourceResolverSetup() {
    resource = createMock(Resource.class);
    expect(request.getResource()).andReturn(resource);
    expect(request.getContextPath()).andReturn(SITE_PATH).anyTimes();
  }

  protected void goodSiteNodeSetup() throws RepositoryException {
    goodResourceResolverSetup();
    node = createNiceMock(Node.class);
    accessControlManager = createNiceMock(AccessControlManager.class);
    expect(resource.adaptTo(eq(Node.class))).andReturn(node);
    expect(node.hasProperty(eq(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY))).andReturn(true)
        .anyTimes();
    MockProperty resourceType = new MockProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY);
    resourceType.setValue(SiteService.SITE_RESOURCE_TYPE);
    expect(node.getProperty(eq(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY))).andReturn(
        resourceType).anyTimes();
    expect(node.getPath()).andReturn(SITE_PATH).anyTimes();
    expect(node.getName()).andReturn("sitename").anyTimes();
    expect(node.getSession()).andReturn(session).anyTimes();
    expect(session.getAccessControlManager()).andReturn(accessControlManager).anyTimes();
  }
  
  @Test
  public void testNullSite() throws IOException, ServletException {
    goodResourceResolverSetup();
    expect(resource.adaptTo(eq(Node.class))).andReturn(null);
    response.sendError(eq(HttpServletResponse.SC_NO_CONTENT), isA(String.class));

    makeRequest();
  }

  @Test
  public void testNotASite() throws IOException, ServletException, RepositoryException {
    goodResourceResolverSetup();
    Node siteNode = createMock(Node.class);
    expect(resource.adaptTo(eq(Node.class))).andReturn(siteNode);
    expect(siteNode.hasProperty(eq(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY))).andReturn(
        false);
    response.sendError(eq(HttpServletResponse.SC_BAD_REQUEST), isA(String.class));
    expect(siteNode.getPath()).andReturn(SITE_PATH).anyTimes();

    makeRequest();
  }


}
