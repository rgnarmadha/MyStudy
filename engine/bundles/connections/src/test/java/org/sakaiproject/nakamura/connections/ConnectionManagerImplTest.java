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
package org.sakaiproject.nakamura.connections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.sakaiproject.nakamura.api.connections.ConnectionConstants;
import org.sakaiproject.nakamura.api.connections.ConnectionException;
import org.sakaiproject.nakamura.api.connections.ConnectionState;
import org.sakaiproject.nakamura.api.locking.LockManager;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.user.UserConstants;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

/**
 *
 */
public class ConnectionManagerImplTest {

  private ConnectionManagerImpl connectionManager;
  private LockManager lockManager;
  private ProfileService profileService;

  @Before
  public void setUp() {
    lockManager = mock(LockManager.class);
    profileService = mock(ProfileService.class);

    connectionManager = new ConnectionManagerImpl();
    connectionManager.lockManager = lockManager;
    connectionManager.profileService = profileService;
  }

  @Test
  public void testAddArbitraryProperties() throws RepositoryException {
    MockNode node = new MockNode("/path/to/connection/node");
    Map<String, String[]> properties = new HashMap<String, String[]>();
    properties.put("alfa", new String[] { "a" });
    properties.put("beta", new String[] { "a", "b" });
    properties.put("charlie", new String[] { "c" });

    connectionManager.addArbitraryProperties(node, properties);
    assertEquals(node.getProperty("alfa").getValues().length, 1);
    assertEquals(node.getProperty("beta").getValues().length, 2);
    assertEquals(node.getProperty("charlie").getValues().length, 1);
  }

  @Test
  public void testHandleInvitation() throws RepositoryException {
    // Alice is student and supervised
    // Bob is the supervisor and the lecturer

    MockNode fromNode = new MockNode("/_user/a/al/alice/contacts/b/bo/bob");
    MockNode toNode = new MockNode("/_user/b/bo/bob/contacts/a/al/alice");

    Map<String, String[]> props = new HashMap<String, String[]>();

    props.put(ConnectionConstants.PARAM_FROM_RELATIONSHIPS, new String[] { "Supervisor",
        "Lecturer" });
    props.put(ConnectionConstants.PARAM_TO_RELATIONSHIPS, new String[] { "Supervised",
        "Student" });
    props.put(ConnectionConstants.SAKAI_CONNECTION_TYPES, new String[] { "foo" });
    props.put("random", new String[] { "israndom" });

    connectionManager.handleInvitation(props, null, fromNode, toNode);

    Value[] fromValues = fromNode.getProperty(ConnectionConstants.SAKAI_CONNECTION_TYPES)
        .getValues();

    Value[] toValues = toNode.getProperty(ConnectionConstants.SAKAI_CONNECTION_TYPES)
        .getValues();

    assertEquals(3, fromValues.length);
    int j = 0;
    // order may not be what we expect it to be
    for ( int i = 0; i < 3; i++ ) {
      if ( "foo".equals(fromValues[i].getString())) {
        j = j|1;
      }
      if ( "Lecturer".equals(fromValues[i].getString())) {
        j = j|2;
      }
      if ( "Supervisor".equals(fromValues[i].getString())) {
        j = j|4;
      }
    }

    Assert.assertTrue((j&1)==1);
    Assert.assertTrue((j&2)==2);
    Assert.assertTrue((j&4)==4);
    assertEquals(3, toValues.length);

    j = 0;
    for ( int i = 0; i < 3; i++ ) {
      if ( "foo".equals(toValues[i].getString())) {
        j = j|1;
      }
      if ( "Student".equals(toValues[i].getString())) {
        j = j|2;
      }
      if ( "Supervised".equals(toValues[i].getString())) {
        j = j|4;
      }
    }
    Assert.assertTrue((j&1)==1);
    Assert.assertTrue((j&2)==2);
    Assert.assertTrue((j&4)==4);


    Value[] fromRandomValues = fromNode.getProperty("random").getValues();
    Value[] toRandomValues = toNode.getProperty("random").getValues();

    assertEquals("israndom", fromRandomValues[0].getString());
    assertEquals("israndom", toRandomValues[0].getString());
  }

  @Test
  public void testCheckValidUserIdAnon() {
    Session session = mock(Session.class);
    try {
      connectionManager.checkValidUserId(session, UserConstants.ANON_USERID);
      fail("This should've thrown a ConnectionException.");
    } catch (ConnectionException e) {
      assertEquals(403, e.getCode());
    }
  }

  @Test
  public void testCheckValidUserIdNonExisting() throws RepositoryException {
    JackrabbitSession session = mock(JackrabbitSession.class);
    UserManager um = mock(UserManager.class);
    when(session.getUserManager()).thenReturn(um);
    when(session.getUserID()).thenReturn("bob");
    when(um.getAuthorizable("alice")).thenReturn(null);
    try {
      connectionManager.checkValidUserId(session, "alice");
      fail("This should've thrown a ConnectionException.");
    } catch (ConnectionException e) {
      assertEquals(404, e.getCode());
    }
  }

  @Test
  public void testCheckValidUserId() throws ConnectionException, RepositoryException {
    JackrabbitSession session = mock(JackrabbitSession.class);
    UserManager um = mock(UserManager.class);
    Authorizable au = mock(Authorizable.class);
    when(au.getID()).thenReturn("alice");
    when(session.getUserManager()).thenReturn(um);
    when(session.getUserID()).thenReturn("bob");
    when(um.getAuthorizable("alice")).thenReturn(au);
    Authorizable actual = connectionManager.checkValidUserId(session, "alice");
    assertEquals(au, actual);
  }

  @Test
  public void testGetConnectionState() throws ConnectionException, RepositoryException {
    // Passing in null
    try {
      connectionManager.getConnectionState(null);
      fail("Passing in null should result in exception.");
    } catch (IllegalArgumentException e) {
      // Swallow it and continue with the test.
    }

    // PAssing in node without property should result in state == None
    MockNode node = new MockNode("/path/to/connection");
    ConnectionState state = connectionManager.getConnectionState(node);
    assertEquals(ConnectionState.NONE, state);

    // Passing in node with state property.
    node.setProperty(ConnectionConstants.SAKAI_CONNECTION_STATE, "ACCEPTED");
    state = connectionManager.getConnectionState(node);
    assertEquals(ConnectionState.ACCEPTED, state);

    // Passing in node with wrong state property.
    node.setProperty(ConnectionConstants.SAKAI_CONNECTION_STATE, "fubar");
    state = connectionManager.getConnectionState(node);
    assertEquals(ConnectionState.NONE, state);
  }

  @Test
  public void testDeepGetCreateNodeExisting() throws RepositoryException {
    JackrabbitSession session = mock(JackrabbitSession.class);
    Authorizable from = mock(Authorizable.class);
    Authorizable to = mock(Authorizable.class);

    when(from.getID()).thenReturn("alice");
    when(from.isGroup()).thenReturn(false);

    when(to.getID()).thenReturn("bob");
    when(to.isGroup()).thenReturn(false);

    String contactPath = "/_user/alice/contacts/bob";
    Node expectedNode = new MockNode(contactPath);
    when(session.itemExists(contactPath)).thenReturn(true);
    when(session.getItem(contactPath)).thenReturn(expectedNode);
    Node result = connectionManager.getOrCreateConnectionNode(session, from, to);
    assertEquals(contactPath, result.getPath());
  }

  @Test
  public void testDeepGetCreateNodeExistingBase() throws RepositoryException {
    JackrabbitSession session = mock(JackrabbitSession.class);
    Authorizable from = mock(Authorizable.class);
    Authorizable to = mock(Authorizable.class);

    when(from.getID()).thenReturn("alice");
    when(from.isGroup()).thenReturn(false);

    when(to.getID()).thenReturn("bob");
    when(to.isGroup()).thenReturn(false);
    Node profile = mock(Node.class);
    when(profile.getIdentifier()).thenReturn("bob-iden-tifi-er");
    when(profileService.getProfilePath(Matchers.isA(Authorizable.class))).thenReturn("/_user/bob/public/authprofile");
    when(session.getItem("/_user/bob/public/authprofile")).thenReturn(profile);

    String basePath = "/_user/alice/contacts";
    String contactPath = basePath + "/bob";

    Node baseNode = mock(Node.class);
    when(baseNode.getPath()).thenReturn(basePath);
    Node contactNode = mock(Node.class);
    when(contactNode.getPath()).thenReturn(contactPath);
    when(contactNode.isNew()).thenReturn(true);
    when(baseNode.addNode("bob")).thenReturn(contactNode);

    when(session.itemExists(contactPath)).thenReturn(false);
    when(session.itemExists(basePath)).thenReturn(true);
    when(session.getItem(basePath)).thenReturn(baseNode);

    Node result = connectionManager.getOrCreateConnectionNode(session, from, to);
    assertEquals(contactPath, result.getPath());
    verify(contactNode).setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, ConnectionConstants.SAKAI_CONTACT_RT);
    verify(contactNode).setProperty("jcr:reference", "bob-iden-tifi-er", PropertyType.REFERENCE);
  }
}
