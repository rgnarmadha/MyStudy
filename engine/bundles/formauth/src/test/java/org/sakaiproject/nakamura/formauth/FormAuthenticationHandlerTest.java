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
package org.sakaiproject.nakamura.formauth;

import static org.apache.sling.jcr.resource.JcrResourceConstants.AUTHENTICATION_INFO_CREDENTIALS;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.easymock.Capture;
import org.junit.Test;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 */
public class FormAuthenticationHandlerTest {

  /**
   * Test a normal login
   */
  @Test
  public void testLoginOk() {
    FormAuthenticationHandler formAuthenticationHandler = new FormAuthenticationHandler();
    HttpServletRequest request = createMock(HttpServletRequest.class);
    HttpServletResponse response = createMock(HttpServletResponse.class);
    HttpSession session = createMock(HttpSession.class);


    expect(session.getCreationTime()).andReturn(System.currentTimeMillis()).anyTimes();
    expect(session.getLastAccessedTime()).andReturn(System.currentTimeMillis()).anyTimes();
    expect(request.getMethod()).andReturn("POST");
    expect(request.getParameter(FormLoginServlet.TRY_LOGIN)).andReturn("1");
    expect(request.getParameter(FormLoginServlet.USERNAME)).andReturn("user").atLeastOnce();
    expect(request.getParameter(FormLoginServlet.PASSWORD))
        .andReturn("password").atLeastOnce();
    Capture<Object> captured = new Capture<Object>();
    Capture<String> key = new Capture<String>();
    request.setAttribute(capture(key), capture(captured));
    expectLastCall();


    replay(request, response, session);


    AuthenticationInfo authenticationInfo = formAuthenticationHandler.extractCredentials(request, response);
    assertEquals(
        "org.sakaiproject.nakamura.formauth.FormAuthenticationHandler$FormAuthentication",
        key.getValue());
    assertEquals(
        "org.sakaiproject.nakamura.formauth.FormAuthenticationHandler$FormAuthentication",
        captured.getValue().getClass().getName());
    assertNotNull(authenticationInfo);
    assertEquals(FormAuthenticationHandler.SESSION_AUTH, authenticationInfo.getAuthType());
    Credentials credentials = (Credentials) authenticationInfo.get(AUTHENTICATION_INFO_CREDENTIALS);
    assertNotNull(credentials);
    SimpleCredentials sc = (SimpleCredentials) credentials;
    assertEquals("user",sc.getUserID());
    assertArrayEquals("password".toCharArray(),sc.getPassword());
    verify(request, response, session);
  }


  /**
   * Test logout from an existing session.
   */
  @Test
  public void testLogout() {
    FormAuthenticationHandler formAuthenticationHandler = new FormAuthenticationHandler();
    HttpServletRequest request = createMock(HttpServletRequest.class);
    HttpServletResponse response = createMock(HttpServletResponse.class);

    expect(request.getMethod()).andReturn("POST");
    expect(request.getParameter(FormLoginServlet.TRY_LOGIN)).andReturn(null);

    replay(request, response);
    formAuthenticationHandler.extractCredentials(request, response);
    verify(request, response);
  }




}
