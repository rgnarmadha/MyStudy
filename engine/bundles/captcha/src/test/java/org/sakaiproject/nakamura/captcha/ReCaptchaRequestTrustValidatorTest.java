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
package org.sakaiproject.nakamura.captcha;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sakaiproject.nakamura.api.auth.trusted.RequestTrustValidator;
import org.sakaiproject.nakamura.testutils.http.DummyServer;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 *
 */
public class ReCaptchaRequestTrustValidatorTest {
  @Mock
  private HttpServletRequest request;

  private DummyServer server;
  private ReCaptchaService reCaptchaService;
  private ReCaptchaRequestTrustValidator trustValidator;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    server = new DummyServer();

    reCaptchaService = new ReCaptchaService();
    Map<String, String> properties = new HashMap<String, String>();
    properties.put(ReCaptchaService.RECAPTCHA_ENDPOINT, server.getUrl());
    properties.put(ReCaptchaService.KEY_PRIVATE, "myUberPrivateKey");
    properties.put(ReCaptchaService.KEY_PUBLIC, "some-lame-public-key");
    reCaptchaService.activate(properties);

    trustValidator = new ReCaptchaRequestTrustValidator();
    trustValidator.captchaService = reCaptchaService;
  }

  @Test
  public void testProperRequest() {
    server.setResponseBody("true");
    server.setStatus(200);

    when(request.getParameter(":recaptcha-challenge")).thenReturn("12345");
    when(request.getParameter(":recaptcha-response")).thenReturn("some word");
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");

    assertTrue(trustValidator.isTrusted(request));
  }

  @Test
  public void testMissingParameters() {
    assertFalse(reCaptchaService.checkRequest(request));
  }

  @Test
  public void testWrongStatusCode() {
    server.setStatus(401);

    when(request.getParameter(":recaptcha-challenge")).thenReturn("12345");
    when(request.getParameter(":recaptcha-response")).thenReturn("some word");
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");

    assertFalse(trustValidator.isTrusted(request));
  }

  @Test
  public void testWrongOutput() {
    server.setResponseBody("false because of foobar");
    server.setStatus(200);

    when(request.getParameter(":recaptcha-challenge")).thenReturn("12345");
    when(request.getParameter(":recaptcha-response")).thenReturn("some word");
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");

    assertFalse(trustValidator.isTrusted(request));
  }

  @Test
  public void testLevel() {
    assertTrue(trustValidator.getLevel() >= RequestTrustValidator.CREATE_USER);
  }
}
