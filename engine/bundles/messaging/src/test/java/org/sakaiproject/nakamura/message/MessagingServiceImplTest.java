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
package org.sakaiproject.nakamura.message;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.locking.LockManager;
import org.sakaiproject.nakamura.api.locking.LockTimeoutException;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessagingException;
import org.sakaiproject.nakamura.api.site.SiteException;
import org.sakaiproject.nakamura.api.site.SiteService;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

/**
 *
 */
public class MessagingServiceImplTest {

  private MessagingServiceImpl messagingServiceImpl;
  private SiteService siteService;
  private JackrabbitSession session;

  private String siteName = "physics-101";
  private String sitePath = "/sites/" + siteName;
  private String siteNameException = "fubar";
  private String userName = "admin";
  private String groupName = "g-physics-101-viewers";

  @Before
  public void setUp() throws Exception {
    Node siteNode = createMock(Node.class);
    expect(siteNode.getPath()).andReturn(sitePath);
    replay(siteNode);

    session = createMock(JackrabbitSession.class);
    Authorizable user = createMock(Authorizable.class);
    expect(user.getID()).andReturn(userName).anyTimes();
    expect(user.isGroup()).andReturn(false).anyTimes();
    
    ItemBasedPrincipal principal = createMock(ItemBasedPrincipal.class);
    expect(user.getPrincipal()).andReturn(principal).anyTimes();
    expect(principal.getPath()).andReturn("/rep:system/rep:authorizables/rep:users/a/ad/admin").anyTimes();
    expect(user.hasProperty("path")).andReturn(true).anyTimes();
    Value value = createMock(Value.class);
    expect(user.getProperty("path")).andReturn(new Value[]{value}).anyTimes();
    expect(value.getString()).andReturn("/a/ad/admin").anyTimes();

    
    
    Authorizable group = createMock(Authorizable.class);
    expect(group.getID()).andReturn(groupName).anyTimes();
    expect(group.isGroup()).andReturn(true).anyTimes();
    
    ItemBasedPrincipal gprincipal = createMock(ItemBasedPrincipal.class);
    expect(group.getPrincipal()).andReturn(gprincipal).anyTimes();
    expect(gprincipal.getPath()).andReturn("/rep:system/rep:authorizables/rep:groups/g/g_/g_p/g_physics_101_viewers").anyTimes();
    expect(group.hasProperty("path")).andReturn(true).anyTimes();
    Value gvalue = createMock(Value.class);
    expect(group.getProperty("path")).andReturn(new Value[]{gvalue}).anyTimes();
    expect(gvalue.getString()).andReturn("/g/g_/g_p/g_physics_101_viewers").anyTimes();


    UserManager um = createMock(UserManager.class);
    expect(um.getAuthorizable(userName)).andReturn(user).anyTimes();
    expect(um.getAuthorizable(groupName)).andReturn(group).anyTimes();

    replay(um, user, group, principal, value, gprincipal, gvalue);
    
    expect(session.getUserManager()).andReturn(um).anyTimes();

    siteService = createMock(SiteService.class);
    expect(siteService.findSiteByName(session, siteName)).andReturn(siteNode).anyTimes();
    expect(siteService.findSiteByName(session, siteNameException)).andThrow(
        new SiteException(400, "fubar")).anyTimes();
    replay(siteService);

    messagingServiceImpl = new MessagingServiceImpl();
    messagingServiceImpl.siteService = siteService;
  }

  @After
  public void tearDown() {
    messagingServiceImpl.siteService = null;
    verify(siteService);
  }

  @Test
  public void testFullPathToStoreSite() {
    replay(session);
    
    // Sites
    String path = messagingServiceImpl.getFullPathToStore("s-physics-101", session);
    assertEquals(sitePath + "/store", path);

    // Groups
    path = messagingServiceImpl.getFullPathToStore(groupName, session);
    assertEquals("/_group/g/g_/g_p/g_physics_101_viewers/message", path);

    // Users
    path = messagingServiceImpl.getFullPathToStore(userName, session);
    assertEquals("/_user/a/ad/admin/message", path);

    // Site Exception
    try {
      path = messagingServiceImpl.getFullPathToStore("s-" + siteNameException, session);
      fail("This should fail.");
    } catch (MessagingException e) {
      assertEquals(400, e.getCode());
      assertEquals("fubar", e.getMessage());
    }
  }

  @Test
  public void testIsMessageStore() throws ValueFormatException, RepositoryException {
    Node node = createMock(Node.class);
    Property prop = createMock(Property.class);
    expect(prop.getString()).andReturn(MessageConstants.SAKAI_MESSAGESTORE_RT);
    expect(node.getProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY))
        .andReturn(prop);
    expect(node.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY))
        .andReturn(true);

    replay(prop, node);

    boolean isNode = messagingServiceImpl.isMessageStore(node);
    assertEquals(true, isNode);

    Node randNode = createMock(Node.class);
    expect(randNode.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY))
        .andReturn(false);
    replay(randNode);
    isNode = messagingServiceImpl.isMessageStore(randNode);
    assertEquals(false, isNode);

    Node excepNode = createMock(Node.class);
    expect(excepNode.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY))
        .andThrow(new RepositoryException());
    isNode = messagingServiceImpl.isMessageStore(excepNode);
    assertEquals(false, isNode);
  }

  @Test
  public void testGetFullPathToMessage() {
    replay(session);
    
    String messageId = "cd5c208be6bd17f9e3d4c979ee9e319eca61ad6c";
    String path = messagingServiceImpl.getFullPathToMessage(userName, messageId, session);
    assertEquals(
        "/_user/a/ad/admin/message/cd/5c/20/8b/cd5c208be6bd17f9e3d4c979ee9e319eca61ad6c",
        path);
  }

  @Test
  public void testIsStore() throws RepositoryException {
    Node storeNode = new MockNode("/path/to/msg");
    storeNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY,
        MessageConstants.SAKAI_MESSAGESTORE_RT);
    boolean isStore = messagingServiceImpl.isMessageStore(storeNode);
    assertEquals(true, isStore);

    Node randomNode = new MockNode("/path/to/msg");
    randomNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY,
        MessageConstants.SAKAI_MESSAGE_RT);
    isStore = messagingServiceImpl.isMessageStore(randomNode);
    assertEquals(false, isStore);
  }

  @Test
  public void testCreate() throws LockTimeoutException, RepositoryException {
    Map<String, Object> mapProperties = new HashMap<String, Object>();
    mapProperties.put("num", 10L);
    mapProperties.put("s", "foobar");
    String messageId = "foo";

    expect(session.getUserID()).andReturn("admin");
    // expect(session.getUserID()).andReturn("admin");

    Node messageNode = new MockNode("/_user/a/ad/admin/message");

    expect(session.itemExists("/_user/a/ad/admin/message/fo/o_/__/__/foo"))
        .andReturn(true);
    expect(session.getItem("/_user/a/ad/admin/message/fo/o_/__/__/foo")).andReturn(
        messageNode);
    expect(session.hasPendingChanges()).andReturn(true);
    session.save();
    // expect(session.itemExists("/_user/message/d0/33/e2/2a/admin/0b/ee/c7/b5/foo")).andReturn(true);
    // expect(session.getItem("/_user/message/d0/33/e2/2a/admin/0b/ee/c7/b5/foo")).andReturn(messageNode);
    // expect(session.hasPendingChanges()).andReturn(true);

    LockManager lockManager = createMock(LockManager.class);
    expect(lockManager.waitForLock("/_user/a/ad/admin/message")).andReturn(null);
    lockManager.clearLocks();
    replay(lockManager, session);
    
    messagingServiceImpl.lockManager = lockManager;
    Node result = messagingServiceImpl.create(session, mapProperties, messageId);
    assertEquals("foo", result.getProperty(MessageConstants.PROP_SAKAI_ID).getString());
    assertEquals(10L, result.getProperty("num").getLong());
    assertEquals("foobar", result.getProperty("s").getString());
  }

  @Test
  public void testCreateFail() throws RepositoryException, LockTimeoutException {
    Map<String, Object> mapProperties = new HashMap<String, Object>();
    String messageId = "foo";

    expect(session.getUserID()).andReturn("admin");
    expect(session.itemExists("/_user/a/ad/admin/message/fo/o_/__/__/foo"))
        .andThrow(new RepositoryException());

    LockManager lockManager = createMock(LockManager.class);
    expect(lockManager.waitForLock("/_user/a/ad/admin/message")).andReturn(null);
    lockManager.clearLocks();
    replay(session, lockManager);
    
    messagingServiceImpl.lockManager = lockManager;
    try {
      messagingServiceImpl.create(session, mapProperties, messageId);
      fail("This should have thrown a Messageexception");
    } catch (MessagingException e) {
      assertEquals("Unable to save message.", e.getMessage());
    }
  }

  @Test
  public void testGetStore() throws ValueFormatException, PathNotFoundException,
      RepositoryException {

    Property storeP = mock(Property.class);
    when(storeP.getString()).thenReturn(MessageConstants.SAKAI_MESSAGESTORE_RT);
    Node storeNode = mock(Node.class);
    when(storeNode.getPath()).thenReturn("/path/to/store");
    when(storeNode.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)).thenReturn(true);
    when(storeNode.getProperty(SLING_RESOURCE_TYPE_PROPERTY)).thenReturn(storeP);

    Property msgP = mock(Property.class);
    when(msgP.getString()).thenReturn(MessageConstants.SAKAI_MESSAGE_RT);
    Node node = mock(Node.class);
    when(node.getPath()).thenReturn("/path/to/store/msg");
    when(node.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)).thenReturn(true);
    when(node.getProperty(SLING_RESOURCE_TYPE_PROPERTY)).thenReturn(msgP);
    when(node.getParent()).thenReturn(storeNode);

    String storePath = messagingServiceImpl.getMessageStorePathFromMessageNode(node);
    assertEquals("/path/to/store", storePath);

  }
}
