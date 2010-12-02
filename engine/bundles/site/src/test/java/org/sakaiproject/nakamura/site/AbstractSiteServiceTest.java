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
package org.sakaiproject.nakamura.site;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.jcr.api.SlingRepository;
import org.easymock.EasyMock;
import org.junit.Before;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Value;

public abstract class AbstractSiteServiceTest extends AbstractEasyMockTest {

  protected EventAdmin eventAdmin;
  protected UserManager userManager;

  protected SiteServiceImpl siteService;
  protected SlingRepository slingRepository;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    userManager = createMock(UserManager.class);
    eventAdmin = createMock(EventAdmin.class);
    slingRepository = createMock(SlingRepository.class);
  }

  protected Group createDummyGroup(String groupName) throws RepositoryException {
    Group group = createMock(Group.class);
    registerAuthorizable(group, groupName);
    expect(group.isMember(isA(Authorizable.class))).andReturn(false).anyTimes();
    return group;
  }

  protected Group createDummyGroupWithMember(String groupName, Authorizable member)
      throws RepositoryException {
    List<Authorizable> members = new ArrayList<Authorizable>();
    members.add(member);
    return createDummyGroupWithMembers(groupName, members);
  }

  protected Group createDummyGroupWithMembers(String groupName, List<Authorizable> members)
      throws RepositoryException {
    Group group = createMock(Group.class);
    registerAuthorizable(group, groupName);
    for (Authorizable member : members) {
      expect(group.isMember(eq(member))).andReturn(true).anyTimes();
    }
    expect(group.isGroup()).andReturn(true).anyTimes();
    expect(group.getDeclaredMembers()).andReturn(members.iterator()).anyTimes();
    return group;
  }

  protected User createDummyUser(String userName) throws RepositoryException {
    List<Group> groups = new ArrayList<Group>();
    return createDummyUserWithGroups(userName, groups);
  }

  protected User createDummyUserWithGroups(String userName, List<Group> groups) throws RepositoryException {
    User user = createMock(User.class);
    registerAuthorizable(user, userName);
    expect(user.isGroup()).andReturn(false).anyTimes();
    expect(user.memberOf()).andReturn(groups.iterator()).anyTimes();
    return user;
  }

  private void registerAuthorizable(Authorizable authorizable, String name)
      throws RepositoryException {
    ItemBasedPrincipal p = EasyMock.createMock(ItemBasedPrincipal.class);
    String hashedPath = "/"+name.substring(0,1)+"/"+name.substring(0,2)+"/"+name;
    expect(p.getPath()).andReturn("rep:" + hashedPath).anyTimes();
    expect(p.getName()).andReturn(name).anyTimes();
    expect(authorizable.getPrincipal()).andReturn(p).anyTimes();
    expect(authorizable.hasProperty("path")).andReturn(true).anyTimes();
    Value v = EasyMock.createNiceMock(Value.class);
    expect(v.getString()).andReturn(hashedPath).anyTimes();
    expect(authorizable.getProperty("path")).andReturn(new Value[] { v }).anyTimes();
    expect(authorizable.getID()).andReturn(name).anyTimes();
    EasyMock.replay(p);
    EasyMock.replay(v);
    expect(userManager.getAuthorizable(eq(name))).andReturn(authorizable).anyTimes();
  }

  protected void preRequest() {
    replay();
    siteService = new SiteServiceImpl();
    siteService.bindEventAdmin(eventAdmin);
    siteService.slingRepository = slingRepository;
  }

  protected void postRequest() {
    siteService.unbindEventAdmin(eventAdmin);
    verify();
  }

}
