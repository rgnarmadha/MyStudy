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
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.message.MessagingService;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Session;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 *
 */
public class ChatMessageSearchResultProviderTest {

  private MessagingService messagingService;
  private ChatMessageSearchPropertyProvider propProvider;
  private Session session;
  private String user = "johndoe";
  private ResourceResolver resourceResolver;

  @Before
  public void setUp() {
    messagingService = createMock(MessagingService.class);

    propProvider = new ChatMessageSearchPropertyProvider();
    propProvider.messagingService = messagingService;
    session = createMock(Session.class);
    resourceResolver = createMock(ResourceResolver.class);
    expect(resourceResolver.adaptTo(Session.class)).andReturn(session)
        .anyTimes();
    replay(resourceResolver, session);
    expect(messagingService.getFullPathToStore(user, session)).andReturn(
        "/full/path/to/store");

    replay(messagingService);

  }

  @After
  public void tearDown() {
    propProvider.messagingService = null;
    verify(messagingService);
  }

  @Test
  public void testProperties() {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getRemoteUser()).andReturn(user);
    expect(request.getResourceResolver()).andReturn(resourceResolver);
    RequestParameter param = createMock(RequestParameter.class);
    expect(param.getString()).andReturn("jack,peter,mary").anyTimes();
    expect(request.getRequestParameter("_from")).andReturn(param);
    replay(param, request);
    Map<String, String> props = new HashMap<String, String>();
    propProvider.loadUserProperties(request, props);

    String expected = " and ((@sakai:from=\"jack\" or @sakai:from=\"peter\" or @sakai:from=\"mary\" or @sakai:from=\"johndoe\")";
    expected += " or (@sakai:to=\"jack\" or @sakai:to=\"peter\" or @sakai:to=\"mary\" or @sakai:to=\"johndoe\"))";
    assertEquals(expected, props.get("_from"));

  }

}
