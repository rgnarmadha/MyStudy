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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.junit.Test;
import org.sakaiproject.nakamura.api.user.UserConstants;

import javax.jcr.Node;

/**
 * Lot's of ACL issues going on in here..
 */
public class MessageAuthorizablePostProcessorTest {

  @Test
  public void testNotAUser() throws Exception {
    MessageAuthorizablePostProcessor proc = new MessageAuthorizablePostProcessor();
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    Group group = mock(Group.class);
    JackrabbitSession session = mock(JackrabbitSession.class);

    RequestPathInfo info = mock(RequestPathInfo.class);
    when(info.getResourcePath()).thenReturn("/wrong/path");
    when(request.getRequestPathInfo()).thenReturn(info);

    proc.process(group, session, Modification.onCreated("/wrong/path"), null);
  }

// we cant test this becuase we need a principal manager, and that is part Acl Utils  @Test
  public void testAuthorizable() throws Exception {
    MessageAuthorizablePostProcessor proc = new MessageAuthorizablePostProcessor();
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    JackrabbitSession session = mock(JackrabbitSession.class);
    Node node = mock(Node.class);
    User user = mock(User.class);

    RequestPathInfo info = mock(RequestPathInfo.class);
    when(info.getResourcePath()).thenReturn(UserConstants.SYSTEM_USER_MANAGER_USER_PATH);
    when(request.getRequestPathInfo()).thenReturn(info);
    RequestParameter nameParam = mock(RequestParameter.class);
    when(nameParam.getString()).thenReturn("newUserID");
    when(request.getRequestParameter(SlingPostConstants.RP_NODE_NAME)).thenReturn(
        nameParam);
    when(user.getID()).thenReturn("newUserID");
    when(session.itemExists("/_user/message/5e/66/54/f7/newUserID")).thenReturn(true);
    when(session.getItem("/_user/message/5e/66/54/f7/newUserID")).thenReturn(node);
    UserManager userManager = mock(UserManager.class);
    when(userManager.getAuthorizable("newUserID")).thenReturn(null);
    when(session.getUserManager()).thenReturn(userManager);

    proc.process(user, session, Modification.onCreated(UserConstants.SYSTEM_USER_MANAGER_USER_PATH+"newUserID"), null);
  }
}
