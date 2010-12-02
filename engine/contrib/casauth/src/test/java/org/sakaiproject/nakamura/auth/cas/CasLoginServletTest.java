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
package org.sakaiproject.nakamura.auth.cas;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.auth.trusted.TrustedTokenServiceImpl;

import javax.servlet.http.HttpSession;

/**
 *
 */
@RunWith(value = MockitoJUnitRunner.class)
public class CasLoginServletTest {
  CasLoginServlet servlet;
  TrustedTokenServiceImpl trustedTokenService;

  @Mock
  CasAuthenticationHandler handler;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  SlingHttpServletRequest request;

  @Mock
  HttpSession session;

  @Mock
  SlingHttpServletResponse response;

  @Mock
  AuthenticationInfo authnInfo;

  @Before
  public void setUp() throws Exception {
    trustedTokenService = new TrustedTokenServiceImpl();
    trustedTokenService.activateForTesting();
    servlet = new CasLoginServlet(handler, trustedTokenService);

    when(request.getSession()).thenReturn(session);
  }

  @Test
  public void coverageBooster() {
    new CasLoginServlet();
  }

  @Test
  public void requestCredentials() throws Exception {
    servlet.service(request, response);
    verify(handler).requestCredentials(request, response);
  }

  @Test
  public void redirectWithoutTarget() throws Exception {
    when(request.getAuthType()).thenReturn(CasAuthenticationHandler.AUTH_TYPE);

    ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);

    when(request.getContextPath()).thenReturn("someContextPath");
    when(request.getAttribute(CasAuthenticationHandler.AUTHN_INFO)).thenReturn(null);
    servlet.service(request, response);

    verify(response).sendRedirect(redirectCaptor.capture());
    verify(handler, Mockito.never()).requestCredentials(request, response);

    assertEquals("someContextPath/", redirectCaptor.getValue());
  }

  @Test
  public void redirectWithTarget() throws Exception {
    when(request.getAuthType()).thenReturn(CasAuthenticationHandler.AUTH_TYPE);

    ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);

    when(request.getAttribute(CasAuthenticationHandler.AUTHN_INFO)).thenReturn(null);
    when(request.getRequestURI()).thenReturn("someURI");
    when(request.getAttribute(Authenticator.LOGIN_RESOURCE)).thenReturn("greatplace");
    servlet.service(request, response);

    verify(response).sendRedirect(redirectCaptor.capture());
    verify(handler, Mockito.never()).requestCredentials(request, response);

    assertEquals("greatplace", redirectCaptor.getValue());
  }

  @Test
  public void redirectWithTargetEqualsRequestURI() throws Exception {
    when(request.getAuthType()).thenReturn(CasAuthenticationHandler.AUTH_TYPE);

    ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);

    when(request.getAttribute(CasAuthenticationHandler.AUTHN_INFO)).thenReturn(null);
    when(request.getRequestURI()).thenReturn("someURI");
    when(request.getParameter(Authenticator.LOGIN_RESOURCE)).thenReturn("greatplace");
    servlet.service(request, response);

    verify(response).sendRedirect(redirectCaptor.capture());
    verify(handler, Mockito.never()).requestCredentials(request, response);

    assertEquals("greatplace", redirectCaptor.getValue());
  }

  @Test
  public void authAndAddToken() throws Exception {
    when(request.getAuthType()).thenReturn(CasAuthenticationHandler.AUTH_TYPE);

    ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);

    when(request.getAttribute(CasAuthenticationHandler.AUTHN_INFO)).thenReturn(authnInfo);
    when(request.getRequestURI()).thenReturn("someURI");
    when(request.getAttribute(Authenticator.LOGIN_RESOURCE)).thenReturn("greatplace");
    servlet.service(request, response);

    verify(response).sendRedirect(redirectCaptor.capture());
    verify(handler, Mockito.never()).requestCredentials(request, response);

    assertEquals("greatplace", redirectCaptor.getValue());
  }
}
