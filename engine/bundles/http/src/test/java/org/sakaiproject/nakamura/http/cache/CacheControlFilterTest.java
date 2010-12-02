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
package org.sakaiproject.nakamura.http.cache;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.memory.Cache;
import org.sakaiproject.nakamura.api.memory.CacheManagerService;
import org.sakaiproject.nakamura.api.memory.CacheScope;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;

@RunWith(MockitoJUnitRunner.class)
public class CacheControlFilterTest {

  @Mock
  private SlingHttpServletRequest request;

  @Mock
  private SlingHttpServletResponse response;
  
  
  @Mock
  private FilterChain chain;

  private CacheControlFilter cacheControlFilter;

  @Mock
  private ComponentContext componentContext;

  @Mock
  private FilterConfig filterConfig;

  @Mock
  private CacheManagerService cacheMangerService;

  @Mock
  private Cache<Object> cache;

  @Before
  public void setup() throws Exception {
    cacheControlFilter = new CacheControlFilter();
    Dictionary<String, Object> properties = new Hashtable<String, Object>();
    properties.put(CacheControlFilter.SAKAI_CACHE_PATTERNS, new String[] {
        "root;.*(js|css)$;.expires:3456000;Cache-Control: max-age=43200 public;Vary:Accept-Encoding",
        "root;.*html$;.expires:3456000;Cache-Control: max-age-43200 public;Vary:Accept-Encoding", 
        "styles;.*\\.(ico|pdf|flv|jpg|jpeg|png|gif|js|css|swf)$;.expires:3456000;Cache-Control: max-age=43200 public;Vary:Accept-Encoding" 
        });
    properties.put(CacheControlFilter.SAKAI_CACHE_PATHS, new String[] { 
        "dev;.expires:3456000;Cache-Control: max-age=432000 public;Vary:Accept-Encoding", 
        "devwidgets;.expires:3456000;Cache-Control:max-age=432000 public;Vary:Accept-Encoding",
        "cacheable;.expires:3456000;.requestCache:3600;Cache-Control:max-age=432000 public;Vary:Accept-Encoding",
        "p;Cache-Control:no-cache" });
    when(componentContext.getProperties()).thenReturn(properties);
    cacheControlFilter.activate(componentContext);
    cacheControlFilter.init(filterConfig);
  }
  
  @After
  public void teardown() {
    cacheControlFilter.destroy();
  }

  @Test
  public void setsHeaderOnJavascriptRequests() throws Exception {
    verifyExpiresHeaderWithPath("GET", "/dev/config.js", true);
  }
  @Test
  public void setsNoHeaderOnRoot() throws Exception {
    verifyExpiresHeaderWithPath("GET", "/", false);
  }

  @Test
  public void setsHeaderOnRootRequests() throws Exception {
    verifyExpiresHeaderWithPath("GET", "/index.html", true);
  }

  @Test
  public void setsHeaderOnCssRequests() throws Exception {
    verifyExpiresHeaderWithPath("GET", "/styles/midnight/main.css", true);
  }

  @Test
  public void willNotSetHeaderOnTextFile() throws Exception {
    verifyExpiresHeaderWithPath("GET",
        "/~user/zach/Documents/What I Did Last Summer.txt", false);
  }

  @Test
  public void willNotFilterOnPost() throws Exception {
    verifyExpiresHeaderWithPath("POST", "/dev/config.js", false);
  }

  @Test
  public void checkRequestCaching() throws Exception {
    when(request.getMethod()).thenReturn("GET");
    when(request.getPathInfo()).thenReturn("/cacheable/config.json");
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ServletOutputStream servletOutputStream = new ServletOutputStream() {
      
      @Override
      public void write(int b) throws IOException {
        baos.write(b);
      }
    };
    when(response.getOutputStream()).thenReturn(servletOutputStream);

    when(cacheMangerService.getCache(CacheControlFilter.class.getName()+"-cache", CacheScope.INSTANCE)).thenReturn(cache);
    
    cacheControlFilter.cacheManagerService = cacheMangerService;

    cacheControlFilter.doFilter(request, response, new TFilter(true));

    verify(response, Mockito.atLeastOnce()).setHeader(anyString(), anyString());
    verify(cache).put(Mockito.eq("/cacheable/config.json?null"), Matchers.any(CachedResponse.class));
    
    
    
  }
  
  
  @Test
  public void checkRequestCachingReplay() throws Exception {
    when(request.getMethod()).thenReturn("GET");
    when(request.getPathInfo()).thenReturn("/cacheable/config.json");
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ServletOutputStream servletOutputStream = new ServletOutputStream() {
      
      @Override
      public void write(int b) throws IOException {
        baos.write(b);
      }
    };
    when(response.getOutputStream()).thenReturn(servletOutputStream);

    when(cacheMangerService.getCache(CacheControlFilter.class.getName()+"-cache", CacheScope.INSTANCE)).thenReturn(cache);
    CachedResponse cachedResponse  = populateResponseCapture(true);
    when(cache.get("/cacheable/config.json?null")).thenReturn(cachedResponse);
    
    cacheControlFilter.cacheManagerService = cacheMangerService;

    cacheControlFilter.doFilter(request, response, null);

    verify(response, Mockito.atLeastOnce()).setHeader(anyString(), anyString());
    
    
    
    
  }

  private CachedResponse populateResponseCapture(boolean useOutputStream) throws IOException {
    OperationResponseCapture sresponse = new OperationResponseCapture();
    sresponse.addDateHeader("Date", System.currentTimeMillis());
    sresponse.setDateHeader("Last-Modified", System.currentTimeMillis());
    sresponse.setCharacterEncoding("URF-8");
    sresponse.setContentLength(10);
    sresponse.setContentType("test/plain");
    sresponse.setHeader("Cache-Control", "max-age=3600");
    sresponse.setIntHeader("Age", 1000);
    sresponse.setLocale(new Locale("en","GB"));
    sresponse.setStatus(200);
    sresponse.setStatus(200, "Ok");
    sresponse.addHeader("Cache-Control", " public");
    sresponse.addIntHeader("Age", 101);
    if ( useOutputStream ) {
      ServletOutputStream baseStream = new ServletOutputStream() {
        
        @Override
        public void write(int b) throws IOException {
          // TODO Auto-generated method stub
          
        }
      };
      sresponse.getOutputStream(baseStream).write(new byte[1024]);
    } else {
      StringWriter writer = new StringWriter();
      sresponse.getWriter(new PrintWriter(writer)).write("ABCDEF");        
    }  
    return new CachedResponse(sresponse, 30);
  }

  @Test
  public void checkRequestCachingWriter() throws Exception {
    when(request.getMethod()).thenReturn("GET");
    when(request.getPathInfo()).thenReturn("/cacheable/config.json");
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(printWriter);
    when(cacheMangerService.getCache(CacheControlFilter.class.getName()+"-cache", CacheScope.INSTANCE)).thenReturn(cache);

    cacheControlFilter.cacheManagerService = cacheMangerService;
    cacheControlFilter.doFilter(request, response, new TFilter(false));

    verify(response, Mockito.atLeastOnce()).setHeader(anyString(), anyString());
    
    verify(cache).put(Mockito.eq("/cacheable/config.json?null"), Matchers.any(CachedResponse.class));
  }


  private void verifyExpiresHeaderWithPath(String method, String path,
      boolean expectHeader) throws ServletException, IOException {
    when(request.getMethod()).thenReturn(method);
    when(request.getPathInfo()).thenReturn(path);
    
    
    cacheControlFilter.doFilter(request, response, chain);

    if (expectHeader) {
      verify(response, Mockito.atLeastOnce()).setHeader(anyString(), anyString());
    } else {
      verify(response, never()).addHeader(anyString(), anyString());
    }
  }

}
