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
package org.sakaiproject.nakamura.batch;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sakaiproject.nakamura.batch.BatchServlet.REQUESTS_PARAMETER;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class BatchServletTest {

  private BatchServlet servlet;
  private SlingHttpServletRequest request;
  private SlingHttpServletResponse response;

  @Before
  public void setUp() throws Exception {
    servlet = new BatchServlet();
    request = mock(SlingHttpServletRequest.class);
    response = mock(SlingHttpServletResponse.class);

  }

  @Test
  public void testInvalidRequest() throws ServletException, IOException {
    when(request.getParameter(REQUESTS_PARAMETER)).thenReturn("marlformedparameter");
    servlet.doGet(request, response);
    verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST,
        "Failed to parse the " + REQUESTS_PARAMETER + " parameter");
  }

  @Test
  public void testSimpleRequest() throws Exception {
    String json = "[{\"url\" : \"/foo/bar\",\"method\" : \"POST\",\"parameters\" : {\"val\" : 123,\"val@TypeHint\" : \"Long\"}}]";
    when(request.getParameter(REQUESTS_PARAMETER)).thenReturn(json);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter writer = new PrintWriter(baos);

    RequestDispatcher dispatcher = mock(RequestDispatcher.class);
    ResourceResolver resourceResolver = mock(ResourceResolver.class);
    Resource resource = mock(Resource.class);
    when(request.getResourceResolver()).thenReturn(resourceResolver);
    when(resourceResolver.resolve(request, "/foo/bar")).thenReturn(resource);
    when(request.getRequestDispatcher(resource)).thenReturn(dispatcher);
    when(response.getWriter()).thenReturn(writer);
    servlet.doPost(request, response);
  }

}
