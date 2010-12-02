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
package org.sakaiproject.nakamura.docproxy;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.REPOSITORY_REF;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.junit.After;
import org.junit.Test;
import org.sakaiproject.nakamura.api.docproxy.DocProxyException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;

/**
 *
 */
public class SearchServletTest extends AbstractDocProxyServlet {

  private ExternalDocumentSearchServlet servlet;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.docproxy.AbstractDocProxyServlet#setUp()
   */
  @Override
  public void setUp() throws Exception {
    super.setUp();

    servlet = new ExternalDocumentSearchServlet();
    servlet.activate(componentContext);
    servlet.tracker = tracker;
  }

  @After
  public void tearDown() {
    servlet.deactivate(componentContext);
  }

  @Test
  public void testSearch() throws PathNotFoundException, RepositoryException,
      DocProxyException, IOException, ServletException, JSONException {
    // Create a couple of files.
    for (int i = 0; i < 10; i++) {
      createFile(diskProcessor, proxyNode, "test-search-" + i + "-servlet", "alfa");
    }
    // Mock objects.
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    Session session = createMock(Session.class);

    Resource resource = createMock(Resource.class);
    MockNode searchNode = new MockNode("/var/search/docproxy/disk/disk");
    searchNode.setProperty(REPOSITORY_REF, "uuid");
    searchNode.setSession(session);
    searchNode.setProperty("sakai:search-prop-starts-with", "{starts-with}");
    expect(session.getNodeByIdentifier("uuid")).andReturn(proxyNode);
    expect(resource.adaptTo(Node.class)).andReturn(searchNode);
    expect(request.getResource()).andReturn(resource);

    addStringRequestParameter(request, "starts-with", "test-search-");
    addStringRequestParameter(request, "ends-with", "-servlet");
    addStringRequestParameter(request, "items", "5");
    addStringRequestParameter(request, "page", "1");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter write = new PrintWriter(baos);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    expect(response.getWriter()).andReturn(write);
    replay();

    servlet.doGet(request, response);
    write.flush();

    String s = baos.toString("UTF-8");
    JSONArray o = new JSONArray(s);

    assertEquals(5, o.length());
  }
}
