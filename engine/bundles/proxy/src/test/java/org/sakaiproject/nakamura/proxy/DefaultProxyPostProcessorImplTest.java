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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.proxy.ProxyResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletOutputStream;


/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultProxyPostProcessorImplTest {

  @Mock
  private SlingHttpServletResponse response;

  @Mock
  private ProxyResponse proxyResponse;

  private InputStream proxyResponseInputStream;

  @Mock
  private ServletOutputStream responseOutputStream;

  private Map<String, String[]> responseHeaders;

  private DefaultProxyPostProcessorImpl proxyPostProcessor;

  @Before
  public void setup() throws Exception {
    proxyPostProcessor = new DefaultProxyPostProcessorImpl();
    responseHeaders = new HashMap<String, String[]>();
    putInSomeCannedHeaders(responseHeaders);
    proxyResponseInputStream = new ByteArrayInputStream("Hello, world.".getBytes("UTF-8"));
  }

  private void putInSomeCannedHeaders(Map<String, String[]> headerMap) {
    headerMap.put("Date", new String[] {"Wed, 24 Feb 2010 17:11:12 GMT"});
    headerMap.put("Server", new String[] {"Chunked Update Server"});
    headerMap.put("X-XSS-Protection", new String[] {"0"});
    headerMap.put("Cache-Control", new String[] {"public,max-age=21600"});
    headerMap.put("Content-Type", new String[] {"application/vnd.google.safebrowsing-chunk"});
    headerMap.put("Content", new String[] {"public,max-age=21600"});
    headerMap.put("Content-Length", new String[] {"3233"});
    headerMap.put("Age", new String[] {"19"});
  }

  @Test
  public void responseHasAllHeadersFromProxyResponse() throws Exception {
    //given
    proxyResponseCanReturnMapOfHeaders();
    proxyResponseCanReturnBodyAsInputStream();
    slingResponseCanReturnOutputStream();

    //when
    proxyPostProcessor.process(null, response, proxyResponse);

    //then
    for (Entry<String, String[]> proxyResponseHeader : proxyResponse.getResponseHeaders().entrySet()) {
      for (String value : proxyResponseHeader.getValue()) {
        verify(response).setHeader(proxyResponseHeader.getKey(), value);
      }
    }
  }
  private void slingResponseCanReturnOutputStream() throws IOException {
    when(response.getOutputStream()).thenReturn(responseOutputStream);
  }

  private void proxyResponseCanReturnBodyAsInputStream() throws IOException {
    when(proxyResponse.getResponseBodyAsInputStream()).thenReturn(proxyResponseInputStream);
  }

  private void proxyResponseCanReturnMapOfHeaders() {
    when(proxyResponse.getResponseHeaders()).thenReturn(responseHeaders);
  }

  @Test
  public void nameIsAsExpected() {
    assertEquals("default", proxyPostProcessor.getName());
  }

}
