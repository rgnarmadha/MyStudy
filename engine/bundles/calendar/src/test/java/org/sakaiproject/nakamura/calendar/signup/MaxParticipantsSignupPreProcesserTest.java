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
package org.sakaiproject.nakamura.calendar.signup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sakaiproject.nakamura.api.calendar.CalendarConstants.SAKAI_EVENT_SIGNUP_PARTICIPANT_RT;
import static org.sakaiproject.nakamura.calendar.signup.MaxParticipantsSignupPreProcessors.SAKAI_EVENT_MAX_PARTICIPANTS;

import junit.framework.Assert;

import org.apache.sling.api.SlingHttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.calendar.CalendarException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class MaxParticipantsSignupPreProcesserTest {

  private MaxParticipantsSignupPreProcessors processor;

  @Before
  public void setUp() {
    processor = new MaxParticipantsSignupPreProcessors();
  }

  @Test
  public void testNoCheckNeeded() {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    Node signupNode = mock(Node.class);
    try {
      processor.checkSignup(request, signupNode);
    } catch (CalendarException e) {
      fail("This should not throw an exception.");
    }
  }

  @Test
  public void testMax() throws RepositoryException {
    String signupPath = "/path/to/signup";
    long maxParticipants = 2;
    String queryString = "/jcr:root" + signupPath + "//*[@sling:resourceType='"
        + SAKAI_EVENT_SIGNUP_PARTICIPANT_RT + "']";

    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    Node signupNode = mock(Node.class);
    Property maxParticipantsProp = mock(Property.class);
    Session session = mock(Session.class);
    Workspace workspace = mock(Workspace.class);
    QueryManager qm = mock(QueryManager.class);
    QueryResult qr = mock(QueryResult.class);
    Query query = mock(Query.class);
    NodeIterator nodeIterator = mock(NodeIterator.class);

    // Mock the property
    when(maxParticipantsProp.getLong()).thenReturn(maxParticipants);

    // Mock the signup node.
    when(signupNode.getSession()).thenReturn(session);
    when(signupNode.getPath()).thenReturn(signupPath);
    when(signupNode.hasProperty(SAKAI_EVENT_MAX_PARTICIPANTS)).thenReturn(true);
    when(signupNode.getProperty(SAKAI_EVENT_MAX_PARTICIPANTS)).thenReturn(
        maxParticipantsProp);

    // Mock the query
    when(session.getWorkspace()).thenReturn(workspace);
    when(workspace.getQueryManager()).thenReturn(qm);
    when(qm.createQuery(queryString, Query.XPATH)).thenReturn(query);
    when(query.execute()).thenReturn(qr);
    when(qr.getNodes()).thenReturn(nodeIterator);

    // Mock the node iterator (4 found nodes.)
    when(nodeIterator.hasNext()).thenReturn(true, true, true, true, false);

    try {
      processor.checkSignup(request, signupNode);
      fail("This should have thrown an exception!");
    } catch (CalendarException e) {
      Assert.assertEquals(HttpServletResponse.SC_BAD_REQUEST, e.getCode());
    }
  }

  @Test
  public void testException() throws RepositoryException {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    Node signupNode = mock(Node.class);

    when(signupNode.hasProperty(SAKAI_EVENT_MAX_PARTICIPANTS)).thenThrow(
        new RepositoryException());

    try {
      processor.checkSignup(request, signupNode);
      fail("Should've thrown an exception.");
    } catch (CalendarException e) {
      assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getCode());
    }

  }
}
