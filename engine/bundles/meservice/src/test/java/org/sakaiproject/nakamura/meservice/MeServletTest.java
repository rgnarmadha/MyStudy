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
package org.sakaiproject.nakamura.meservice;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.sakaiproject.nakamura.api.connections.ConnectionConstants.SAKAI_CONNECTION_STATE;
import static org.sakaiproject.nakamura.api.connections.ConnectionState.ACCEPTED;
import static org.sakaiproject.nakamura.api.connections.ConnectionState.INVITED;
import static org.sakaiproject.nakamura.api.connections.ConnectionState.PENDING;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.message.MessagingException;
import org.sakaiproject.nakamura.api.message.MessagingService;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.profile.ProfileServiceImpl;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class MeServletTest extends AbstractEasyMockTest {

  private ByteArrayOutputStream baos;
  private PrintWriter w;
  private JackrabbitSession session;
  private SlingHttpServletRequest request;
  private SlingHttpServletResponse response;
  private MessagingService messagingService;
  private MeServlet servlet;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    baos = new ByteArrayOutputStream();
    w = new PrintWriter(baos);

    request = createMock(SlingHttpServletRequest.class);
    response = createMock(SlingHttpServletResponse.class);
    session = createMock(JackrabbitSession.class);
    ResourceResolver resolver = createMock(ResourceResolver.class);
    messagingService = createMock(MessagingService.class);

    expect(resolver.adaptTo(Session.class)).andReturn(session);
    expect(request.getResourceResolver()).andReturn(resolver).anyTimes();
    expect(response.getWriter()).andReturn(w);

    servlet = new MeServlet();
    servlet.messagingService = messagingService;

    // TODO With this, we are testing the internals of the ProfileServiceImpl
    // class as well as the internals of the MeServlet class. Mocking it would
    // reduce the cost of test maintenance.
    servlet.profileService = new ProfileServiceImpl();
  }

  @Test
  public void testGeneralInfoAdmin() throws JSONException, UnsupportedEncodingException,
      RepositoryException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter w = new PrintWriter(baos);
    ExtendedJSONWriter write = new ExtendedJSONWriter(w);

    Authorizable user = createAuthorizable("admin", false, true);

    Set<String> subjects = new HashSet<String>();
    subjects.add("administrators");
    Map<String, Object> properties = new HashMap<String, Object>();

    write.object();
    servlet.writeGeneralInfo(write, user, subjects, properties);
    write.endObject();

    w.flush();
    String s = baos.toString("UTF-8");
    JSONObject j = new JSONObject(s);

    assertEquals("admin", j.getString("userid"));
    assertEquals(true, j.getBoolean("superUser"));
    assertEquals("a/ad/admin/", j.getString("userStoragePrefix"));
    assertEquals(1, j.getJSONArray("subjects").length());
  }

  @Test
  public void testWriteLocale() throws JSONException, UnsupportedEncodingException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter w = new PrintWriter(baos);
    ExtendedJSONWriter write = new ExtendedJSONWriter(w);

    Map<String, Object> properties = new HashMap<String, Object>();
    properties.put("locale", "en_US");
    properties.put("timezone", "America/Los_Angeles");

    write.object();
    servlet.writeLocale(write, properties, request);
    write.endObject();
    w.flush();
    String s = baos.toString("UTF-8");
    JSONObject j = new JSONObject(s);
    JSONObject locale = j.getJSONObject("locale");
    assertEquals("US", locale.getString("country"));
    assertEquals("USA", locale.getString("ISO3Country"));
    assertEquals("en", locale.getString("language"));
    assertEquals("USA", locale.getString("ISO3Country"));
    assertEquals("USA", locale.getString("ISO3Country"));
    JSONObject timezone = locale.getJSONObject("timezone");
    assertEquals("America/Los_Angeles", timezone.getString("name"));
  }

  @Test
  public void testAnon() throws RepositoryException, JSONException, ServletException,
      IOException {

    Authorizable au = createAuthorizable(UserConstants.ANON_USERID, false, true);
    UserManager um = createUserManager(null, true, au);

    String profilePath = PersonalUtils.getProfilePath(au);
    Node profileNode = new MockNode(profilePath);

    Node rootNode = createMock(Node.class);
    expect(rootNode.hasNode(profilePath.substring(1))).andReturn(true).anyTimes();
    expect(session.getRootNode()).andReturn(rootNode).anyTimes();
    expect(session.getItem(profilePath)).andReturn(profileNode).anyTimes();
    expect(session.getNode(profilePath)).andReturn(profileNode).anyTimes();
    expect(session.getUserID()).andReturn(UserConstants.ANON_USERID).anyTimes();
    expect(session.getUserManager()).andReturn(um).anyTimes();

    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    replay();

    servlet.doGet(request, response);
    w.flush();
    String s = baos.toString("UTF-8");
    JSONObject json = new JSONObject(s);
    JSONObject user = json.getJSONObject("user");

    assertEquals(true, user.getBoolean("anon"));
    assertEquals(false, user.getBoolean("superUser"));
    assertEquals(0, user.getJSONArray("subjects").length());
    assertEquals(0, json.getJSONObject("messages").get("unread"));
  }

  @Test
  public void testExceptions() throws IOException, ServletException,
      PathNotFoundException, RepositoryException {

    Authorizable au = createAuthorizable(UserConstants.ANON_USERID, false, true);
    String profilePath = PersonalUtils.getProfilePath(au);
    expect(session.getUserID()).andReturn(UserConstants.ANON_USERID).anyTimes();
    expect(session.getItem(profilePath)).andThrow(new RepositoryException()).anyTimes();
    Node rootNode = createMock(Node.class);
    expect(rootNode.hasNode(profilePath.substring(1))).andReturn(true).anyTimes();
    expect(session.getRootNode()).andReturn(rootNode).anyTimes();
    expect(session.getNode(profilePath)).andThrow(new RepositoryException()).anyTimes();

    UserManager um = createUserManager(null, true, au);
    expect(session.getUserManager()).andReturn(um).anyTimes();
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
        "Failed to get the profile node.");
    replay();

    servlet.doGet(request, response);
  }

  @Test
  public void testMessages() throws MessagingException, JSONException,
      RepositoryException {

    MeServlet servlet = new MeServlet();
    servlet.messagingService = messagingService;

    Authorizable au = createAuthorizable("jack", false, true);
    expect(messagingService.getFullPathToStore("jack", session)).andReturn(
        "/path/to/store");

    // Mock the query.
    Workspace workSpace = createMock(Workspace.class);
    QueryManager qm = createMock(QueryManager.class);
    Query q = createMock(Query.class);
    QueryResult qr = createMock(QueryResult.class);
    NodeIterator iterator = createMock(NodeIterator.class);
    expect(session.getWorkspace()).andReturn(workSpace);
    expect(workSpace.getQueryManager()).andReturn(qm);
    expect(
        qm
            .createQuery(EasyMock.matches(".*\\/path\\/to\\/store.*"), EasyMock
                .eq("xpath"))).andReturn(q);
    expect(q.execute()).andReturn(qr);
    expect(qr.getNodes()).andReturn(iterator);
    expect(iterator.hasNext()).andReturn(true).times(2).andReturn(false);
    expect(iterator.next()).andReturn(null).times(2);

    replay();
    servlet.writeMessageCounts(new ExtendedJSONWriter(w), session, au);

    w.flush();
    JSONObject o = new JSONObject(baos.toString());
    assertEquals(2, o.get("unread"));
  }

  @Test
  public void testContactCounts() throws MessagingException, JSONException,
      RepositoryException {

    Authorizable au = createAuthorizable("jack", false, true);

    // Mock the query.
    Workspace workSpace = createMock(Workspace.class);
    QueryManager qm = createMock(QueryManager.class);
    Query q = createMock(Query.class);
    QueryResult qr = createMock(QueryResult.class);
    NodeIterator iterator = createMock(NodeIterator.class);
    expect(session.getWorkspace()).andReturn(workSpace);
    expect(workSpace.getQueryManager()).andReturn(qm);
    expect(
        qm.createQuery(EasyMock.matches(".*\\/_user\\/j\\/ja\\/jack\\/contacts.*"),
            EasyMock.eq("xpath"))).andReturn(q);
    expect(q.execute()).andReturn(qr);
    expect(qr.getNodes()).andReturn(iterator);

    MockNode pendingNode = new MockNode("/path/to/node");
    pendingNode.setProperty(SAKAI_CONNECTION_STATE, PENDING.toString());
    MockNode acceptedNode = new MockNode("/path/to/node");
    acceptedNode.setProperty(SAKAI_CONNECTION_STATE, ACCEPTED.toString());
    MockNode invitedNode = new MockNode("/path/to/node");
    invitedNode.setProperty(SAKAI_CONNECTION_STATE, INVITED.toString());

    expect(iterator.hasNext()).andReturn(true).times(4).andReturn(false);
    expect(iterator.nextNode()).andReturn(pendingNode).andReturn(invitedNode).andReturn(
        acceptedNode).andReturn(invitedNode);

    replay();
    servlet.writeContactCounts(new ExtendedJSONWriter(w), session, au);

    w.flush();
    JSONObject o = new JSONObject(baos.toString());
    assertEquals(2, o.get("invited"));
    assertEquals(1, o.get("accepted"));
    assertEquals(1, o.get("pending"));
  }
}
