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
package org.sakaiproject.nakamura.message.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.locking.LockManager;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessageRoutes;
import org.sakaiproject.nakamura.api.message.MessagingService;
import org.sakaiproject.nakamura.message.listener.MessageRoutesImpl;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

/**
 *
 */
public class InternalMessageHandlerTest {

  private InternalMessageHandler handler;
  private MessagingService messagingService;
  private LockManager lockManager;
  private SlingRepository slingRepository;
  private JackrabbitSession session;
  private String groupName = "g_group1";
  
  @Before
  public void setUp() throws Exception {
    messagingService = mock(MessagingService.class);
    slingRepository = mock(SlingRepository.class);
    lockManager = mock(LockManager.class);
    handler = new InternalMessageHandler();
    handler.activateTesting();
    handler.messagingService = messagingService;
    handler.slingRepository = slingRepository;
    handler.lockManager = lockManager;
    session = mock(JackrabbitSession.class);
  }

  @Test
  public void testHandle() throws Exception {
         
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    when(request.getRemoteUser()).thenReturn("admin");
    Authorizable admin = createAuthorizable("admin", false);
    Group group = (Group) createAuthorizable(groupName, true);
    UserManager um = createUserManager(null, admin, group);
    ResourceResolver resolver = mock(ResourceResolver.class);
    when(session.getUserManager()).thenReturn(um);
    when(request.getResourceResolver()).thenReturn(resolver);
    when(resolver.adaptTo(Session.class)).thenReturn(session);
    
    
    String path = "/path/to/msg";
    String newPath = "/path/to/new/msg";
    String to = "internal:admin";
    
    testMessage(path, newPath, to);
    
    //Set up group members:
    
    List<Authorizable> members = new ArrayList<Authorizable>();
    members.add(admin);
    
    
    registerAuthorizable(group, um, groupName);
    for (Authorizable member : members) {
      when(group.isMember(member)).thenReturn(true);
    }
    when(group.isGroup()).thenReturn(true);
    when(group.getDeclaredMembers()).thenReturn(members.iterator());
        
    path = "/path/to/msg2";
    newPath = "/path/to/new/msg2";
    to = "internal:" + groupName;
    testMessage(path, newPath, to);

  }
  
  private void testMessage(String path, String newPath, String to) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException{
 // Original message created to send
    Node originalMessage = new MockNode(path);
    originalMessage.setProperty(MessageConstants.PROP_SAKAI_TO, to);
    originalMessage.setProperty(MessageConstants.PROP_SAKAI_ID, "foo");

    Node newNode = new MockNode(newPath);
    Node newNodeParent = new MockNode(newPath.substring(0, newPath.lastIndexOf("/")));
    Workspace space = mock(Workspace.class);
    when(session.getWorkspace()).thenReturn(space);

    when(session.itemExists(newNodeParent.getPath())).thenReturn(true);
    when(session.getItem(newNodeParent.getPath())).thenReturn(newNodeParent);
    when(session.itemExists(newPath)).thenReturn(true);
    when(session.getItem(newPath)).thenReturn(newNode);

    when(slingRepository.loginAdministrative(null)).thenReturn(session);

    when(messagingService.getFullPathToMessage("admin", "foo", session)).thenReturn(
        newPath);
    
    MessageRoutes routes = new MessageRoutesImpl(originalMessage);

    handler.send(routes, null, originalMessage);

    assertEquals(false, newNode.getProperty(MessageConstants.PROP_SAKAI_READ)
        .getBoolean());
    assertEquals(MessageConstants.BOX_INBOX, newNode.getProperty(
        MessageConstants.PROP_SAKAI_MESSAGEBOX).getString());
    assertEquals(MessageConstants.STATE_NOTIFIED, newNode.getProperty(
        MessageConstants.PROP_SAKAI_SENDSTATE).getString());
    newNode.remove();
  }
  
  private void registerAuthorizable(Authorizable authorizable, UserManager um, String name)
  throws RepositoryException {
    ItemBasedPrincipal p = mock(ItemBasedPrincipal.class);
    String hashedPath = "/"+name.substring(0,1)+"/"+name.substring(0,2)+"/"+name;
    when(p.getPath()).thenReturn("rep:" + hashedPath);
    when(p.getName()).thenReturn(name);
    when(authorizable.getPrincipal()).thenReturn(p);
    when(authorizable.hasProperty("path")).thenReturn(true);
    Value v = mock(Value.class);
    when(v.getString()).thenReturn(hashedPath);
    when(authorizable.getProperty("path")).thenReturn(new Value[] { v });
    when(authorizable.getID()).thenReturn(name);
    when(um.getAuthorizable(name)).thenReturn(authorizable);
  }
  
  protected Authorizable createAuthorizable(String id, boolean isGroup)
  throws RepositoryException {
    Authorizable au;
    if(isGroup)
        au = mock(Group.class);
    else
        au = mock(Authorizable.class);
    when(au.getID()).thenReturn(id);
    when(au.isGroup()).thenReturn(isGroup);
    ItemBasedPrincipal p = mock(ItemBasedPrincipal.class);
    String hashedPath = "/"+id.substring(0,1)+"/"+id.substring(0,2)+"/"+id;
    when(p.getPath()).thenReturn("rep:" + hashedPath);
    when(au.getPrincipal()).thenReturn(p);
    when(au.hasProperty("path")).thenReturn(true);
    Value v = mock(Value.class);
    when(v.getString()).thenReturn(hashedPath);
    when(au.getProperty("path")).thenReturn(new Value[] { v });
    return au;
  }
  
  protected UserManager createUserManager(UserManager um,
      Authorizable... authorizables) throws RepositoryException {
    if (um == null) {
      um = mock(UserManager.class);
    }
    for (Authorizable au : authorizables) {
      when(um.getAuthorizable(au.getID())).thenReturn(au);
    }
    return um;
  }
}
