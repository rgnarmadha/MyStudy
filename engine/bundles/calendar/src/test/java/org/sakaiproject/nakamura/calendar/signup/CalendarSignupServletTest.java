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

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sakaiproject.nakamura.api.calendar.CalendarConstants.PARTICIPANTS_NODE_NAME;
import static org.sakaiproject.nakamura.api.calendar.CalendarConstants.SAKAI_EVENT_SIGNUP_PARTICIPANT_RT;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.commons.testing.jcr.MockValue;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sakaiproject.nakamura.api.calendar.CalendarException;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.user.UserConstants;

import java.io.IOException;

import javax.jcr.Value;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class CalendarSignupServletTest {

  private CalendarSignupServlet servlet;
  private MockNode signupNode;
  private String signupPath;
  private JackrabbitSession session;
  private String userName;
  private MockValue pathValue;
  private SlingRepository slingRepository;
  private ProfileService profileService;

  @Before
  public void setUp() throws Exception {
    signupPath = "/path/to/calendar/path/to/event/signup";
    signupNode = new MockNode(signupPath);
    slingRepository = mock(SlingRepository.class);
    servlet = new CalendarSignupServlet();
    servlet.slingRepository = slingRepository;

    UserManager um = mock(UserManager.class);
    userName = "jack";
    Authorizable au = mock(Authorizable.class);
    when(au.hasProperty("path")).thenReturn(true);
    pathValue = new MockValue("/j/ja/jack");
    when(au.getProperty("path")).thenReturn(new Value[] { pathValue });
    when(um.getAuthorizable(userName)).thenReturn(au);
    session = mock(JackrabbitSession.class);
    when(session.getUserID()).thenReturn(userName);
    when(session.getUserManager()).thenReturn(um);
    signupNode.setSession(session);

    profileService = mock(ProfileService.class);
    when(profileService.getProfilePath(any(Authorizable.class))).thenReturn(
        "/j/ja/jack/authprofile");
    servlet.profileService = profileService;
  }

  @Test
  public void testAnon() throws ServletException, IOException {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);
    when(request.getRemoteUser()).thenReturn(UserConstants.ANON_USERID);
    servlet.doPost(request, response);

    verify(response).sendError(Mockito.eq(HttpServletResponse.SC_UNAUTHORIZED),
        Mockito.anyString());
  }

  @Test
  public void testSignedupAlready() throws Exception {

    when(
        session.itemExists(signupPath + "/" + PARTICIPANTS_NODE_NAME
            + pathValue.getString())).thenReturn(true);

    try {
      servlet.checkAlreadySignedup(signupNode);
      fail("This should have thrown an exception.");
    } catch (CalendarException e) {
      assertEquals(400, e.getCode());
    }
  }

  @Test
  public void testHandleSignup() throws Exception {

    // Participant node.
    MockNode participantsNode = new MockNode(signupPath + "/" + PARTICIPANTS_NODE_NAME
        + pathValue.getString());

    // Admin session
    JackrabbitSession adminSession = mock(JackrabbitSession.class);
    when(adminSession.itemExists(participantsNode.getPath())).thenReturn(true);
    when(adminSession.getItem(participantsNode.getPath())).thenReturn(participantsNode);
    when(adminSession.hasPendingChanges()).thenReturn(true);

    when(slingRepository.loginAdministrative(null)).thenReturn(adminSession);

    servlet.handleSignup(signupNode);

    verify(adminSession).save();
    verify(adminSession).logout();
    assertEquals(participantsNode.getProperty(SLING_RESOURCE_TYPE_PROPERTY).getString(),
        SAKAI_EVENT_SIGNUP_PARTICIPANT_RT);
    assertEquals(participantsNode.getProperty("sakai:user").getString(), userName);
  }

}
