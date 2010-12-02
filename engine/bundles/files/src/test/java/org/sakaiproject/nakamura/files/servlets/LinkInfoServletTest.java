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
package org.sakaiproject.nakamura.files.servlets;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.when;

import static org.mockito.Mockito.mock;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;

/**
 *
 */
public class LinkInfoServletTest {

  @SuppressWarnings(value={"NP_ALWAYS_NULL"}, justification="Wierd, incorrect report, on System.err.println(s);")
  @Test
  public void test() throws IOException, ServletException, JSONException,
      RepositoryException {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);

    Resource resource = mock(Resource.class);
    Node node = new MockNode("/path/to/link");
    when(request.getResource()).thenReturn(resource);
    when(resource.adaptTo(Node.class)).thenReturn(node);

    ResourceResolver resolver = mock(ResourceResolver.class);
    Session session = mock(Session.class);
    when(resolver.adaptTo(Session.class)).thenReturn(session);
    when(request.getResourceResolver()).thenReturn(resolver);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter w = new PrintWriter(baos);

    when(response.getWriter()).thenReturn(w);

    LinkInfoServlet servlet = new LinkInfoServlet();
    servlet.doGet(request, response);

    w.flush();
    String s = baos.toString("UTF-8");
    JSONObject o = new JSONObject(s);
    assertEquals(node.getPath(), o.get("jcr:path"));

    System.err.println(s);
  }

}
