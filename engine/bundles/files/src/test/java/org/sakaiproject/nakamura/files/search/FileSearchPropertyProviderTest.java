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
package org.sakaiproject.nakamura.files.search;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.connections.ConnectionManager;
import org.sakaiproject.nakamura.api.connections.ConnectionState;
import org.sakaiproject.nakamura.api.site.SiteService;

import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class FileSearchPropertyProviderTest {

  private ConnectionManager connectionManager;
  private SiteService siteService;
  private FileSearchPropertyProvider provider;

  @Before
  public void setUp() {
    siteService = mock(SiteService.class);
    connectionManager = mock(ConnectionManager.class);

    provider = new FileSearchPropertyProvider();
    provider.connectionManager = connectionManager;
    provider.siteService = siteService;
  }

  @Test
  public void testSortOrder() {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);

    RequestParameter sortOnParam = mock(RequestParameter.class);
    RequestParameter sortOrderParam = mock(RequestParameter.class);

    when(sortOnParam.getString()).thenReturn("jcr:mimeType");
    when(sortOrderParam.getString()).thenReturn("descending");

    when(request.getRequestParameter("sortOn")).thenReturn(sortOnParam);
    when(request.getRequestParameter("sortOrder")).thenReturn(sortOrderParam);

    String result = provider.doSortOrder(request);
    assertEquals(" order by @jcr:mimeType descending", result);
  }

  @Test
  public void testTags() {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);

    String[] tags = new String[] { "foo", "bar" };
    when(request.getParameterValues("sakai:tags")).thenReturn(tags);
    String result = provider.doTags(request);
    assertEquals(" and (@sakai:tags=\"foo\" and @sakai:tags=\"bar\")", result);
  }

  @Test
  public void testEmptyValue() {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    String result = provider.getSearchValue(request);
    assertEquals("*", result);
  }

  @Test
  public void testNonEmptyValue() {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    RequestParameter searchParam = mock(RequestParameter.class);
    when(searchParam.getString()).thenReturn("term");
    when(request.getRequestParameter("q")).thenReturn(searchParam);
    String result = provider.getSearchValue(request);
    assertEquals("term", result);
  }

  @Test
  public void testContacts() {
    List<String> connections = Arrays.asList(new String[] { "bob", "jack" });
    when(connectionManager.getConnectedUsers("alice", ConnectionState.ACCEPTED))
        .thenReturn(connections);

    String query = provider.getMyContacts("alice");
    assertEquals(" and (@jcr:createdBy=\"bob\" or @jcr:createdBy=\"jack\")", query);
  }

  @Test
  public void testNoContacts() {
    List<String> connections = Arrays.asList(new String[] {});
    when(connectionManager.getConnectedUsers("alice", ConnectionState.ACCEPTED))
        .thenReturn(connections);

    String query = provider.getMyContacts("alice");
    assertEquals("", query);
  }
}
