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
package org.sakaiproject.nakamura.discussion;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.sakaiproject.nakamura.api.discussion.DiscussionConstants.PROP_MARKER;
import static org.sakaiproject.nakamura.api.discussion.DiscussionConstants.PROP_REPLY_ON;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.junit.After;
import org.junit.Test;
import org.sakaiproject.nakamura.api.discussion.DiscussionManager;
import org.sakaiproject.nakamura.api.message.MessagingException;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import javax.jcr.Node;
import javax.jcr.Session;

/**
 *
 */
public class DiscussionCreateMessagePreProcessorTest extends AbstractEasyMockTest {

  private DiscussionCreateMessagePreProcessor processor;
  private DiscussionManager discussionManager;
  private SlingHttpServletRequest request;
  private Session session;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest#setUp()
   */
  @Override
  public void setUp() throws Exception {
    super.setUp();

    discussionManager = createMock(DiscussionManager.class);
    processor = new DiscussionCreateMessagePreProcessor();
    request = createMock(SlingHttpServletRequest.class);
    session = createMock(Session.class);

    processor.bindDiscussionManager(discussionManager);
  }

  @After
  public void tearDown() {
    processor.unbindDiscussionManager(discussionManager);
  }

  @Test
  public void testMarker() {
    setupBaseRequest();

    addStringRequestParameter(request, PROP_MARKER, "123");
    expect(request.getRequestParameter(PROP_REPLY_ON)).andReturn(null);

    replay();
    processor.checkRequest(request);
  }

  @Test
  public void testNoMarker() {
    setupBaseRequest();

    expect(request.getRequestParameter(PROP_MARKER)).andReturn(null);

    replay();
    try {
      processor.checkRequest(request);
    } catch (MessagingException e) {
      assertEquals(400, e.getCode());
    }
  }

  @Test
  public void testMarkerAndValidReplyOn() {
    setupBaseRequest();
    String marker = "id123";
    String messageId = "messageId132";

    addStringRequestParameter(request, PROP_MARKER, marker);
    addStringRequestParameter(request, PROP_REPLY_ON, messageId);

    Node messageNode = new MockNode("/_user/message/bla");

    expect(discussionManager.findMessage(messageId, marker, session, "/_user/message"))
        .andReturn(messageNode);

    replay();
    processor.checkRequest(request);
  }

  @Test
  public void testMarkerAndNonValidReplyOn() {
    setupBaseRequest();
    String marker = "id123";
    String messageId = "messageId132";

    addStringRequestParameter(request, PROP_MARKER, marker);
    addStringRequestParameter(request, PROP_REPLY_ON, messageId);
    expect(discussionManager.findMessage(messageId, marker, session, "/_user/message"))
        .andReturn(null);

    replay();
    try {
      processor.checkRequest(request);
    } catch (MessagingException e) {
      assertEquals(400, e.getCode());
    }
  }

  /**
   * 
   */
  private void setupBaseRequest() {
    RequestPathInfo pathInfo = createMock(RequestPathInfo.class);
    expect(pathInfo.getResourcePath()).andReturn("/_user/message");

    ResourceResolver resolver = createMock(ResourceResolver.class);
    expect(resolver.adaptTo(Session.class)).andReturn(session);

    expect(request.getResourceResolver()).andReturn(resolver);
    expect(request.getRequestPathInfo()).andReturn(pathInfo);
  }

}
