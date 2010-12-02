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

import junit.framework.Assert;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.easymock.EasyMock;
import org.junit.Test;
import org.sakaiproject.nakamura.auth.trusted.TrustedTokenServiceImpl;
import org.sakaiproject.nakamura.formauth.FormAuthenticationHandler.FormAuthentication;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Session;
import javax.servlet.ServletException;


/**
 *
 */
public class FormLoginServletTest {

  private List<Object> mocks = new ArrayList<Object>();

  @Test
  public void testDoPostLogin() throws ServletException, IOException, InvalidKeyException, NoSuchAlgorithmException, IllegalStateException {
    SlingHttpServletRequest requestIn = EasyMock.createMock(SlingHttpServletRequest.class);


    FormAuthenticationHandler handler = new FormAuthenticationHandler();
    EasyMock.expect(requestIn.getMethod()).andReturn("POST");
    EasyMock.expect(requestIn.getParameter(FormLoginServlet.TRY_LOGIN)).andReturn("1");
    EasyMock.expect(requestIn.getParameter(FormLoginServlet.USERNAME)).andReturn("ieb").times(2);
    EasyMock.expect(requestIn.getParameter(FormLoginServlet.PASSWORD)).andReturn("pass");
    EasyMock.replay(requestIn);


    FormLoginServlet formLoginServlet = new FormLoginServlet();
    TrustedTokenServiceImpl trustedTokenServiceImpl = new TrustedTokenServiceImpl();
    formLoginServlet.trustedTokenService = trustedTokenServiceImpl;

    FormAuthentication formAuthentication = handler.new FormAuthentication(requestIn);

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    Session session = createMock(Session.class);


    EasyMock.expect(request.getAttribute(FormAuthenticationHandler.FORM_AUTHENTICATION)).andReturn(formAuthentication);

    EasyMock.expect(request.getResourceResolver()).andReturn(resourceResolver);
    EasyMock.expect(resourceResolver.adaptTo(Session.class)).andReturn(session);
    EasyMock.expect(session.getUserID()).andReturn("ieb");


    EasyMock.expect(request.getParameter("d")).andReturn("/test");
    response.sendRedirect("/test");
    EasyMock.expectLastCall();
    replay();

    trustedTokenServiceImpl.activateForTesting();


    formLoginServlet.doPost(request, response);

    List<Object[]> calls = trustedTokenServiceImpl.getCalls();
    Assert.assertEquals(1, calls.size());
    Assert.assertEquals("injectToken", calls.get(0)[0]);
    Assert.assertEquals(request, calls.get(0)[1]);
    Assert.assertEquals(response, calls.get(0)[2]);

    verify();
  }

  @Test
  public void testDoPostLoginAuthFailed() throws ServletException, IOException, InvalidKeyException, NoSuchAlgorithmException, IllegalStateException {


    FormLoginServlet formLoginServlet = new FormLoginServlet();
    TrustedTokenServiceImpl trustedTokenServiceImpl = new TrustedTokenServiceImpl();
    formLoginServlet.trustedTokenService = trustedTokenServiceImpl;


    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);


    EasyMock.expect(request.getAttribute(FormAuthenticationHandler.FORM_AUTHENTICATION)).andReturn(null);


    response.setContentType("text/plain");
    EasyMock.expectLastCall();
    response.setCharacterEncoding("UTF-8");
    EasyMock.expectLastCall();
    replay();

    trustedTokenServiceImpl.activateForTesting();


    formLoginServlet.doPost(request, response);

    List<Object[]> calls = trustedTokenServiceImpl.getCalls();
    Assert.assertEquals(0, calls.size());

    verify();
  }

  @Test
  public void testDoPostLoginInvalid() throws ServletException, IOException, InvalidKeyException, NoSuchAlgorithmException, IllegalStateException {
    SlingHttpServletRequest requestIn = EasyMock.createMock(SlingHttpServletRequest.class);


    FormAuthenticationHandler handler = new FormAuthenticationHandler();
    EasyMock.expect(requestIn.getMethod()).andReturn("POST");
    EasyMock.expect(requestIn.getParameter(FormLoginServlet.TRY_LOGIN)).andReturn("1");
    EasyMock.expect(requestIn.getParameter(FormLoginServlet.USERNAME)).andReturn("ieb").times(2);
    EasyMock.expect(requestIn.getParameter(FormLoginServlet.PASSWORD)).andReturn("pass");
    EasyMock.replay(requestIn);


    FormLoginServlet formLoginServlet = new FormLoginServlet();
    TrustedTokenServiceImpl trustedTokenServiceImpl = new TrustedTokenServiceImpl();
    formLoginServlet.trustedTokenService = trustedTokenServiceImpl;

    FormAuthentication formAuthentication = handler.new FormAuthentication(requestIn);

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    Session session = createMock(Session.class);


    EasyMock.expect(request.getAttribute(FormAuthenticationHandler.FORM_AUTHENTICATION)).andReturn(formAuthentication);

    EasyMock.expect(request.getResourceResolver()).andReturn(resourceResolver);
    EasyMock.expect(resourceResolver.adaptTo(Session.class)).andReturn(session);
    EasyMock.expect(session.getUserID()).andReturn("crossedoversession");


    response.setStatus(401);
    EasyMock.expectLastCall();
    replay();

    trustedTokenServiceImpl.activateForTesting();


    formLoginServlet.doPost(request, response);

    List<Object[]> calls = trustedTokenServiceImpl.getCalls();
    Assert.assertEquals(0, calls.size());

    verify();
  }


  /**
   * @param class1
   * @return
   */
  private <T> T createMock(Class<T> class1) {
    T m = EasyMock.createMock(class1);
    mocks.add(m);
    return m;
  }

  /**
   *
   */
  private void replay() {
    EasyMock.replay(mocks.toArray());
  }

  /**
   *
   */
  private void verify() {
    EasyMock.verify(mocks.toArray());
  }


}
