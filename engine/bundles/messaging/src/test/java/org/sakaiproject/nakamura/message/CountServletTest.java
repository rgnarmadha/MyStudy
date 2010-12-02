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
package org.sakaiproject.nakamura.message;

import static org.junit.Assert.assertNotSame;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.commons.testing.jcr.MockNodeIterator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.message.MessagingService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.ServletException;

/**
 *
 */
public class CountServletTest {

  private CountServlet servlet;
  private MessagingService messagingService;

  @Before
  public void setUp() {
    servlet = new CountServlet();
    messagingService = mock(MessagingService.class);

    servlet.messagingService = messagingService;
  }

  @After
  public void tearDown() {
    servlet.messagingService = messagingService;
  }

  @Test
  public void testParams() throws ServletException, IOException, RepositoryException,
      JSONException {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter write = new PrintWriter(baos);
    when(response.getWriter()).thenReturn(write);

    // Request stuff
    RequestParameter groupParam = mock(RequestParameter.class);
    when(groupParam.getString()).thenReturn("foo");
    when(request.getRemoteUser()).thenReturn("admin");
    when(request.getRequestParameter("groupedby")).thenReturn(groupParam);

    String queryString = "/jcr:root/path/to/store//*[@sling:resourceType=\"sakai/message\" and @sakai:type=\"internal\"]";

    // Results
    Map<String, String> props = new HashMap<String, String>();
    props.put("foo", "a");
    Map<String, String> propsC = new HashMap<String, String>();
    propsC.put("foo", "c");

    MockNode resultA = createMockNode("/path/to/msgA", props);
    MockNode resultB = createMockNode("/path/to/msgB", props);
    MockNode resultC = createMockNode("/path/to/msgC", propsC);

    Node[] nodes = new Node[] { resultA, resultB, resultC };

    NodeIterator iterator = new MockNodeIterator(nodes);

    // Session & search
    Session session = mock(Session.class);
    Workspace workspace = mock(Workspace.class);
    QueryManager queryManager = mock(QueryManager.class);
    Query q = mock(Query.class);
    QueryResult queryResult = mock(QueryResult.class);

    when(queryResult.getNodes()).thenReturn(iterator);
    when(q.execute()).thenReturn(queryResult);
    when(queryManager.createQuery(queryString, Query.XPATH)).thenReturn(q);
    when(workspace.getQueryManager()).thenReturn(queryManager);
    when(session.getWorkspace()).thenReturn(workspace);

    // Node
    MockNode node = new MockNode("/_user/message.count.json");
    node.setSession(session);
    Resource resource = mock(Resource.class);
    when(resource.adaptTo(Node.class)).thenReturn(node);
    when(request.getResource()).thenReturn(resource);

    when(messagingService.getFullPathToStore("admin", session)).thenReturn(
        "/path/to/store");

    servlet.doGet(request, response);

    write.flush();
    String s = baos.toString("UTF-8");
    JSONObject o = new JSONObject(s);

    JSONArray arr = o.getJSONArray("count");
    assertEquals(2, arr.length());
    assertNotSame(arr.getJSONObject(0).getString("group"), arr.getJSONObject(1)
        .getString("group"));
    assertNotSame(arr.getJSONObject(0).getString("count"), arr.getJSONObject(1)
        .getString("count"));

  }

  /**
   * Create a {@link MockNode}.
   * 
   * @param path
   *          The path for this node.
   * @param props
   *          The properties that should be set.
   * @return
   * @throws RepositoryException
   */
  private MockNode createMockNode(String path, Map<String, String> props)
      throws RepositoryException {
    MockNode node = new MockNode(path);
    for (Entry<String, String> e : props.entrySet()) {
      node.setProperty(e.getKey(), e.getValue());
    }
    return node;
  }

}
