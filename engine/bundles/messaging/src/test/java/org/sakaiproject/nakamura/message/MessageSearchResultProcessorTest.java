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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessagingService;
import org.sakaiproject.nakamura.message.internal.InternalMessageHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

/**
 *
 */
public class MessageSearchResultProcessorTest {

  private MessageSearchResultProcessor proc;
  private MessagingService messagingService;

  @Before
  public void setUp() {
    messagingService = mock(MessagingService.class);

    proc = new MessageSearchResultProcessor();
    proc.messagingService = messagingService;

    InternalMessageHandler handler = new InternalMessageHandler();
    MessageProfileWriterTracker tracker = mock(MessageProfileWriterTracker.class);
    when(tracker.getMessageProfileWriterByType("internal")).thenReturn(handler);
    proc.tracker = tracker;
  }

  @After
  public void tearDown() {
    proc.messagingService = null;
  }

  @Test
  public void testProc() throws JSONException, RepositoryException, IOException {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Writer w = new PrintWriter(baos);
    JSONWriter write = new JSONWriter(w);

    Session session = mock(Session.class);

    // We handle a previous msg.
    String previousId = "prevId";
    String userID = "john";
    String pathToPrevMsg = "/path/to/store/prevId";
    when(session.getUserID()).thenReturn(userID);
    when(messagingService.getFullPathToMessage(userID, previousId, session)).thenReturn(
        pathToPrevMsg);
    Node previousMsg = createDummyMessage(session, previousId);
    when(session.itemExists(previousMsg.getPath())).thenReturn(true);
    when(session.getItem(previousMsg.getPath())).thenReturn(previousMsg);

    Node resultNode = createDummyMessage(session, "msgid");
    resultNode.setProperty(MessageConstants.PROP_SAKAI_PREVIOUS_MESSAGE, previousId);
    proc.writeNode(request, write, resultNode);
    w.flush();

    String s = baos.toString("UTF-8");
    JSONObject o = new JSONObject(s);

    assertEquals(o.getString("id"), "msgid");
    assertEquals(o.getString(MessageConstants.PROP_SAKAI_MESSAGEBOX),
        MessageConstants.BOX_INBOX);
    assertEquals(2, o.getJSONArray("foo").length());

    assertEquals(previousId, o.getString(MessageConstants.PROP_SAKAI_PREVIOUS_MESSAGE));

    JSONObject prev = o.getJSONObject("previousMessage");
    assertEquals(prev.getString("id"), previousId);
  }

  private MockNode createDummyMessage(Session session, String msgID)
      throws ValueFormatException, VersionException, LockException,
      ConstraintViolationException, RepositoryException {
    String path = "/path/to/store/" + msgID;
    MockNode msgNode = new MockNode(path);
    msgNode.setSession(session);
    msgNode.setProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX,
        MessageConstants.BOX_INBOX);
    msgNode.setProperty("foo", new String[] { "a", "b" });
    return msgNode;
  }

}
