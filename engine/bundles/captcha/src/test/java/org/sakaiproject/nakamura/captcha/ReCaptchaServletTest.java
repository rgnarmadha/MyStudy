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

import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

/**
 *
 */
public class ReCaptchaServletTest {

  private ReCaptchaServlet servlet;
  private ReCaptchaService service;

  private String publicKey = "my-public-key";

  @Mock
  private SlingHttpServletRequest request;
  @Mock
  private SlingHttpServletResponse response;

  private StringWriter stringWriter;
  private PrintWriter printWriter;

  @Before
  public void setUp() throws IOException {
    servlet = new ReCaptchaServlet();

    service = new ReCaptchaService();
    Map<String, String> properties = new HashMap<String, String>();
    properties.put(ReCaptchaService.KEY_PRIVATE, "myUberPrivateKey");
    properties.put(ReCaptchaService.KEY_PUBLIC, publicKey);
    properties.put(ReCaptchaService.RECAPTCHA_ENDPOINT, "http://foo.com/captcha/service");
    service.activate(properties);
    servlet.captchaService = service;

    MockitoAnnotations.initMocks(this);

    stringWriter = new StringWriter();
    printWriter = new PrintWriter(stringWriter);

    when(response.getWriter()).thenReturn(printWriter);

  }

  @Test
  public void testJSON() throws ServletException, IOException {
    servlet.doGet(request, response);
    verify(response).setContentType("application/json");
    verify(response).setCharacterEncoding("UTF-8");
  }

  @Test
  public void testProperties() throws ServletException, IOException, JSONException {
    servlet.doGet(request, response);
    printWriter.flush();
    JSONObject json = new JSONObject(stringWriter.toString());
    assertEquals(1, json.length());
    assertEquals(publicKey, json.get("public-key"));
  }

}
