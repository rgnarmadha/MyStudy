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
package org.sakaiproject.nakamura.connections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.junit.Test;
import org.sakaiproject.nakamura.api.search.SearchServiceFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Row;

/**
 *
 */
public class ConnectionFinderSearchResultProcessorTest {

  @Test
  public void test() throws RepositoryException, JSONException,
      UnsupportedEncodingException {
    ConnectionFinderSearchResultProcessor processor = new ConnectionFinderSearchResultProcessor();
    SearchServiceFactory searchServiceFactory = mock(SearchServiceFactory.class);
    processor.searchServiceFactory = searchServiceFactory;

    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    ResourceResolver resolver = mock(ResourceResolver.class);
    JackrabbitSession session = mock(JackrabbitSession.class);
    when(request.getResourceResolver()).thenReturn(resolver);
    when(resolver.adaptTo(Session.class)).thenReturn(session);

    UserManager um = mock(UserManager.class);
    when(session.getUserManager()).thenReturn(um);
    when(request.getRemoteUser()).thenReturn("alice");
    Authorizable auAlice = mock(Authorizable.class);
    when(auAlice.getID()).thenReturn("alice");
    when(auAlice.isGroup()).thenReturn(false);
    Authorizable auBob = mock(Authorizable.class);
    when(auBob.getID()).thenReturn("bob");
    when(auBob.isGroup()).thenReturn(false);
    when(um.getAuthorizable("alice")).thenReturn(auAlice);
    when(um.getAuthorizable("bob")).thenReturn(auBob);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter w = new PrintWriter(baos);
    JSONWriter write = new JSONWriter(w);
    Row row = mock(Row.class);
    MockNode resultNode = new MockNode("/_user/bob/public/authprofile");
    resultNode.setProperty("rep:userId", "bob");
    resultNode.setProperty("lastName", "The Builder");
    when(row.getNode()).thenReturn(resultNode);

    MockNode contactNode = new MockNode("/_user/alice/contacts/bob");
    contactNode.setProperty("sling:resourceType", "sakai/contact");
    when(session.getItem("/_user/alice/contacts/bob")).thenReturn(contactNode);

    RequestPathInfo pathInfo = mock(RequestPathInfo.class);
    when(request.getRequestPathInfo()).thenReturn(pathInfo);

    processor.writeNode(request, write, null, row);

    w.flush();
    String s = baos.toString("UTF-8");
    JSONObject o = new JSONObject(s);
    assertEquals("bob", o.getString("target"));
    assertEquals("The Builder", o.getJSONObject("profile").getString("lastName"));
    assertEquals("sakai/contact", o.getJSONObject("details").getString(
        "sling:resourceType"));
  }

}
