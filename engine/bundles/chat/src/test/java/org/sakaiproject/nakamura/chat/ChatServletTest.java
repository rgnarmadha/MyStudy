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
package org.sakaiproject.nakamura.chat;

import static org.junit.Assert.assertEquals;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.memory.Cache;
import org.sakaiproject.nakamura.api.memory.CacheManagerService;
import org.sakaiproject.nakamura.api.memory.CacheScope;
import org.sakaiproject.nakamura.memory.MapCacheImpl;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;

/**
 *
 */
public class ChatServletTest {

  private String user1 = "Jack";
  private CacheManagerService cacheManagerService;
  private ChatManagerServiceImpl chatManagerService;
  private Cache<Object> chatCache;
  private ChatServlet chatServlet;

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    chatCache = new MapCacheImpl<Object>();
    cacheManagerService = createMock(CacheManagerService.class);
    expect(cacheManagerService.getCache("chat", CacheScope.CLUSTERREPLICATED))
        .andReturn(chatCache).anyTimes();

    replay(cacheManagerService);

    chatManagerService = new ChatManagerServiceImpl();
    chatManagerService.bindCacheManagerService(cacheManagerService);

    chatServlet = new ChatServlet();
    chatServlet.bindChatManagerService(chatManagerService);
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
    chatManagerService.unbindCacheManagerService(cacheManagerService);
    chatServlet.unbindChatManagerService(chatManagerService);
    verify(cacheManagerService);
  }

  @Test
  public void testFirstTime() throws IOException, JSONException,
      ServletException {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    expect(request.getRemoteUser()).andReturn(user1);
    expect(request.getRequestParameter("t")).andReturn(null);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter printWriter = new PrintWriter(baos);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    expect(response.getWriter()).andReturn(printWriter);
    response.setHeader("Connection", "close");
    replay(request, response);

    chatServlet.doGet(request, response);

    printWriter.flush();
    JSONObject obj = new JSONObject(baos.toString("UTF-8"));
    assertEquals(true, obj.get("update"));
  }

  @Test
  public void testNoUpdate() throws ServletException, IOException,
      JSONException {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    expect(request.getRemoteUser()).andReturn(user1);
    RequestParameter param = createMock(RequestParameter.class);
    expect(param.getString()).andReturn("100");
    expect(request.getRequestParameter("t")).andReturn(param);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter printWriter = new PrintWriter(baos);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    expect(response.getWriter()).andReturn(printWriter);
    response.setHeader("Connection", "close");
    replay(param, request, response);

    // User1 gets a message at 50
    chatManagerService.put(user1, 50);

    chatServlet.doGet(request, response);

    printWriter.flush();
    JSONObject obj = new JSONObject(baos.toString("UTF-8"));
    assertEquals(false, obj.get("update"));
  }

  @Test
  public void testUpdate() throws ServletException, IOException, JSONException {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    expect(request.getRemoteUser()).andReturn(user1);
    RequestParameter param = createMock(RequestParameter.class);
    expect(param.getString()).andReturn("20");
    expect(request.getRequestParameter("t")).andReturn(param);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter printWriter = new PrintWriter(baos);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    expect(response.getWriter()).andReturn(printWriter);
    response.setHeader("Connection", "close");
    replay(param, request, response);

    // User1 gets a message at 50
    chatManagerService.put(user1, 50);

    chatServlet.doGet(request, response);

    printWriter.flush();
    JSONObject obj = new JSONObject(baos.toString("UTF-8"));
    assertEquals(true, obj.get("update"));
  }

}
