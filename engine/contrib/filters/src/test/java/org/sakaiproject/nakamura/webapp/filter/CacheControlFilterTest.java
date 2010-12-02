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
package org.sakaiproject.nakamura.webapp.filter;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.component.ComponentContext;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;


@RunWith(MockitoJUnitRunner.class)
public class CacheControlFilterTest {
  
  @Mock SlingHttpServletRequest request;
  @Mock SlingHttpServletResponse response;
  @Mock FilterChain chain;
  
  @Mock ComponentContext context;
  @Mock Dictionary<Object,Object> contextProperties;
  
  private CacheControlFilter cacheControlFilter;
  
  @Before
  public void setup() throws Exception {
    cacheControlFilter = new CacheControlFilter();
    cacheControlFilter.activate(null);
  }
  
  @Test public void setsHeaderOnJavascriptRequests() throws Exception {
    verifyExpiresHeaderWithPath("GET", "/dev/config.js", true);
  }
  
  @Test public void setsHeaderOnCssRequests() throws Exception {
    verifyExpiresHeaderWithPath("GET", "/styles/midnight/main.css", true);
  }
  
  @Test public void willNotSetHeaderOnTextFile() throws Exception {
    verifyExpiresHeaderWithPath("GET", "/~user/zach/Documents/What I Did Last Summer.txt", false);
  }
  
  @Test public void willNotFilterOnPost() throws Exception {
    verifyExpiresHeaderWithPath("POST", "/dev/config.js", false);
  }
  
  @SuppressWarnings("unchecked")
  @Test public void activateWithPatternRules() throws Exception {
    Dictionary props = new Hashtable<Object, Object>();
    props.put("pattern1", ".*\\.(ico|pdf|flv|jpg|jpeg|png|gif|swf)$");
    props.put("maxage1", "432000");
    when(context.getProperties()).thenReturn(props);
    cacheControlFilter.activate(context);
    
    verifyExpiresHeaderWithPath("GET", "/dev/_images/smiley.gif", true);
  }
  
  private void verifyExpiresHeaderWithPath(String method, String path, boolean expectHeader) throws ServletException, IOException {
    when(request.getMethod()).thenReturn(method);
    when(request.getPathInfo()).thenReturn(path);
    
    cacheControlFilter.doFilter(request, response, chain);
    
    if (expectHeader) {
      verify(response).addHeader(anyString(), anyString());
    } else {
      verify(response, never()).addHeader(anyString(), anyString());
    }
  }

}
