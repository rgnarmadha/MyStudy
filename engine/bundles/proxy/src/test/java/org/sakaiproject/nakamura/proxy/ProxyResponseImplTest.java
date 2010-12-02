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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ProxyResponseImplTest {

  @Mock
  private HttpMethod method;

  private List<Header> headers;

  @Before
  public void setup() {
    headers = new ArrayList<Header>();
  }

  @Test
  public void constructWithMultiValuedHeader() {
    // given
    int result = 200;
    methodHasAMultiValuedHeader();
    methodReturnsHeaders();
    // when
    ProxyResponseImpl proxyResponse = new ProxyResponseImpl(result, method);

    // then
    Map<String, String[]> proxyResponseHeaders = proxyResponse.getResponseHeaders();
    assertEquals(2, proxyResponseHeaders.get("Accept").length);
  }

  @Test
  public void accessorsJustHandBackWhatIsOnTheMethodObject() throws Exception {
    // given
    int result = 200;
    methodHasAResponseBody();
    methodReturnsEmptyHeaders();

    // when
    ProxyResponseImpl proxyResponse = new ProxyResponseImpl(result, method);

    // then
    assertEquals(result, proxyResponse.getResultCode());
    assertEquals(method.getResponseBody(), proxyResponse.getResponseBody());
    assertEquals(method.getResponseBodyAsStream(), proxyResponse
        .getResponseBodyAsInputStream());
    assertEquals(method.getResponseBodyAsString(), proxyResponse
        .getResponseBodyAsString());
  }

  @Test
  public void throwsAwayJSESSIONIDCookie() {
    // given
    int result = 200;
    methodHasJSESSIONIDHeader();
    methodHasSomeOtherSetCookieHeader();
    methodReturnsHeaders();

    // when
    ProxyResponseImpl proxyResponse = new ProxyResponseImpl(result, method);

    // then
    assertTrue(proxyResponse.getResponseHeaders().containsKey("set-cookie"));
    for (String headerValue : proxyResponse.getResponseHeaders().get("set-cookie")) {
      assertFalse(headerValue.contains("JSESSIONID"));
    }
  }

  private void methodReturnsHeaders() {
    when(method.getResponseHeaders()).thenReturn(headers.toArray(new Header[] {}));
  }

  private void methodHasSomeOtherSetCookieHeader() {
    headers.add(new Header("set-cookie", "supercoolness=extreme"));
  }

  private void methodHasJSESSIONIDHeader() {
    headers.add(new Header("set-cookie", "JSESSIONID-30sdkf2-3dkfjsie"));
  }

  private void methodReturnsEmptyHeaders() {
    when(method.getResponseHeaders()).thenReturn(new Header[] {});
  }

  @SuppressWarnings("deprecation")
  private void methodHasAResponseBody() throws Exception {
    String body = "Hello, world.";
    when(method.getResponseBody()).thenReturn(body.getBytes());
    when(method.getResponseBodyAsStream()).thenReturn(
        new java.io.StringBufferInputStream(body));
    when(method.getResponseBodyAsString()).thenReturn(body);
  }

  private void methodHasAMultiValuedHeader() {
    headers.add(new Header("Accept", "text/plain"));
    headers.add(new Header("Accept", "text/html"));
  }

}
