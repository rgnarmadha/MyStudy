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
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.proxy.ProxyResponse;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;


/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class RSSProxyPostProcessorTest {

  RSSProxyPostProcessor proxyPostProcessor;

  @Mock
  private SlingHttpServletResponse response;

  @Mock
  private ProxyResponse proxyResponse;

  @Mock
  private Map<String, String[]> proxyResponseHeaders;

  @Mock
  private ServletOutputStream responseOutputStream;

  @Before
  public void setup() {
    proxyPostProcessor = new RSSProxyPostProcessor();
  }

  @Test
  public void nameIsAsExpected() {
    assertEquals("rss", proxyPostProcessor.getName());
  }

  @Test
  public void rejectsTooLongResponse() throws Exception {
    //given
    proxyResponseCanReturnHeaders();
    proxyResponseHeaderContainsVeryLongContentLength();

    //when
    proxyPostProcessor.process(null, response, proxyResponse);

    //then
    verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
  }

  @Test
  public void rejectsUnsupportedContentType() throws Exception {
    //given
    proxyResponseCanReturnHeaders();
    proxyResponseHeaderContainsUnsupportedContentType();

    //when
    proxyPostProcessor.activate(new HashMap<String, Object>());
    proxyPostProcessor.process(null, response, proxyResponse);

    //then
    verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
  }

  @Test
  public void canValidateAndWriteRssToTheSlingResponse() throws Exception {
    //given
    proxyResponseCanReturnHeaders();
    proxyResponseHasSampleRss();
    responseHasOutputStreamAvailable();

    //when
    proxyPostProcessor.activate(new HashMap<String, Object>());
    proxyPostProcessor.process(null, response, proxyResponse);

    //then

  }

  @Test
  public void rejectsRssWithoutChannelTitle() throws Exception {
  //given
    proxyResponseCanReturnHeaders();
    proxyResponseHasInvalidRss();

    //when
    proxyPostProcessor.activate(new HashMap<String, Object>());
    proxyPostProcessor.process(null, response, proxyResponse);

    //then
    verify(response).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), anyString());
  }

  @Test
  public void rejectsRssWithoutWellFormedXml() throws Exception {
  //given
    proxyResponseCanReturnHeaders();
    proxyResponseHasIllFormedXml();

    //when
    proxyPostProcessor.activate(new HashMap());
    proxyPostProcessor.process(null, response, proxyResponse);

    //then
    verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
  }

  private void proxyResponseHeaderContainsUnsupportedContentType() {
    when(proxyResponseHeaders.get("Content-Type")).thenReturn(new String[]{"application/foo"});
  }

  private void proxyResponseHeaderContainsVeryLongContentLength() {
    when(proxyResponseHeaders.get("Content-Length")).thenReturn(new String[]{"" + Integer.MAX_VALUE});
  }

  private void proxyResponseCanReturnHeaders() {
    when(proxyResponse.getResponseHeaders()).thenReturn(proxyResponseHeaders);
  }

  private void proxyResponseHasSampleRss() throws Exception {
    when(proxyResponse.getResponseBodyAsInputStream()).thenReturn(this.getClass().getClassLoader().getResourceAsStream("sample-rss.xml"));
  }

  private void proxyResponseHasInvalidRss() throws Exception {
    when(proxyResponse.getResponseBodyAsInputStream()).thenReturn(this.getClass().getClassLoader().getResourceAsStream("invalid-sample-rss.xml"));
  }

  private void proxyResponseHasIllFormedXml() throws Exception {
    when(proxyResponse.getResponseBodyAsInputStream()).thenReturn(this.getClass().getClassLoader().getResourceAsStream("invalid-xml.xml"));
  }

  private void responseHasOutputStreamAvailable() throws Exception {
    when(response.getOutputStream()).thenReturn(responseOutputStream);
  }

}
