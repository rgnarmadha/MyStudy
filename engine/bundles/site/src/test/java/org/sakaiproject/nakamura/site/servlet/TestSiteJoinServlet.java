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

import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.commons.testing.jcr.MockProperty;
import org.apache.sling.commons.testing.jcr.MockValue;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.api.site.SiteService.Joinable;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

public class TestSiteJoinServlet extends AbstractSitePostTest {

  private void makeSiteJoinable(Joinable joinable) throws RepositoryException {
    addStringPropertyToNode(node, SiteService.JOINABLE, joinable.toString());
  }

  private void makeGroupJoinable(Group group, Joinable joinable) throws RepositoryException {
    expect(group.hasProperty(eq(SiteService.JOINABLE))).andReturn(true).anyTimes();
    Value[] value = new Value[] { new MockValue(joinable.toString()) };
    expect(group.getProperty(eq(SiteService.JOINABLE))).andReturn(value).anyTimes();
  }

  @Override
  protected void makeRequest() throws ServletException, IOException {
    preRequest();
    SiteJoinServlet servlet = new SiteJoinServlet();
    servlet.bindSiteService(siteService);    
    servlet.doPost(request, response);
    servlet.unbindSiteService(siteService);
    postRequest();
  }

  @Test
  public void testGroupJoinAlreadyMember() throws RepositoryException, ServletException,
      IOException {
    goodRequestSetup();
    createDummyUser("TEST UID");
    expect(session.getUserID()).andReturn("TEST UID");
    expect(node.hasProperty(eq(SiteService.AUTHORIZABLE))).andReturn(true);
    MockProperty authProperty = new MockProperty(SiteService.AUTHORIZABLE);
    authProperty.setValue("TEST UID");
    expect(node.getProperty(eq(SiteService.AUTHORIZABLE))).andReturn(authProperty);
    response.sendError(eq(HttpServletResponse.SC_CONFLICT), isA(String.class));

    makeRequest();
  }

  @Test
  public void testJoinAlreadyMemberByUserManager() throws RepositoryException, ServletException,
      IOException {
    goodRequestSetupWithSiteGroups(new String[] { TEST_GROUP });
    createDummyGroupWithMember(TEST_GROUP, user);
    response.sendError(eq(HttpServletResponse.SC_CONFLICT), isA(String.class));
    makeRequest();
  }

  @Test
  public void testJoinNonGroup() throws RepositoryException, IOException, ServletException {
    goodRequestSetupNoGroups();
    makeSiteJoinable(Joinable.yes);
    User nonGroup = createMock(User.class);
    expect(userManager.getAuthorizable(TEST_GROUP)).andReturn(nonGroup);
    response.sendError(eq(HttpServletResponse.SC_BAD_REQUEST), isA(String.class));

    makeRequest();
  }

  @Test
  public void testJoinGroupSelectedTargetNotInSite() throws RepositoryException, IOException, ServletException {
    goodRequestSetupNoGroups();
    makeSiteJoinable(Joinable.yes);
    Group newGroup = createDummyGroup(TEST_GROUP);
    makeGroupJoinable(newGroup, Joinable.yes);
    response.sendError(eq(HttpServletResponse.SC_BAD_REQUEST), isA(String.class));

    makeRequest();
  }

  @Test
  public void testJoinUnjoinableSite() throws RepositoryException, IOException, ServletException {
    goodRequestSetupWithSiteGroups(new String[] { TEST_GROUP });
    Group newGroup = createDummyGroup(TEST_GROUP);
    makeSiteJoinable(Joinable.no);
    makeGroupJoinable(newGroup, Joinable.yes);
    response.sendError(eq(HttpServletResponse.SC_CONFLICT), isA(String.class));

    makeRequest();
  }

  @Test
  public void testJoinUnjoinableGroup() throws RepositoryException, IOException, ServletException {
    goodRequestSetupWithSiteGroups(new String[] { TEST_GROUP });
    Group newGroup = createDummyGroup(TEST_GROUP);
    makeSiteJoinable(Joinable.yes);
    makeGroupJoinable(newGroup, Joinable.no);
    response.sendError(eq(HttpServletResponse.SC_CONFLICT), isA(String.class));

    makeRequest();
  }

  @Test
  public void testJoin() throws RepositoryException, IOException, ServletException {
    goodRequestSetupWithSiteGroups(new String[] { TEST_GROUP });
    Group newGroup = createDummyGroup(TEST_GROUP);
    makeSiteJoinable(Joinable.yes);
    makeGroupJoinable(newGroup, Joinable.yes);
    expect(newGroup.addMember(eq(user))).andReturn(true);
    eventAdmin.postEvent(isA(Event.class));
    response.sendError(eq(HttpServletResponse.SC_OK));

    makeRequest();
  }
}
