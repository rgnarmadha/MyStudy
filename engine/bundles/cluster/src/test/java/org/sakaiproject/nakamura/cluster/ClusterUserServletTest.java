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

import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.easymock.Capture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.memory.Cache;
import org.sakaiproject.nakamura.api.memory.CacheManagerService;
import org.sakaiproject.nakamura.api.memory.CacheScope;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Value;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;


/**
 *
 */
public class ClusterUserServletTest extends AbstractEasyMockTest {
  private ClusterTrackingServiceImpl clusterTrackingServiceImpl;
  private CacheManagerService cacheManagerService;
  private Cache<Object> userTrackingCache;
  private Cache<Object> serverTrackingCache;
  private String serverId;
  private Capture<String> serverIdCapture;
  private Capture<ClusterServerImpl> clusterServerCapture;
  private ClusterUserServlet clusterUserServlet;
  private UserManager userManager;
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

    userManager = createMock(UserManager.class);
    clusterUserServlet = new ClusterUserServlet(clusterTrackingServiceImpl,userManager);

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

  @SuppressWarnings("unchecked")
  @Test
  public void testNormalGet() throws Exception {
    activate();
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    Resource resource = createMock(Resource.class);
    Node node = createMock(Node.class);


    Value valueA = createMock(Value.class);
    Value valueB = createMock(Value.class);
    Value valueC = createMock(Value.class);

    Value[] values = new Value[]{
        valueA,
        valueB,
        valueC
    };

    ClusterUserImpl clusterUser = new ClusterUserImpl("ieb", "otherServerId");

    User user = createMock(User.class);
    Principal principal = createMock(Principal.class);

    List<String> propertyNames = new ArrayList<String>();
    propertyNames.add("prop1");
    propertyNames.add("prop2");
    propertyNames.add("prop3");

    Iterator<Group> iterator = createMock(Iterator.class);

    Group group = createMock(Group.class);


    expect(request.getResource()).andReturn(resource);
    expect(resource.adaptTo(Node.class)).andReturn(node);

    // request validated, processing request.
    expect(request.getParameter("c")).andReturn("some-tracking-cookie");
    expect(userTrackingCache.get("some-tracking-cookie")).andReturn(clusterUser);

    expect(userManager.getAuthorizable("ieb")).andReturn(user);
    StringWriter writer = new StringWriter();
    expect(response.getWriter()).andReturn(new PrintWriter(writer));

    expect(user.getID()).andReturn("ieb");
    expect(user.getPrincipal()).andReturn(principal);
    expect(principal.getName()).andReturn("principal:ieb");
    expect(user.getPropertyNames()).andReturn(propertyNames.iterator());
    expect(user.getProperty("prop1")).andReturn(values);
    expect(valueA.getString()).andReturn("tokenA");
    expect(valueB.getString()).andReturn("tokenB");
    expect(valueC.getString()).andReturn("tokenC");
    expect(user.getProperty("prop2")).andReturn(new Value[]{valueA});
    expect(valueA.getString()).andReturn("tokenA");
    expect(user.getProperty("prop3")).andReturn(new Value[]{});


    expect(user.declaredMemberOf()).andReturn(iterator);
    expect(iterator.hasNext()).andReturn(true);
    expect(iterator.next()).andReturn(group);
    expect(group.getID()).andReturn("group:A");

    expect(iterator.hasNext()).andReturn(true);
    expect(iterator.next()).andReturn(group);
    expect(group.getID()).andReturn("group:B");

    expect(iterator.hasNext()).andReturn(false);


    expect(user.memberOf()).andReturn(iterator);
    expect(iterator.hasNext()).andReturn(true);
    expect(iterator.next()).andReturn(group);
    expect(group.getID()).andReturn("indirectgroup:A");

    expect(iterator.hasNext()).andReturn(true);
    expect(iterator.next()).andReturn(group);
    expect(group.getID()).andReturn("indirectgroup:B");
    expect(iterator.hasNext()).andReturn(false);

    deactivate();

    replay();
    clusterTrackingServiceImpl.activate(componentContext);

    clusterUserServlet.doGet(request, response);

    clusterTrackingServiceImpl.deactivate(componentContext);

    JSONObject jsonObject = new JSONObject(writer.toString());
    assertEquals(serverId, jsonObject.get("server"));
    JSONObject userObject = jsonObject.getJSONObject("user");
    assertEquals("otherServerId",userObject.get("homeServer"));
    assertEquals("ieb",userObject.get("id"));
    assertEquals("principal:ieb",userObject.get("principal"));
    JSONObject properties = userObject.getJSONObject("properties");
    assertEquals(3, properties.getJSONArray("prop1").length());
    assertEquals("tokenA", properties.get("prop2"));
    assertEquals(0, properties.getJSONArray("prop3").length());


    JSONArray declaredMembership = userObject.getJSONArray("declaredMembership");
    assertEquals(2, declaredMembership.length());
    assertEquals("group:A", declaredMembership.get(0));
    assertEquals("group:B", declaredMembership.get(1));

    JSONArray membership = userObject.getJSONArray("membership");
    assertEquals(2, membership.length());
    assertEquals("indirectgroup:A", membership.get(0));
    assertEquals("indirectgroup:B", membership.get(1));

    checkActivation();
    verify();
  }



  @Test
  public void testNotFoundGet() throws Exception {
    activate();
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    Resource resource = createMock(Resource.class);
    Node node = createMock(Node.class);


    expect(request.getResource()).andReturn(resource);
    expect(resource.adaptTo(Node.class)).andReturn(node);

    // request validated, processing request.
    expect(request.getParameter("c")).andReturn(serverId+"-sometrackingcookie");
    expect(userTrackingCache.get(serverId+"-sometrackingcookie")).andReturn(null);

    Capture<Integer> codeCapture = new Capture<Integer>();
    Capture<String> messageCapture = new Capture<String>();
    response.sendError(capture(codeCapture ),capture(messageCapture));
    expectLastCall();

    clusterUserServlet.testing = true;

    deactivate();

    replay();
    clusterTrackingServiceImpl.activate(componentContext);

    clusterUserServlet.doGet(request, response);

    clusterTrackingServiceImpl.deactivate(componentContext);


    checkActivation();

    assertEquals(404, codeCapture.getValue().intValue());

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
