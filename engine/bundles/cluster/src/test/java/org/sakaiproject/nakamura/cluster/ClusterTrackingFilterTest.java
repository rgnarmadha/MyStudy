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
import static org.junit.Assert.assertTrue;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.easymock.Capture;
import org.easymock.EasyMock;
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
import javax.servlet.FilterChain;
import javax.servlet.http.Cookie;


/**
 *
 */
public class ClusterTrackingFilterTest extends AbstractEasyMockTest {
  private ClusterTrackingServiceImpl clusterTrackingServiceImpl;
  private CacheManagerService cacheManagerService;
  private Cache<Object> userTrackingCache;
  private Cache<Object> serverTrackingCache;
  private String serverId;
  private Capture<String> serverIdCapture;
  private Capture<ClusterServerImpl> clusterServerCapture;
  private ClusterTrackingFilter clusterTrackingFilter;
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

    clusterTrackingFilter = new ClusterTrackingFilter();
    clusterTrackingFilter.clusterTrackingService = clusterTrackingServiceImpl;
    
    componentContext = createMock(ComponentContext.class);
    Hashtable<String, Object> dict = new Hashtable<String, Object>();
    dict.put(ClusterTrackingServiceImpl.PROP_SECURE_HOST_URL, "http://localhost:8081");
    expect(componentContext.getProperties()).andReturn(dict).anyTimes();

  }

  @After
  public void after() {
  }

  @Test
  public void testActivateDeactivate() throws Exception {
    activate();





    deactivate();

    replay();
    clusterTrackingServiceImpl.activate(componentContext);


    clusterTrackingServiceImpl.deactivate(componentContext);

    checkActivation();
    verify();
  }

  @Test
  public void testNormalGet() throws Exception {
    activate();
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    FilterChain chain = createMock(FilterChain.class);
    
    Cookie[] cookies = null;
    expect(request.getCookies()).andReturn(cookies);
    expect(request.getRemoteUser()).andReturn("ieb");
    expect(response.isCommitted()).andReturn(false);
    response.addCookie((Cookie) EasyMock.anyObject());
    expectLastCall();
    response.addHeader("Cache-Control", "no-cache=\"set-cookie\" ");
    expectLastCall();
    response.addDateHeader("Expires", 0);
    expectLastCall();

    chain.doFilter(request, response);
    expectLastCall();

    deactivate();

    replay();
    clusterTrackingServiceImpl.activate(componentContext);

    
    clusterTrackingFilter.doFilter(request, response, chain);

    clusterTrackingServiceImpl.deactivate(componentContext);

    checkActivation();
    verify();
  }


  /**
   *
   */
  private void checkActivation() {
    assertTrue(serverIdCapture.hasCaptured());
    assertEquals(serverId, serverIdCapture.getValue());
    assertTrue(clusterServerCapture.hasCaptured());
    ClusterServerImpl clusterServerImpl = clusterServerCapture.getValue();
    assertEquals(serverId, clusterServerImpl.getServerId());
    assertTrue(System.currentTimeMillis() >= clusterServerImpl.getLastModified());
  }

  /**
   * @throws ReflectionException
   * @throws MBeanException
   * @throws NullPointerException
   * @throws InstanceNotFoundException
   * @throws AttributeNotFoundException
   * @throws MalformedObjectNameException
   *
   */
  private void activate() throws Exception {
    serverId = getServerId();
    serverIdCapture = new Capture<String>();
    clusterServerCapture = new Capture<ClusterServerImpl>();
    expect(serverTrackingCache.list()).andReturn(new ArrayList<Object>()).times(2);
    expect(
        serverTrackingCache.put(capture(serverIdCapture), capture(clusterServerCapture)))
        .andReturn(new Object());
  }

  /**
   *
   */
  private void deactivate() {
    serverTrackingCache.remove(serverId);
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
  private String getServerId() throws Exception {
    MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    ObjectName name = new ObjectName("java.lang:type=Runtime");
    return ((String) mbeanServer.getAttribute(name, "Name")).replace('@', '-');
  }

}
