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
package org.sakaiproject.nakamura.discussion.searchresults;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.junit.Test;
import org.sakaiproject.nakamura.api.discussion.DiscussionConstants;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.presence.PresenceService;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.search.SearchServiceFactory;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;
import org.sakaiproject.nakamura.testutils.easymock.MockRowIterator;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.RowIterator;
import javax.jcr.security.AccessControlManager;
import javax.jcr.version.VersionException;

/**
 *
 */
public class DiscussionThreadedProcessorTest extends AbstractEasyMockTest {

  private DiscussionThreadedSearchBatchResultProcessor processor;
  private PresenceService presenceService;

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest#setUp()
   */
  @Override
  public void setUp() throws Exception {
    super.setUp();

    processor = new DiscussionThreadedSearchBatchResultProcessor();
    processor.searchServiceFactory = createNiceMock(SearchServiceFactory.class);
    presenceService = createNiceMock(PresenceService.class);
    processor.presenceService = presenceService;
  }

  @Test
  public void testProces() throws ValueFormatException, VersionException, LockException,
      ConstraintViolationException, RepositoryException, JSONException, IOException {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);

    JackrabbitSession session = createMock(JackrabbitSession.class);

    ProfileService profileService = createMock(ProfileService.class);
    processor.profileService = profileService;

    AccessControlManager accessControlManager = createNiceMock(AccessControlManager.class);
    expect(session.getAccessControlManager()).andReturn(accessControlManager).anyTimes();
    Authorizable adminUser = createAuthorizable("admin", false, true);
    Authorizable anonUser = createAuthorizable("anonymous", false, true);
    expect(profileService.getCompactProfileMap(adminUser, session)).andReturn(
        ValueMap.EMPTY).anyTimes();
    expect(profileService.getCompactProfileMap(anonUser, session)).andReturn(
        ValueMap.EMPTY).anyTimes();
    UserManager um = createUserManager(null, true, adminUser, anonUser);
    expect(session.getUserManager()).andReturn(um).anyTimes();
    ResourceResolver resolver = createMock(ResourceResolver.class);
    expect(resolver.adaptTo(Session.class)).andReturn(session);
    expect(request.getResourceResolver()).andReturn(resolver);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Writer w = new PrintWriter(baos);
    ExtendedJSONWriter writer = new ExtendedJSONWriter(w);

    // 4 nodes
    // a
    // - b
    // - d
    // - c

    MockNode profileNode = new MockNode("/_user/a/ad/admin/public/authprofile");
    MockNode anonProfileNode = new MockNode("/_user/a/an/anonymous/public/authprofile");

    MockNode nodeA = new MockNode("/msg/a");
    nodeA.setSession(session);
    nodeA.setProperty(MessageConstants.PROP_SAKAI_ID, "a");
    nodeA.setProperty(MessageConstants.PROP_SAKAI_FROM, "admin");

    MockNode nodeB = new MockNode("/msg/b");
    nodeB.setSession(session);
    nodeB.setProperty(MessageConstants.PROP_SAKAI_ID, "b");
    nodeB.setProperty(DiscussionConstants.PROP_REPLY_ON, "a");
    nodeB.setProperty(MessageConstants.PROP_SAKAI_FROM, "anonymous");
    nodeB.setProperty(DiscussionConstants.PROP_EDITEDBY, "admin");

    MockNode nodeC = new MockNode("/msg/c");
    nodeC.setSession(session);
    nodeC.setProperty(MessageConstants.PROP_SAKAI_ID, "c");
    nodeC.setProperty(DiscussionConstants.PROP_REPLY_ON, "a");
    nodeC.setProperty(MessageConstants.PROP_SAKAI_FROM, "admin");

    MockNode nodeD = new MockNode("/msg/d");
    nodeD.setSession(session);
    nodeD.setProperty(MessageConstants.PROP_SAKAI_ID, "d");
    nodeD.setProperty(DiscussionConstants.PROP_REPLY_ON, "b");
    nodeD.setProperty(MessageConstants.PROP_SAKAI_FROM, "admin");

    List<Node> nodes = new ArrayList<Node>();
    nodes.add(nodeA);
    nodes.add(nodeB);
    nodes.add(nodeC);
    nodes.add(nodeD);

    expect(session.getItem("/msg/a")).andReturn(nodeA);
    expect(session.getItem("/msg/b")).andReturn(nodeB);
    expect(session.getItem("/msg/c")).andReturn(nodeC);
    expect(session.getItem("/msg/d")).andReturn(nodeD);
    expect(session.getItem("/_user/a/ad/admin/public/authprofile"))
        .andReturn(profileNode).anyTimes();
    expect(session.getItem("/_user/a/an/anonymous/public/authprofile")).andReturn(
        anonProfileNode).anyTimes();

    RowIterator iterator = new MockRowIterator(nodes);

    replay();
    processor.writeNodes(request, writer, null, iterator);
    w.flush();

    String s = baos.toString("UTF-8");

    JSONObject json = new JSONObject(s);
    assertEquals(json.getJSONArray("replies").length(), 2);
    assertEquals(json.getJSONArray("replies").getJSONObject(0).getJSONArray("replies")
        .length(), 1);
    assertEquals("a", json.getJSONObject("post").get("sakai:id"));
    assertEquals("b", json.getJSONArray("replies").getJSONObject(0).getJSONObject("post")
        .get("sakai:id"));
  }
}
