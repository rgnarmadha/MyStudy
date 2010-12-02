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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sakaiproject.nakamura.api.memory.Cache;
import org.sakaiproject.nakamura.api.memory.CacheScope;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class WidgetizeServletTest extends AbstractWidgetServletTest {

  private WidgetizeServlet servlet;

  @Before
  public void setUp() throws IOException {
    super.setUp();

    servlet = new WidgetizeServlet();
    servlet.widgetService = widgetService;
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGoodWidgetUncached() throws Exception {
    // Always return null for cached content.
    Cache<Object> cache = mock(Cache.class);
    when(
        cacheManagerService
            .getCache(Mockito.anyString(), Mockito.eq(CacheScope.INSTANCE))).thenReturn(
        cache);

    // Setup request to point to the correct twitter widget.
    Resource twitterResource = resolver.getResource("/widgets/twitter");
    when(request.getResource()).thenReturn(twitterResource);
    
    
    servlet.doGet(request, response);

    printWriter.flush();
    JSONObject json = new JSONObject(stringWriter.toString());

    // Assert the responses
    verify(response).setCharacterEncoding("UTF-8");
    verify(response).setContentType("application/json");

    // Check if the correct languages get loaded.
    assertNotNull(json.get("bundles"));
    assertNotNull(json.getJSONObject("bundles").get("default"));
    assertNotNull(json.getJSONObject("bundles").get("nl_NL"));
    String def = json.getJSONObject("bundles").getJSONObject("default").getString(
        "YOUR_STATUS_HAS_BEEN_SUCCESSFULLY_UPDATED");
    assertEquals("Your status has been succesfully updated", def);
    String dutch = json.getJSONObject("bundles").getJSONObject("nl_NL").getString(
        "YOUR_STATUS_HAS_BEEN_SUCCESSFULLY_UPDATED");
    assertEquals("Uw status is geupdated", dutch);

    // Check if the all the files get loaded.
    assertNotNull(json.getJSONObject("css").getJSONObject("twitter.css"));
    assertNotNull(json.getJSONObject("javascript").getJSONObject("twitter.js"));
    assertNotNull(json.getJSONObject("twitter.html"));

    // Make sure that the content is there.
    assertEquals(String.class, json.getJSONObject("css").getJSONObject("twitter.css")
        .get("content").getClass());
    assertEquals(String.class, json.getJSONObject("javascript").getJSONObject(
        "twitter.js").get("content").getClass());
    assertEquals(String.class, json.getJSONObject("twitter.html").get("content")
        .getClass());

    // Make sure that images don't get loaded.
    assertEquals(false, json.getJSONObject("images").getJSONObject("twitter.png").get(
        "content"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testBadWidget() throws Exception {
    // Always return null for cached content.
    Cache<Object> cache = mock(Cache.class);
    when(
        cacheManagerService
            .getCache(Mockito.anyString(), Mockito.eq(CacheScope.INSTANCE))).thenReturn(
        cache);


    // Setup request to point to the malformed badwidget.
    Resource badwidget = resolver.getResource("/widgets/badwidget");
    when(request.getResource()).thenReturn(badwidget);
    
    servlet.doGet(request, response);

    verify(response).sendError(HttpServletResponse.SC_FORBIDDEN,
        "The current resource is not a widget.");

  }

}
