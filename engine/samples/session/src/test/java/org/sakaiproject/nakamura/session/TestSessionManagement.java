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
package org.sakaiproject.nakamura.session;

import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

import org.easymock.Capture;
import org.junit.Test;
import org.sakaiproject.nakamura.api.memory.Cache;
import org.sakaiproject.nakamura.api.memory.CacheManagerService;
import org.sakaiproject.nakamura.api.memory.CacheScope;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class TestSessionManagement {

  @Test
  @SuppressWarnings("unchecked")
  public void testSessionCachingHandlesNull() {
    CacheManagerService cacheService = createMock(CacheManagerService.class);
    Cache<Object> cache = createMock(Cache.class);
    SessionManagerServiceImpl service = new SessionManagerServiceImpl();
    service.bindCacheManagerService(cacheService);

    Capture<String> requestKey = new Capture<String>();
    Capture<String> requestCache = new Capture<String>();
    expect(
        cacheService
            .getCache(and(isA(String.class), capture(requestCache)), eq(CacheScope.REQUEST)))
        .andReturn(cache);
    expect(cache.get(and(isA(String.class), capture(requestKey)))).andReturn(null);

    replay(cache, cacheService);
    assertNull("Expected request to be null", service.getCurrentRequest());
    verify(cache, cacheService);
    reset(cache, cacheService);

    expect(cacheService.getCache(eq(requestCache.getValue()), eq(CacheScope.REQUEST))).andReturn(
        cache);
    expectLastCall().anyTimes();
    expect(cache.get(eq(requestKey.getValue()))).andReturn(null);
    expectLastCall().anyTimes();

    replay(cache, cacheService);
    assertNull("Expected session to be null", service.getCurrentSession());
    assertNull("Expected user to be null", service.getCurrentUserId());
    verify(cache, cacheService);
    service.unbindCacheManagerService(cacheService);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testSessionCachingCachesSession() {
    CacheManagerService cacheService = createMock(CacheManagerService.class);
    Cache<Object> cache = createMock(Cache.class);
    SessionManagerServiceImpl service = new SessionManagerServiceImpl();
    service.bindCacheManagerService(cacheService);

    Capture<String> requestKey = new Capture<String>();
    Capture<String> requestCache = new Capture<String>();
    expect(
        cacheService
            .getCache(and(isA(String.class), capture(requestCache)), eq(CacheScope.REQUEST)))
        .andReturn(cache);
    expect(cache.get(and(isA(String.class), capture(requestKey)))).andReturn(null);

    replay(cache, cacheService);
    assertNull("Expected request to be null", service.getCurrentRequest());
    verify(cache, cacheService);
    reset(cache, cacheService);

    HttpServletRequest currentRequest = createMock(HttpServletRequest.class);
    HttpSession currentSession = createMock(HttpSession.class);
    String testUser = "bob";

    expect(cacheService.getCache(eq(requestCache.getValue()), eq(CacheScope.REQUEST))).andReturn(
        cache);
    expectLastCall().anyTimes();
    expect(cache.put(eq(requestKey.getValue()), eq(currentRequest))).andReturn(currentRequest);
    expect(cache.get(eq(requestKey.getValue()))).andReturn(currentRequest);
    expectLastCall().anyTimes();
    expect(currentRequest.getSession()).andReturn(currentSession);
    expectLastCall().anyTimes();
    expect(currentRequest.getRemoteUser()).andReturn(testUser);
    expectLastCall().anyTimes();

    replay(cache, cacheService, currentRequest, currentSession);

    service.bindRequest(currentRequest);
    assertEquals("Expected request to be set", currentRequest, service.getCurrentRequest());
    assertEquals("Expected session to be set", currentSession, service.getCurrentSession());
    assertEquals("Expected user to be set", testUser, service.getCurrentUserId());
    verify(cache, cacheService, currentRequest, currentSession);
    service.unbindCacheManagerService(cacheService);
  }

}
