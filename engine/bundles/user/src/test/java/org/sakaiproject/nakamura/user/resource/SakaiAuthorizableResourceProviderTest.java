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
package org.sakaiproject.nakamura.user.resource;

import junit.framework.Assert;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.security.UserPrincipal;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.easymock.EasyMock;
import org.junit.Test;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.security.Principal;
import java.util.Iterator;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;

/**
 */
public class SakaiAuthorizableResourceProviderTest extends AbstractEasyMockTest {

  @Test
  public void testSyntheticResources() {

    ResourceResolver resourceResolver = createNiceMock(ResourceResolver.class);

    replay();
    SakaiAuthorizableResourceProvider srp = new SakaiAuthorizableResourceProvider();
    Resource resource = srp.getResource(resourceResolver,
        SakaiAuthorizableResourceProvider.SYSTEM_USER_MANAGER_USER_PATH);
    Assert.assertEquals("sling/users", resource.getResourceType());
    resource = srp.getResource(resourceResolver,
        SakaiAuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PATH);
    Assert.assertEquals("sling/groups", resource.getResourceType());
    resource = srp.getResource(resourceResolver,
        SakaiAuthorizableResourceProvider.SYSTEM_USER_MANAGER_PATH);
    Assert.assertEquals("sling/userManager", resource.getResourceType());

    verify();
  }

  @Test
  public void testUserResolution() throws AccessDeniedException,
      UnsupportedRepositoryOperationException, RepositoryException {
    ResourceResolver resourceResolver = createNiceMock(ResourceResolver.class);
    JackrabbitSession session = createNiceMock(JackrabbitSession.class);
    UserManager userManager = createNiceMock(UserManager.class);
    User user = createMock(User.class);
    EasyMock.expect(resourceResolver.adaptTo(Session.class)).andReturn(session);
    EasyMock.expect(session.getUserManager()).andReturn(userManager);
    EasyMock.expect(userManager.getAuthorizable("ieb")).andReturn(user);
    EasyMock.expect(user.isGroup()).andReturn(false);

    replay();
    SakaiAuthorizableResourceProvider srp = new SakaiAuthorizableResourceProvider();
    Resource resource = srp.getResource(resourceResolver,
        SakaiAuthorizableResourceProvider.SYSTEM_USER_MANAGER_USER_PREFIX + "ieb");
    Assert.assertNotNull(resource);
    verify();
  }

  @Test
  public void testGroupResolution() throws AccessDeniedException,
      UnsupportedRepositoryOperationException, RepositoryException {
    ResourceResolver resourceResolver = createNiceMock(ResourceResolver.class);
    JackrabbitSession session = createNiceMock(JackrabbitSession.class);
    UserManager userManager = createNiceMock(UserManager.class);
    Group group = createMock(Group.class);
    EasyMock.expect(resourceResolver.adaptTo(Session.class)).andReturn(session);
    EasyMock.expect(session.getUserManager()).andReturn(userManager);
    EasyMock.expect(userManager.getAuthorizable("g-course101")).andReturn(group);
    EasyMock.expect(group.isGroup()).andReturn(true);

    replay();
    SakaiAuthorizableResourceProvider srp = new SakaiAuthorizableResourceProvider();
    Resource resource = srp.getResource(resourceResolver,
        SakaiAuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX
            + "g-course101");
    Assert.assertNotNull(resource);
    verify();
  }

  @Test
  public void testBadGroupResolution() throws AccessDeniedException,
      UnsupportedRepositoryOperationException, RepositoryException {
    ResourceResolver resourceResolver = createNiceMock(ResourceResolver.class);

    replay();
    SakaiAuthorizableResourceProvider srp = new SakaiAuthorizableResourceProvider();
    Resource resource = srp.getResource(resourceResolver,
        SakaiAuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX
            + "g-course101/ieb");
    Assert.assertNull(resource);
    verify();
  }

  @Test
  public void testBadResolution() throws AccessDeniedException,
      UnsupportedRepositoryOperationException, RepositoryException {
    ResourceResolver resourceResolver = createNiceMock(ResourceResolver.class);

    replay();
    SakaiAuthorizableResourceProvider srp = new SakaiAuthorizableResourceProvider();
    Resource resource = srp.getResource(resourceResolver, "g-course101/ieb");
    Assert.assertNull(resource);
    verify();
  }

  @Test
  public void testFailedGroupResolution() throws AccessDeniedException,
      UnsupportedRepositoryOperationException, RepositoryException {
    ResourceResolver resourceResolver = createNiceMock(ResourceResolver.class);
    JackrabbitSession session = createNiceMock(JackrabbitSession.class);
    UserManager userManager = createNiceMock(UserManager.class);
    EasyMock.expect(resourceResolver.adaptTo(Session.class)).andReturn(session);
    EasyMock.expect(session.getUserManager()).andReturn(userManager);
    EasyMock.expect(userManager.getAuthorizable("g-course101")).andThrow(
        new RepositoryException());

    replay();
    try {
      SakaiAuthorizableResourceProvider srp = new SakaiAuthorizableResourceProvider();
      srp.getResource(resourceResolver,
          SakaiAuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX
              + "g-course101");
      Assert.fail();
    } catch (SlingException e) {
    }
    verify();
  }
  
  @Test
  public void testListAuthorizableChildren() {
    ResourceResolver resourceResolver = createNiceMock(ResourceResolver.class);
    Resource parent = createNiceMock(Resource.class);
    EasyMock.expect(parent.getPath()).andReturn(SakaiAuthorizableResourceProvider.SYSTEM_USER_MANAGER_PATH);
    EasyMock.expect(parent.getResourceResolver()).andReturn(resourceResolver);
    
    replay();
    SakaiAuthorizableResourceProvider srp = new SakaiAuthorizableResourceProvider();
    Iterator<Resource> resources  = srp.listChildren(parent);
    int res = 0;
    int i = 0;
    for ( ; resources.hasNext(); ) {
      Resource resource = resources.next();
      if ( SakaiAuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PATH.equals(resource.getPath()) ) {
        res = res | 1;
      }
      if ( SakaiAuthorizableResourceProvider.SYSTEM_USER_MANAGER_USER_PATH.equals(resource.getPath()) ) {
        res = res | 2;
      }
      i++;
    }
    Assert.assertEquals(3, res);
    Assert.assertEquals(2, i);
    verify();
  }
  @Test
  public void testSearchUserChildren() throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
    ResourceResolver resourceResolver = createNiceMock(ResourceResolver.class);
    Resource parent = createNiceMock(Resource.class);
    JackrabbitSession session = createNiceMock(JackrabbitSession.class);
    PrincipalManager prinipalManager = createMock(PrincipalManager.class);
    PrincipalIterator principalIterator = createMock(PrincipalIterator.class);
    UserManager userManager = createMock(UserManager.class);
    User user1 = createMock(User.class);
    User user2 = createMock(User.class);
    
    EasyMock.expect(parent.getPath()).andReturn(SakaiAuthorizableResourceProvider.SYSTEM_USER_MANAGER_USER_PATH);
    EasyMock.expect(parent.getResourceResolver()).andReturn(resourceResolver).anyTimes();
    EasyMock.expect(resourceResolver.adaptTo(Session.class)).andReturn(session).anyTimes();
    EasyMock.expect(session.getPrincipalManager()).andReturn(prinipalManager).anyTimes();
    EasyMock.expect(session.getUserManager()).andReturn(userManager).anyTimes();
    EasyMock.expect(prinipalManager.getPrincipals(1)).andReturn(principalIterator);
    EasyMock.expect(principalIterator.hasNext()).andReturn(true);

    Principal p1 = new UserPrincipal("ieb");
    EasyMock.expect(principalIterator.nextPrincipal()).andReturn(p1);
    EasyMock.expect(userManager.getAuthorizable("ieb")).andReturn(user1);
    EasyMock.expect(user1.isGroup()).andReturn(false).times(2);
 
    EasyMock.expect(principalIterator.hasNext()).andReturn(true);
    Principal p2 = new UserPrincipal("ieb1");
    EasyMock.expect(principalIterator.nextPrincipal()).andReturn(p2);
    EasyMock.expect(userManager.getAuthorizable("ieb1")).andReturn(user2);
    EasyMock.expect(principalIterator.hasNext()).andReturn(false);
    EasyMock.expect(user2.isGroup()).andReturn(false).times(2);
 
   
    replay();
    SakaiAuthorizableResourceProvider srp = new SakaiAuthorizableResourceProvider();
    Iterator<Resource> resources  = srp.listChildren(parent);
    
    
    int i = 0;
    for ( ; resources.hasNext(); ) {
      resources.next();
      i++;
    }
    Assert.assertEquals(2, i);
    verify();
  }

  
  @Test
  public void testSearchGroupChildren() throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
    ResourceResolver resourceResolver = createNiceMock(ResourceResolver.class);
    Resource parent = createNiceMock(Resource.class);
    JackrabbitSession session = createNiceMock(JackrabbitSession.class);
    PrincipalManager prinipalManager = createMock(PrincipalManager.class);
    PrincipalIterator principalIterator = createMock(PrincipalIterator.class);
    UserManager userManager = createMock(UserManager.class);
    Group g1 = createMock(Group.class);
    Group g2 = createMock(Group.class);
    
    EasyMock.expect(parent.getPath()).andReturn(SakaiAuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PATH);
    EasyMock.expect(parent.getResourceResolver()).andReturn(resourceResolver).anyTimes();
    EasyMock.expect(resourceResolver.adaptTo(Session.class)).andReturn(session).anyTimes();
    EasyMock.expect(session.getPrincipalManager()).andReturn(prinipalManager).anyTimes();
    EasyMock.expect(session.getUserManager()).andReturn(userManager).anyTimes();
    EasyMock.expect(prinipalManager.getPrincipals(2)).andReturn(principalIterator);
    EasyMock.expect(principalIterator.hasNext()).andReturn(true);

    Principal p1 = new UserPrincipal("g-test1");
    EasyMock.expect(principalIterator.nextPrincipal()).andReturn(p1);
    EasyMock.expect(userManager.getAuthorizable("g-test1")).andReturn(g1);
    EasyMock.expect(g1.isGroup()).andReturn(true).times(2);
 
    EasyMock.expect(principalIterator.hasNext()).andReturn(true);
    Principal p2 = new UserPrincipal("g-test2");
    EasyMock.expect(principalIterator.nextPrincipal()).andReturn(p2);
    EasyMock.expect(userManager.getAuthorizable("g-test2")).andReturn(g2);
    EasyMock.expect(principalIterator.hasNext()).andReturn(false);
    EasyMock.expect(g2.isGroup()).andReturn(false).times(2);
 
   
    replay();
    SakaiAuthorizableResourceProvider srp = new SakaiAuthorizableResourceProvider();
    Iterator<Resource> resources  = srp.listChildren(parent);
    
    
    int i = 0;
    for ( ; resources.hasNext(); ) {
      resources.next();
      i++;
    }
    Assert.assertEquals(2, i);
    verify();
  }
}
