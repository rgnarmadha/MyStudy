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
package org.sakaiproject.nakamura.chat;

import static org.junit.Assert.assertEquals;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.memory.Cache;
import org.sakaiproject.nakamura.api.memory.CacheManagerService;
import org.sakaiproject.nakamura.api.memory.CacheScope;
import org.sakaiproject.nakamura.api.message.AbstractMessageRoute;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessageRoute;
import org.sakaiproject.nakamura.api.message.MessageRoutes;
import org.sakaiproject.nakamura.api.message.MessagingService;
import org.sakaiproject.nakamura.memory.MapCacheImpl;

import java.util.ArrayList;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;

/**
 *
 */
public class ChatMessageHandlerTest {

  private SlingRepository slingRepository;
  private MessagingService messagingService;
  private Session adminSession;
  private ChatMessageHandler handler;
  private CacheManagerService cacheManagerService;
  private ChatManagerServiceImpl chatManagerService;
  private Cache<Object> chatCache;

  private String rcpt = "johndoe";
  private String messageId = "12345";
  private String pathToMessage = "/_user/message/johndoe/12345";
  private String fromPath = "/_user/message/jack/12345";

  @Before
  public void setUp() throws RepositoryException {
    slingRepository = createMock(SlingRepository.class);
    adminSession = createMock(Session.class);
    expect(slingRepository.loginAdministrative(null)).andReturn(adminSession);
    replay(slingRepository);

    messagingService = createMock(MessagingService.class);
    expect(messagingService.getFullPathToMessage(rcpt, messageId, adminSession))
        .andReturn(pathToMessage);
    replay(messagingService);

    // chat manager service mocks
    chatCache = new MapCacheImpl<Object>();
    cacheManagerService = createMock(CacheManagerService.class);
    expect(cacheManagerService.getCache("chat", CacheScope.CLUSTERREPLICATED))
        .andReturn(chatCache).anyTimes();
    replay(cacheManagerService);
    chatManagerService = new ChatManagerServiceImpl();
    chatManagerService.bindCacheManagerService(cacheManagerService);

    handler = new ChatMessageHandler();
    handler.chatManagerService = chatManagerService;
    handler.messagingService = messagingService;
    handler.slingRepository = slingRepository;
  }

  @After
  public void tearDown() {
    handler.chatManagerService = null;
    handler.messagingService = null;
    handler.slingRepository = null;
    verify(cacheManagerService, messagingService, slingRepository);
  }

  @Test
  public void testHandle() throws ValueFormatException, RepositoryException {
    Node originalMessage = createMock(Node.class);
    expect(originalMessage.getPath()).andReturn(fromPath).anyTimes();
    Property fromProp = createMock(Property.class);
    expect(fromProp.getString()).andReturn("jack");
    Property createdProp = createMock(Property.class);
    Long time = System.currentTimeMillis() - 10000;
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(time);
    expect(createdProp.getDate()).andReturn(cal);

    Property propId = createMock(Property.class);
    expect(propId.getString()).andReturn(messageId);
    expect(originalMessage.getProperty(MessageConstants.PROP_SAKAI_ID))
        .andReturn(propId);
    expect(originalMessage.getProperty(MessageConstants.PROP_SAKAI_CREATED))
        .andReturn(createdProp);
    expect(originalMessage.getProperty(MessageConstants.PROP_SAKAI_FROM))
        .andReturn(fromProp);

    replay(propId, fromProp, createdProp, originalMessage);

    expect(adminSession.itemExists("/_user/message/johndoe/12345")).andReturn(
        false);
    expect(adminSession.itemExists("/_user/message/johndoe")).andReturn(true)
        .anyTimes();

    Node messageNode = createMock(Node.class);

    Node msgStore = createMock(Node.class);
    expect(msgStore.hasNode("12345")).andReturn(false).anyTimes();
    expect(msgStore.addNode("12345")).andReturn(messageNode).anyTimes();
    expect(adminSession.getItem("/_user/message/johndoe")).andReturn(msgStore)
        .anyTimes();
    adminSession.save();

    Workspace workSpace = createMock(Workspace.class);
    workSpace.copy(fromPath, "/_user/message/johndoe/12345");
    expect(adminSession.getWorkspace()).andReturn(workSpace);

    expect(messageNode.setProperty(MessageConstants.PROP_SAKAI_READ, false))
        .andReturn(null);
    expect(messageNode.setProperty(MessageConstants.PROP_SAKAI_TO, rcpt))
        .andReturn(null);
    expect(
        messageNode.setProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX,
            MessageConstants.BOX_INBOX)).andReturn(null);
    expect(
        messageNode.setProperty(MessageConstants.PROP_SAKAI_SENDSTATE,
            MessageConstants.STATE_NOTIFIED)).andReturn(null);
    expect(
        messageNode.setProperty(
            JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
            MessageConstants.SAKAI_MESSAGE_RT)).andReturn(null);
    adminSession.save();
    
    adminSession.logout();
    EasyMock.expectLastCall();
    
    replay(messageNode, msgStore, adminSession);

    MessageRoutes routes = new MessageRoutesTest();
    MessageRoute route = new AbstractMessageRoute("chat:" + rcpt) {
    };
    routes.add(route);

    handler.send(routes, null, originalMessage);

    assertEquals(time, chatManagerService.get("jack"));
    assertEquals(time, chatManagerService.get("johndoe"));
  }

  public class MessageRoutesTest extends ArrayList<MessageRoute> implements
      MessageRoutes {
    private static final long serialVersionUID = 981738821907909267L;
  }

}
