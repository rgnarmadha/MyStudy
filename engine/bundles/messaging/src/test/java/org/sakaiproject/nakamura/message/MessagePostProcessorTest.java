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
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.sakaiproject.nakamura.api.message.MessageConstants;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

/**
 *
 */
public class MessagePostProcessorTest {

  private MessagePostProcessor processor;
  private MockEventAdmin eventAdmin;
  private SlingHttpServletRequest request;
  private Session session;

  @Before
  public void setUp() {
    eventAdmin = new MockEventAdmin();

    processor = new MessagePostProcessor();
    processor.eventAdmin = eventAdmin;

    session = mock(Session.class);
    ResourceResolver resourceResolver = mock(ResourceResolver.class);
    request = mock(SlingHttpServletRequest.class);

    when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
    when(request.getResourceResolver()).thenReturn(resourceResolver);
  }

  @After
  public void tearDown() {
    processor.eventAdmin = null;
  }

  @Test
  public void testNoModification() throws Exception {
    Modification mod = new Modification(ModificationType.MOVE, "/from", "/to");
    List<Modification> changes = new ArrayList<Modification>();
    changes.add(mod);

    processor.process(request, changes);

    List<Event> events = eventAdmin.getEvents();
    assertEquals(0, events.size());
  }

  @Test
  public void testMessageModification() throws Exception {
    String path = "/path/to/message";
    String modificationPath = path + "/" + MessageConstants.PROP_SAKAI_MESSAGEBOX;
    Modification mod = new Modification(ModificationType.MODIFY, modificationPath,
        modificationPath);
    List<Modification> changes = new ArrayList<Modification>();
    changes.add(mod);

    when(request.getRemoteUser()).thenReturn("johndoe");

    when(session.itemExists(modificationPath)).thenReturn(true);

    Node node = new MockNode(path);
    node.setProperty(SLING_RESOURCE_TYPE_PROPERTY, MessageConstants.SAKAI_MESSAGE_RT);
    node.setProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX, MessageConstants.BOX_OUTBOX);
    node.setProperty(MessageConstants.PROP_SAKAI_SENDSTATE,
        MessageConstants.STATE_PENDING);

    when(session.getItem(path)).thenReturn(node);

    processor.process(request, changes);

    List<Event> events = eventAdmin.getEvents();
    assertEquals(1, events.size());
    Event event = events.get(0);
    assertEquals(MessageConstants.PENDINGMESSAGE_EVENT, event.getTopic());
    String location = (String) event.getProperty(MessageConstants.EVENT_LOCATION);
    assertEquals(path, location);

  }

}
