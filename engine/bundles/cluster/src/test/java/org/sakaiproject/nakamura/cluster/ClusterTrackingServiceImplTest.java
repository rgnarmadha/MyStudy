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
package org.sakaiproject.nakamura.cluster;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.easymock.Capture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.memory.Cache;
import org.sakaiproject.nakamura.api.memory.CacheManagerService;
import org.sakaiproject.nakamura.api.memory.CacheScope;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class ClusterTrackingServiceImplTest extends AbstractEasyMockTest {

  private ClusterTrackingServiceImpl clusterTrackingServiceImpl;
  private CacheManagerService cacheManagerService;
  private Cache<Object> userTrackingCache;
  private Cache<Object> serverTrackingCache;
  private ComponentContext componentContext;

  @SuppressWarnings("unchecked")
  @Before
  public void before() {
    cacheManagerService = createMock(CacheManagerService.class);
    userTrackingCache = createMock(Cache.class);
    serverTrackingCache = createMock(Cache.class);
    expect(
        cacheManagerService.getCache("user-tracking-cache", CacheScope.INSTANCE))
        .andReturn(userTrackingCache).anyTimes();
    expect(
        cacheManagerService.getCache("server-tracking-cache",
            CacheScope.CLUSTERREPLICATED)).andReturn(serverTrackingCache).anyTimes();
    clusterTrackingServiceImpl = new ClusterTrackingServiceImpl(cacheManagerService);
    componentContext = createMock(ComponentContext.class);
    Hashtable<String, Object> dict = new Hashtable<String, Object>();
    dict.put(ClusterTrackingServiceImpl.PROP_SECURE_HOST_URL, "http://localhost:8081");
    expect(componentContext.getProperties()).andReturn(dict).anyTimes();
  }

  @After
  public void after() {
  }

  @Test
  public void testGetServerId() throws Exception {
    ;
    assertNotNull(getServerId());
  }

  /**
   * @return
   * @throws NullPointerException
   * @throws MalformedObjectNameException
   * @throws ReflectionException
   * @throws MBeanException
   * @throws InstanceNotFoundException
   * @throws AttributeNotFoundException
   */
  private String getServerId() throws MalformedObjectNameException, NullPointerException,
      AttributeNotFoundException, InstanceNotFoundException, MBeanException,
      ReflectionException {
    MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    ObjectName name = new ObjectName("java.lang:type=Runtime");
    return ((String) mbeanServer.getAttribute(name, "Name")).replace('@', '-');
  }

  @Test
  public void testActivate() throws Exception {

    // activate
    String serverId = getServerId();
    Capture<String> serverIdCapture = new Capture<String>();
    Capture<ClusterServerImpl> clusterServerCapture = new Capture<ClusterServerImpl>();

    expect(serverTrackingCache.list()).andReturn(new ArrayList<Object>()).times(2);
    expect(
        serverTrackingCache.put(capture(serverIdCapture), capture(clusterServerCapture)))
        .andReturn(new Object());


    replay();
    clusterTrackingServiceImpl.activate(componentContext);
    assertTrue(serverIdCapture.hasCaptured());
    assertEquals(serverId, serverIdCapture.getValue());
    assertTrue(clusterServerCapture.hasCaptured());
    ClusterServerImpl clusterServerImpl = clusterServerCapture.getValue();
    assertEquals(serverId, clusterServerImpl.getServerId());
    assertTrue(System.currentTimeMillis() >= clusterServerImpl.getLastModified());
    verify();
  }

  @Test
  public void testActivateDeactivate() throws Exception {
    // activate
    String serverId = getServerId();
    Capture<String> serverIdCapture = new Capture<String>();
    Capture<ClusterServerImpl> clusterServerCapture = new Capture<ClusterServerImpl>();
    expect(serverTrackingCache.list()).andReturn(new ArrayList<Object>()).times(2);
    expect(
        serverTrackingCache.put(capture(serverIdCapture), capture(clusterServerCapture)))
        .andReturn(new Object());

    // deactivate
    serverTrackingCache.remove(serverId);

    replay();
    clusterTrackingServiceImpl.activate(componentContext);
    clusterTrackingServiceImpl.deactivate(componentContext);
    assertTrue(serverIdCapture.hasCaptured());
    assertEquals(serverId, serverIdCapture.getValue());
    assertTrue(clusterServerCapture.hasCaptured());
    ClusterServerImpl clusterServerImpl = clusterServerCapture.getValue();
    assertEquals(serverId, clusterServerImpl.getServerId());
    assertTrue(System.currentTimeMillis() >= clusterServerImpl.getLastModified());
    verify();
  }


  @Test
  public void testTrackClusterUser() throws Exception {
    // activate
    String serverId = getServerId();
    Capture<String> serverIdCapture = new Capture<String>();
    Capture<ClusterServerImpl> clusterServerCapture = new Capture<ClusterServerImpl>();
    expect(serverTrackingCache.list()).andReturn(new ArrayList<Object>()).times(2);
    expect(
        serverTrackingCache.put(capture(serverIdCapture), capture(clusterServerCapture)))
        .andReturn(new Object());



    // trackClusterUser
    HttpServletRequest request = createMock(HttpServletRequest.class);
    HttpServletResponse response = createMock(HttpServletResponse.class);

    Cookie cookieA = new Cookie("something","someValue");
    Cookie cookieMatching = new Cookie("SAKAI-TRACKING",serverId+"-trackingValue");
    Cookie cookieB = new Cookie("somethingElse","someOtherValue");

    Cookie[] cookies = new Cookie[] {
        cookieA,
        cookieMatching,
        cookieB
    };


    expect(request.getCookies()).andReturn(cookies);
    expect(request.getRemoteUser()).andReturn("userid");

    ClusterServerImpl csImple = new ClusterServerImpl(serverId, 4, "http://sdfsdfs");

    expect(serverTrackingCache.get(serverId)).andReturn(csImple).times(2);
    // nothing in the cache.
    expect(userTrackingCache.get(serverId+"-trackingValue")).andReturn(null);
    Capture<String> trackingValueCapture = new Capture<String>();
    Capture<ClusterUserImpl> clusterUserCapture = new Capture<ClusterUserImpl>();
    expect(userTrackingCache.put(capture(trackingValueCapture), capture(clusterUserCapture))).andReturn(new Object());


    // deactivate
    serverTrackingCache.remove(serverId);

    replay();
    clusterTrackingServiceImpl.activate(componentContext);

    clusterTrackingServiceImpl.trackClusterUser(request, response);

    clusterTrackingServiceImpl.deactivate(componentContext);
    assertTrue(serverIdCapture.hasCaptured());
    assertEquals(serverId, serverIdCapture.getValue());
    assertTrue(clusterServerCapture.hasCaptured());
    ClusterServerImpl clusterServerImpl = clusterServerCapture.getValue();
    assertEquals(serverId, clusterServerImpl.getServerId());
    assertTrue(System.currentTimeMillis() >= clusterServerImpl.getLastModified());

    // check the user capture
    assertTrue(trackingValueCapture.hasCaptured());
    assertTrue(clusterUserCapture.hasCaptured());
    assertEquals(serverId+"-trackingValue",trackingValueCapture.getValue());
    ClusterUserImpl clusterUserImpl = clusterUserCapture.getValue();
    assertEquals("userid",clusterUserImpl.getUser() );
    assertEquals(serverId, clusterUserImpl.getServerId());
    assertTrue(System.currentTimeMillis() >=clusterUserImpl.getLastModified());
    verify();
  }


  @Test
  public void testTrackClusterNewUser() throws Exception {
    // activate
    String serverId = getServerId();
    Capture<String> serverIdCapture = new Capture<String>();
    Capture<ClusterServerImpl> clusterServerCapture = new Capture<ClusterServerImpl>();
    expect(serverTrackingCache.list()).andReturn(new ArrayList<Object>()).times(2);
    expect(
        serverTrackingCache.put(capture(serverIdCapture), capture(clusterServerCapture)))
        .andReturn(new Object());



    // trackClusterUser
    HttpServletRequest request = createMock(HttpServletRequest.class);
    HttpServletResponse response = createMock(HttpServletResponse.class);

    Cookie cookieA = new Cookie("something","someValue");
    Cookie cookieB = new Cookie("somethingElse","someOtherValue");

    Cookie[] cookies = new Cookie[] {
        cookieA,
        cookieB
    };


    expect(request.getCookies()).andReturn(cookies);
    expect(request.getRemoteUser()).andReturn("userid");

    expect(response.isCommitted()).andReturn(false);
    Capture<Cookie> captureCookie = new Capture<Cookie>();
    response.addCookie(capture(captureCookie));
    expectLastCall();
    
    response.addHeader("Cache-Control", "no-cache=\"set-cookie\" ");
    expectLastCall();
    response.addDateHeader("Expires", 0);
    expectLastCall();



    // deactivate
    serverTrackingCache.remove(serverId);

    replay();
    clusterTrackingServiceImpl.activate(componentContext);

    clusterTrackingServiceImpl.trackClusterUser(request, response);

    clusterTrackingServiceImpl.deactivate(componentContext);
    assertTrue(serverIdCapture.hasCaptured());
    assertEquals(serverId, serverIdCapture.getValue());
    assertTrue(clusterServerCapture.hasCaptured());
    ClusterServerImpl clusterServerImpl = clusterServerCapture.getValue();
    assertEquals(serverId, clusterServerImpl.getServerId());
    assertTrue(System.currentTimeMillis() >= clusterServerImpl.getLastModified());

    // check the cookie
    assertTrue(captureCookie.hasCaptured());
    Cookie cookie = captureCookie.getValue();
    assertEquals("SAKAI-TRACKING", cookie.getName());
    assertEquals("/", cookie.getPath());
    assertEquals(-1, cookie.getMaxAge());
    assertNotNull(cookie.getValue());
    assertTrue(cookie.getValue().startsWith(serverId));
    verify();
  }



  @Test
  public void testGetUser() throws Exception {
    // activate
    String serverId = getServerId();
    Capture<String> serverIdCapture = new Capture<String>();
    Capture<ClusterServerImpl> clusterServerCapture = new Capture<ClusterServerImpl>();
    expect(serverTrackingCache.list()).andReturn(new ArrayList<Object>()).times(2);
    expect(
        serverTrackingCache.put(capture(serverIdCapture), capture(clusterServerCapture)))
        .andReturn(new Object());

    ClusterUserImpl clusterUserImpl = new ClusterUserImpl("remoteUser", "serverId");
    expect(userTrackingCache.get("testCookieValue")).andReturn(clusterUserImpl);

    // deactivate
    serverTrackingCache.remove(serverId);

    replay();
    clusterTrackingServiceImpl.activate(componentContext);
    clusterTrackingServiceImpl.getUser("testCookieValue");
    clusterTrackingServiceImpl.deactivate(componentContext);
    assertTrue(serverIdCapture.hasCaptured());
    assertEquals(serverId, serverIdCapture.getValue());
    assertTrue(clusterServerCapture.hasCaptured());
    ClusterServerImpl clusterServerImpl = clusterServerCapture.getValue();
    assertEquals(serverId, clusterServerImpl.getServerId());
    assertTrue(System.currentTimeMillis() >= clusterServerImpl.getLastModified());
    verify();
  }

  @Test
  public void testGetUserNone() throws Exception {
    // activate
    String serverId = getServerId();
    Capture<String> serverIdCapture = new Capture<String>();
    Capture<ClusterServerImpl> clusterServerCapture = new Capture<ClusterServerImpl>();
    expect(serverTrackingCache.list()).andReturn(new ArrayList<Object>()).times(2);
    expect(
        serverTrackingCache.put(capture(serverIdCapture), capture(clusterServerCapture)))
        .andReturn(new Object());

    expect(userTrackingCache.get("testCookieValue")).andReturn(null);

    // deactivate
    serverTrackingCache.remove(serverId);

    replay();
    clusterTrackingServiceImpl.activate(componentContext);
    clusterTrackingServiceImpl.getUser("testCookieValue");
    clusterTrackingServiceImpl.deactivate(componentContext);
    assertTrue(serverIdCapture.hasCaptured());
    assertEquals(serverId, serverIdCapture.getValue());
    assertTrue(clusterServerCapture.hasCaptured());
    ClusterServerImpl clusterServerImpl = clusterServerCapture.getValue();
    assertEquals(serverId, clusterServerImpl.getServerId());
    assertTrue(System.currentTimeMillis() >= clusterServerImpl.getLastModified());
    verify();
  }

  @Test
  public void testUniqueId() throws Exception {
    // activate
    String serverId = getServerId();
    Capture<String> serverIdCapture = new Capture<String>();
    Capture<ClusterServerImpl> clusterServerCapture = new Capture<ClusterServerImpl>();
    expect(serverTrackingCache.list()).andReturn(new ArrayList<Object>()).times(2);
    expect(
        serverTrackingCache.put(capture(serverIdCapture), capture(clusterServerCapture)))
        .andReturn(new Object());

    // deactivate
    serverTrackingCache.remove(serverId);

    replay();
    clusterTrackingServiceImpl.activate(componentContext);
    assertNotNull(clusterTrackingServiceImpl.getClusterUniqueId());
    clusterTrackingServiceImpl.deactivate(componentContext);
    assertTrue(serverIdCapture.hasCaptured());
    assertEquals(serverId, serverIdCapture.getValue());
    assertTrue(clusterServerCapture.hasCaptured());
    ClusterServerImpl clusterServerImpl = clusterServerCapture.getValue();
    assertEquals(serverId, clusterServerImpl.getServerId());
    assertTrue(System.currentTimeMillis() >= clusterServerImpl.getLastModified());
    verify();

  }


}
