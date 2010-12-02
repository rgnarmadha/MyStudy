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

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import org.apache.sling.commons.testing.jcr.MockNode;
import org.junit.Test;
import org.sakaiproject.nakamura.api.discussion.DiscussionConstants;
import org.sakaiproject.nakamura.api.discussion.DiscussionManager;
import org.sakaiproject.nakamura.api.message.MessageConstants;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

/**
 *
 */
public class DiscussionRouterTest {

  @Test
  public void testDelivery() throws ValueFormatException, VersionException,
      LockException, ConstraintViolationException, RepositoryException {

    MockNode settingsNode = new MockNode("/sites/bla/_widgets/12345/settings");
    settingsNode.setProperty(DiscussionConstants.PROP_NOTIFICATION, true);
    settingsNode.setProperty(DiscussionConstants.PROP_NOTIFY_ADDRESS, "admin");

    Session session = createMock(Session.class);

    // Mock the discussion manager
    DiscussionManager discussionManager = createMock(DiscussionManager.class);
    expect(discussionManager.findSettings("12345", session, "discussion")).andReturn(
        settingsNode);

    MockMessageRoutes routing = new MockMessageRoutes();

    MockNode n = new MockNode("/sites/bla/store/foomessage");
    n.setProperty(SLING_RESOURCE_TYPE_PROPERTY, MessageConstants.SAKAI_MESSAGE_RT);
    n.setProperty(MessageConstants.PROP_SAKAI_TO, "discussion:s-foo");
    n.setProperty(DiscussionConstants.PROP_MARKER, "12345");
    n.setSession(session);

    replay(session, discussionManager);

    DiscussionRouter router = new DiscussionRouter();
    router.bindDiscussionManager(discussionManager);
    router.route(n, routing);

    assertEquals(1, routing.size());
    assertEquals("internal:admin", routing.get(0).getTransport() + ":"
        + routing.get(0).getRcpt());
    
    router.unbindDiscussionManager(discussionManager);
  }

}
