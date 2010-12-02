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
package org.sakaiproject.nakamura.proxy;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.proxy.ProxyResponse;
import org.sakaiproject.nakamura.proxy.HeaderProxyPostProcessor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;

@RunWith(value = MockitoJUnitRunner.class)
public class HeaderProxyPostProcessorTest {
  private static final String CONTENT_TYPE_HEADER = "Content-Type";

  private HeaderProxyPostProcessor processor;

  @Mock
  SlingHttpServletResponse response;

  @Mock
  ProxyResponse proxyResponse;

  @Before
  public void setUp() {
    processor = new HeaderProxyPostProcessor();
  }

  @Test
  public void testName() {
    assertEquals(HeaderProxyPostProcessor.NAME, processor.getName());
  }

  @Test
  public void processWithSingleRequestedHeader() throws Exception {
    // construct headers
    HashMap<String, String[]> headers = new HashMap<String, String[]>();
    headers.put(CONTENT_TYPE_HEADER, new String[] { "application/json" });
    when(proxyResponse.getResponseHeaders()).thenReturn(headers);

    // create a writer for the response
    StringWriter sw = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(sw));

    HashMap<String, Object> templateParams = new HashMap<String, Object>();
    templateParams.put("header", CONTENT_TYPE_HEADER);
    processor.process(templateParams, response, proxyResponse);

    // construct the expected output
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(CONTENT_TYPE_HEADER, "application/json");

    assertEquals(jsonObject.toString(), sw.toString());
  }

  @Test
  public void processWithMultipleRequestedHeaders() throws Exception {
    // construct headers
    HashMap<String, String[]> headers = new HashMap<String, String[]>();
    headers.put(CONTENT_TYPE_HEADER, new String[] { "application/json" });
    when(proxyResponse.getResponseHeaders()).thenReturn(headers);

    // create a writer for the response
    StringWriter sw = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(sw));

    HashMap<String, Object> templateParams = new HashMap<String, Object>();
    templateParams.put("header", new String[] { CONTENT_TYPE_HEADER, "Content-Length" });
    processor.process(templateParams, response, proxyResponse);

    // construct the expected output
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(CONTENT_TYPE_HEADER, "application/json");

    assertEquals(jsonObject.toString(), sw.toString());
  }

  @Test
  public void processWithoutRequestedHeaders() throws Exception {
    // construct headers
    HashMap<String, String[]> headers = new HashMap<String, String[]>();
    headers.put(CONTENT_TYPE_HEADER, new String[] { "application/json" });
    when(proxyResponse.getResponseHeaders()).thenReturn(headers);

    // create a writer for the response
    StringWriter sw = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(sw));

    HashMap<String, Object> templateParams = new HashMap<String, Object>();
    processor.process(templateParams, response, proxyResponse);

    // construct the expected output
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(CONTENT_TYPE_HEADER, "application/json");

    assertEquals(jsonObject.toString(), sw.toString());
  }

  @Test
  public void processWithEmptyRequestedHeader() throws Exception {
    // construct headers
    HashMap<String, String[]> headers = new HashMap<String, String[]>();
    headers.put(CONTENT_TYPE_HEADER, null);
    when(proxyResponse.getResponseHeaders()).thenReturn(headers);

    // create a writer for the response
    StringWriter sw = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(sw));

    HashMap<String, Object> templateParams = new HashMap<String, Object>();
    templateParams.put("header", CONTENT_TYPE_HEADER);
    processor.process(templateParams, response, proxyResponse);

    // construct the expected output
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(CONTENT_TYPE_HEADER, "");

    assertEquals(jsonObject.toString(), sw.toString());
  }

  @Test
  public void processWithEmptyRequestedHeaderValue() throws Exception {
    // construct headers
    HashMap<String, String[]> headers = new HashMap<String, String[]>();
    headers.put(CONTENT_TYPE_HEADER, new String[] { null });
    when(proxyResponse.getResponseHeaders()).thenReturn(headers);

    // create a writer for the response
    StringWriter sw = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(sw));

    HashMap<String, Object> templateParams = new HashMap<String, Object>();
    templateParams.put("header", CONTENT_TYPE_HEADER);
    processor.process(templateParams, response, proxyResponse);

    // construct the expected output
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(CONTENT_TYPE_HEADER, "");

    assertEquals(jsonObject.toString(), sw.toString());
  }
}
