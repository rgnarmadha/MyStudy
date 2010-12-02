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

import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.junit.Test;
import org.sakaiproject.nakamura.api.site.SiteService;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractSitePostTest extends AbstractSiteNodeTest {

  protected String TEST_USERNAME = "MyUser";
  protected String TEST_GROUP = "MyGroup";

  protected User user;

  protected void goodRequestAndSessionSetup() throws RepositoryException
  {
    goodRequestSetup();
    user = createDummyUser(TEST_USERNAME);
    expect(session.getUserID()).andReturn(TEST_USERNAME).anyTimes();
  }
    
  protected void goodRequestSetupWithSiteGroups(String[] groups) throws RepositoryException {
    goodRequestAndSessionSetup();
    setSiteGroups(groups);
  }
  
  protected void goodRequestSetupNoGroups() throws RepositoryException {
    goodRequestAndSessionSetup();
    expect(node.hasProperty(eq(SiteService.AUTHORIZABLE))).andReturn(false).anyTimes();
  }

  protected void goodRequestSetup() throws RepositoryException {
    goodSiteNodeSetup();
    RequestParameter group = new DummyRequestParameter(TEST_GROUP);
    expect(request.getRequestParameter(eq(SiteService.PARAM_GROUP))).andReturn(group);
  }

  @Test
  public void testErrorReadingSite() throws IOException, ServletException, RepositoryException {
    goodResourceResolverSetup();
    Node siteNode = createMock(Node.class);
    expect(resource.adaptTo(eq(Node.class))).andReturn(siteNode);
    expect(siteNode.hasProperty(eq(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY))).andThrow(
        new RepositoryException("Expectional"));
    response.sendError(eq(HttpServletResponse.SC_BAD_REQUEST), isA(String.class));

    makeRequest();
  }

  @Test
  public void testNoGroupSpecified() throws RepositoryException, IOException, ServletException {
    goodSiteNodeSetup();
    expect(request.getRequestParameter(eq(SiteService.PARAM_GROUP))).andReturn(null);
    response.sendError(eq(HttpServletResponse.SC_BAD_REQUEST), isA(String.class));

    makeRequest();
  }

}
