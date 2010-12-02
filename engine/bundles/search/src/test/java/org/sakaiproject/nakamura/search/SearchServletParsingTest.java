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
package org.sakaiproject.nakamura.search;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ResourceResolver;
import org.easymock.EasyMock;
import org.easymock.internal.matchers.Any;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;

/**
 *
 */
public class SearchServletParsingTest extends AbstractEasyMockTest {

  private SlingHttpServletRequest request;
  private SearchServlet searchServlet;
  private Object[] mocks;

  @Before
  public void setup() throws Exception {
    searchServlet = new SearchServlet();
    request = EasyMock.createMock(SlingHttpServletRequest.class);
    RequestParameter rp = EasyMock.createMock(RequestParameter.class);
    expect(request.getRemoteUser()).andReturn("admin").anyTimes();
    expect(request.getRequestParameter("q")).andReturn(rp).anyTimes();
    expect(request.getRequestParameter("null")).andReturn(null).anyTimes();
    expect(rp.getString()).andReturn("testing").anyTimes();
    RequestParameter rp_a = EasyMock.createMock(RequestParameter.class);

    expect(request.getRequestParameter("a")).andReturn(rp_a).anyTimes();
    expect(rp_a.getString()).andReturn("again").anyTimes();

    Authorizable au = createAuthorizable("admin", false, true);

    UserManager um = EasyMock.createMock(UserManager.class);
    expect(um.getAuthorizable("admin")).andReturn(au).anyTimes();

    JackrabbitSession session = EasyMock.createMock(JackrabbitSession.class);
    expect(session.getUserManager()).andReturn(um).anyTimes();

    ResourceResolver resourceResolver = EasyMock.createMock(ResourceResolver.class);
    expect(resourceResolver.adaptTo(Session.class)).andReturn(session).anyTimes();
    expect(request.getResourceResolver()).andReturn(resourceResolver).anyTimes();
    mocks = new Object[] {request, rp, rp_a, resourceResolver, um, session};
    EasyMock.replay(mocks);

  }

  @After
  public void tearDown() {
    EasyMock.verify(mocks);
  }

  @Test
  public void testQueryParsing() throws Exception {
    Node node = createNiceMock(Node.class);
    PropertyIterator propertyIterator = createNiceMock(PropertyIterator.class);
    expect(node.getProperties()).andReturn(propertyIterator);
    replay();
    String result = searchServlet.processQueryTemplate(request, node, " {q}", Query.SQL, null);
    assertEquals(" testing", result);
    verify();
  }

  @Test
  public void testQueryParsing1() throws Exception {
    Node node = createNiceMock(Node.class);
    PropertyIterator propertyIterator = createNiceMock(PropertyIterator.class);
    expect(node.getProperties()).andReturn(propertyIterator);
    replay();
    String result = searchServlet.processQueryTemplate(request, node, "{q} ", Query.SQL, null);
    assertEquals("testing ", result);
    verify();
  }

  @Test
  public void testQueryParsing2() throws Exception {
    Node node = createNiceMock(Node.class);
    PropertyIterator propertyIterator = createNiceMock(PropertyIterator.class);
    expect(node.getProperties()).andReturn(propertyIterator);
    replay();
    String result = searchServlet.processQueryTemplate(request, node, "{q} {a}", Query.SQL,
        null);
    assertEquals("testing again", result);
    verify();
  }

  @Test
  public void testQueryParsingDefaultVals() throws Exception {
    Node node = createNiceMock(Node.class);
    PropertyIterator propertyIterator = createNiceMock(PropertyIterator.class);
    expect(node.getProperties()).andReturn(propertyIterator);
    replay();
    String result = searchServlet.processQueryTemplate(request, node, "{q|foo}", Query.SQL,
        null);
    assertEquals("testing", result);
    verify();
  }

  @Test
  public void testQueryParsingDefaultNullVals() throws Exception {
    Node node = createNiceMock(Node.class);
    PropertyIterator propertyIterator = createNiceMock(PropertyIterator.class);
    expect(node.getProperties()).andReturn(propertyIterator);
    replay();
    String result = searchServlet.processQueryTemplate(request, node, "{null|foo}", Query.SQL, null);
    assertEquals("foo", result);
    verify();
  }
}
