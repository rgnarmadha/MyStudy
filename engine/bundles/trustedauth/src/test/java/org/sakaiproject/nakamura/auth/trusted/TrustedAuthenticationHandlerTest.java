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

import static org.apache.sling.jcr.resource.JcrResourceConstants.AUTHENTICATION_INFO_CREDENTIALS;
import static org.junit.Assert.assertFalse;

import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.auth.trusted.TrustedTokenService;
import org.sakaiproject.nakamura.api.cluster.ClusterTrackingService;
import org.sakaiproject.nakamura.api.memory.Cache;
import org.sakaiproject.nakamura.api.memory.CacheManagerService;
import org.sakaiproject.nakamura.api.memory.CacheScope;
import org.sakaiproject.nakamura.auth.trusted.TrustedAuthenticationHandler.TrustedAuthentication;
import org.sakaiproject.nakamura.auth.trusted.TrustedTokenServiceImpl.TrustedUser;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


/**
 *
 */
public class TrustedAuthenticationHandlerTest {
  private TrustedTokenServiceImpl trustedTokenService;
  private List<Object> mocks = new ArrayList<Object>();

  @Before
  public void before() throws NoSuchAlgorithmException, InvalidKeyException, IllegalStateException, UnsupportedEncodingException {
    mocks.clear();
    trustedTokenService = new TrustedTokenServiceImpl();

    ClusterTrackingService clusterTrackingService = createMock(ClusterTrackingService.class);
    CacheManagerService cacheManagerService = createMock(CacheManagerService.class);
    EventAdmin eventAdmin = createMock(EventAdmin.class);

    Cache<Object> cache = new LocalCache<Object>();
    EasyMock.expect(cacheManagerService.getCache(TokenStore.class.getName(), CacheScope.CLUSTERREPLICATED)).andReturn(cache).anyTimes();
    EasyMock.expect(clusterTrackingService.getCurrentServerId()).andReturn("serverID").anyTimes();
    eventAdmin.sendEvent((Event) EasyMock.anyObject());
    EasyMock.expectLastCall().anyTimes();
    eventAdmin.postEvent((Event) EasyMock.anyObject());
    EasyMock.expectLastCall().anyTimes();
    trustedTokenService.clusterTrackingService = clusterTrackingService;
    trustedTokenService.cacheManager = cacheManagerService;
    trustedTokenService.eventAdmin = eventAdmin;
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
    EasyMock.expect(context.getProperties()).andReturn(dict);
    return context;
  }

  @Test
  public void testRequestCredentials() throws Exception {
    TrustedAuthenticationHandler handler = new TrustedAuthenticationHandler();
    assertFalse(handler.requestCredentials(null, null));
  }

  @Test
  public void testNewRequest() throws IOException {
    // create some credentials
    ComponentContext context = configureForSession();
    HttpServletRequest request = createMock(HttpServletRequest.class);
    HttpSession session = createMock(HttpSession.class);
    EasyMock.expect(request.getSession(true)).andReturn(session);
    EasyMock.expect(request.getRemoteAddr()).andReturn("192.168.0.200"); // not trusted.

    Principal principal = createMock(Principal.class);
    EasyMock.expect(request.getUserPrincipal()).andReturn(principal);
    EasyMock.expect(principal.getName()).andReturn(null);
    EasyMock.expect(request.getRemoteUser()).andReturn("ieb");
    Capture<SimpleCredentials> attributeValue = new Capture<SimpleCredentials>();
    Capture<String> attributeName = new Capture<String>();
    session.setAttribute(EasyMock.capture(attributeName), EasyMock.capture(attributeValue));


    HttpServletResponse response = createMock(HttpServletResponse.class);

    replay();
    trustedTokenService.activate(context);
    trustedTokenService.injectToken(request, response);
    Assert.assertTrue(attributeName.hasCaptured());
    Assert.assertTrue(attributeValue.hasCaptured());
    Credentials credentials = attributeValue.getValue();

    verify();
    reset();
    EasyMock.expect(request.getHeader("x-sakai-token")).andReturn(null).anyTimes();

    //nothing in the request
    EasyMock.expect(request.getAttribute(TrustedAuthenticationHandler.RA_AUTHENTICATION_TRUST)).andReturn(null);
    // next look for session
    EasyMock.expect(request.getSession(false)).andReturn(session);
    EasyMock.expect(session.getAttribute(TrustedTokenService.SA_AUTHENTICATION_CREDENTIALS)).andReturn(credentials);
    Capture<String> name1 = new Capture<String>();
    Capture<TrustedAuthentication> object1 = new Capture<TrustedAuthentication>();
    request.setAttribute(EasyMock.capture(name1),EasyMock.capture(object1));
    EasyMock.expectLastCall();
    Capture<String> name2 = new Capture<String>();
    Capture<AuthenticationInfo> object2 = new Capture<AuthenticationInfo>();
    request.setAttribute(EasyMock.capture(name2),EasyMock.capture(object2));
    EasyMock.expectLastCall();

    // dropping the credentials
    EasyMock.expect(request.getSession(false)).andReturn(session);
    session.setAttribute(TrustedTokenService.SA_AUTHENTICATION_CREDENTIALS, null);
    EasyMock.expectLastCall();
    request.setAttribute(TrustedAuthenticationHandler.RA_AUTHENTICATION_INFO, null);
    EasyMock.expectLastCall();
    request.setAttribute(TrustedAuthenticationHandler.RA_AUTHENTICATION_TRUST, null);
    EasyMock.expectLastCall();


    // this time make it invalid
    EasyMock.expect(request.getAttribute(TrustedAuthenticationHandler.RA_AUTHENTICATION_TRUST)).andReturn(null);
    EasyMock.expect(request.getSession(false)).andReturn(session);
    EasyMock.expect(session.getAttribute(TrustedTokenService.SA_AUTHENTICATION_CREDENTIALS)).andReturn(null);

    // and this time with some Auth

    replay();
    TrustedAuthenticationHandler trustedAuthenticationHandler = new TrustedAuthenticationHandler();
    trustedAuthenticationHandler.trustedTokenService = trustedTokenService;

    AuthenticationInfo info = trustedAuthenticationHandler.extractCredentials(request, response);
    Assert.assertNotNull(info);
    Credentials authCredentials = (Credentials)info.get(AUTHENTICATION_INFO_CREDENTIALS);
    Assert.assertTrue(authCredentials instanceof SimpleCredentials);
    SimpleCredentials simpleCredentials = (SimpleCredentials) authCredentials;
    Object o = simpleCredentials.getAttribute(TrustedTokenService.CA_AUTHENTICATION_USER);
    Assert.assertTrue(o instanceof TrustedUser);
    TrustedUser tu = (TrustedUser) o;
    Assert.assertEquals("ieb", tu.getUser());
    Assert.assertTrue(name1.hasCaptured());
    Assert.assertTrue(name2.hasCaptured());
    Assert.assertTrue(object1.hasCaptured());
    Assert.assertTrue(object2.hasCaptured());
    Assert.assertEquals(TrustedAuthenticationHandler.RA_AUTHENTICATION_TRUST, name1.getValue());
    Assert.assertEquals(TrustedAuthenticationHandler.RA_AUTHENTICATION_INFO, name2.getValue());
    Assert.assertEquals(info, object2.getValue());
    Assert.assertNotNull(object1.getValue());

    TrustedAuthentication ta = object1.getValue();

    trustedAuthenticationHandler.dropCredentials(request, response);

    // nothing to find in the request or sesion so we should get a null response
    AuthenticationInfo info2 = trustedAuthenticationHandler.extractCredentials(request, response);

    Assert.assertNull(info2);
    verify();
    reset();

    EasyMock.expect(request.getAttribute(TrustedAuthenticationHandler.RA_AUTHENTICATION_TRUST)).andReturn(ta);
    EasyMock.expect(request.getAttribute(TrustedAuthenticationHandler.RA_AUTHENTICATION_INFO)).andReturn(info);

    // this time extract credentials from the request attributes as listed above
    replay();

    trustedAuthenticationHandler.extractCredentials(request, response);
    Assert.assertNotNull(info);
    authCredentials = (Credentials)info.get(AUTHENTICATION_INFO_CREDENTIALS);
    Assert.assertTrue(authCredentials instanceof SimpleCredentials);
    simpleCredentials = (SimpleCredentials) authCredentials;
    o = simpleCredentials.getAttribute(TrustedTokenService.CA_AUTHENTICATION_USER);
    Assert.assertTrue(o instanceof TrustedUser);
    tu = (TrustedUser) o;
    Assert.assertEquals("ieb", tu.getUser());
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
