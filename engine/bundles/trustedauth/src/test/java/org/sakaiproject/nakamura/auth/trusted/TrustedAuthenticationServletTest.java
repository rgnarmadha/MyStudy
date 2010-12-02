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
package org.sakaiproject.nakamura.auth.trusted;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.NamespaceException;
import org.sakaiproject.nakamura.api.cluster.ClusterTrackingService;
import org.sakaiproject.nakamura.api.memory.Cache;
import org.sakaiproject.nakamura.api.memory.CacheManagerService;
import org.sakaiproject.nakamura.api.memory.CacheScope;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class TrustedAuthenticationServletTest {

  private List<Object> mocks = new ArrayList<Object>();
  private TrustedTokenServiceImpl trustedTokenService;

  @Before
  public void before() throws NoSuchAlgorithmException, InvalidKeyException, IllegalStateException, UnsupportedEncodingException {
    mocks.clear();
    trustedTokenService = new TrustedTokenServiceImpl();

    ClusterTrackingService clusterTrackingService = createMock(ClusterTrackingService.class);
    CacheManagerService cacheManagerService = createMock(CacheManagerService.class);

    Cache<Object> cache = new LocalCache<Object>();
    EasyMock.expect(cacheManagerService.getCache(TokenStore.class.getName(), CacheScope.CLUSTERREPLICATED)).andReturn(cache).anyTimes();
    EasyMock.expect(clusterTrackingService.getCurrentServerId()).andReturn("serverID").anyTimes();
    trustedTokenService.clusterTrackingService = clusterTrackingService;
    trustedTokenService.cacheManager = cacheManagerService;

  }
  public ComponentContext configureForSession() {
    ComponentContext context = createMock(ComponentContext.class);
    Hashtable<String, Object> dict = new Hashtable<String, Object>();
    dict.put(TrustedTokenServiceImpl.USE_SESSION, true);
    dict.put(TrustedTokenServiceImpl.COOKIE_NAME, "secure-cookie");
    dict.put(TrustedTokenServiceImpl.TTL, 1200000L);
    dict.put(TrustedTokenServiceImpl.SECURE_COOKIE, false);
    dict.put(TrustedTokenServiceImpl.TOKEN_FILE_NAME, "target/cookie-token.bin");
    dict.put(TrustedTokenServiceImpl.SERVER_TOKEN_ENABLED, false);
    dict.put(TrustedTokenServiceImpl.SERVER_TOKEN_SAFE_HOSTS_ADDR, "127.0.0.1");
    dict.put(TrustedTokenServiceImpl.SERVER_TOKEN_SHARED_SECRET, "not-so-secret" );
    dict.put(TrustedTokenServiceImpl.DEBUG_COOKIES, false );
    EasyMock.expect(context.getProperties()).andReturn(dict).anyTimes();
    return context;
  }


  @Test
  public void testDoGet() throws ServletException, IOException, NamespaceException, InvalidKeyException, NoSuchAlgorithmException, IllegalStateException {

    ComponentContext context = configureForSession();
    HttpServletRequest request = createMock(HttpServletRequest.class);
    HttpServletResponse response = createMock(HttpServletResponse.class);
    WebContainer webContainer = createMock(WebContainer.class);

    TrustedAuthenticationServlet trustedAuthenticationServlet = new TrustedAuthenticationServlet();
    webContainer.registerServlet(null, trustedAuthenticationServlet, null, trustedAuthenticationServlet);
    EasyMock.expectLastCall();

    EasyMock.expect(request.getRemoteAddr()).andReturn("192.168.0.123");
    EasyMock.expect(request.getUserPrincipal()).andReturn(null);
    EasyMock.expect(request.getRemoteUser()).andReturn(null);
    EasyMock.expect(request.getParameter("d")).andReturn("/test");
    response.sendRedirect("/test");
    EasyMock.expectLastCall();

    replay();
    trustedAuthenticationServlet.trustedTokenService = trustedTokenService;
    trustedAuthenticationServlet.webContainer = webContainer;
    trustedTokenService.activate(context);
    trustedAuthenticationServlet.activate(context);

    trustedAuthenticationServlet.doGet(request, response);


    verify();
  }


  public <T> T createMock(Class<T> mockClass) {
    T m = EasyMock.createMock(mockClass);
    mocks.add(m);
    return m;
  }

  public void replay() {
    EasyMock.replay(mocks.toArray());
  }

  public void verify() {
    EasyMock.verify(mocks.toArray());
  }

  public void reset() {
    EasyMock.reset(mocks.toArray());
  }

}
