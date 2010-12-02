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
package org.sakaiproject.nakamura.user.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.servlets.post.Modification;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sakaiproject.nakamura.api.user.UserConstants;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

/**
 *
 */
public class AbstractSakaiGroupPostServletTest {

  @Mock
  private SlingHttpServletRequest request;
  @Mock
  private Group group;
  @Mock
  private ResourceResolver resourceResolver;
  @Mock
  private Session session;
  @Mock
  private ValueFactory valueFactory;
  @Mock
  private JackrabbitSession jrSession;
  @Mock
  private JackrabbitSession adminSession;
  @Mock
  private Authorizable userAuth;
  @Mock
  private UserManager userManager;
  @Mock
  private UserManager adminUserManager;
  @Mock
  private SlingRepository repository;
  
  private AbstractSakaiGroupPostServlet servlet;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(request.getResourceResolver()).thenReturn(resourceResolver);
    when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
    when(session.getValueFactory()).thenReturn(valueFactory);

    // By default the user 'jeff' is always a manager of the group.
    when(group.hasProperty(UserConstants.PROP_GROUP_MANAGERS)).thenReturn(true);
    Value[] managers = new Value[1];
    Value jeffValue = mock(Value.class);
    when(jeffValue.getString()).thenReturn("jeff");
    managers[0] = jeffValue;
    when(group.getProperty(UserConstants.PROP_GROUP_MANAGERS)).thenReturn(managers);

    servlet = new AbstractSakaiGroupPostServlet() {

      private static final long serialVersionUID = -475691532657996299L;

      @Override
      protected void handleOperation(SlingHttpServletRequest request,
          HtmlResponse htmlResponse, List<Modification> changes)
          throws RepositoryException {
        // TODO Auto-generated method stub

      }
    };
  }

  @Test
  public void testAddManager() throws Exception {
    when(request.getParameterValues(":manager")).thenReturn(
        new String[] { "jack", "john" });
    when(request.getParameterValues(":manager@Delete")).thenReturn(null);

    Value value = mock(Value.class);
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    when(valueFactory.createValue(captor.capture())).thenReturn(value);

    servlet.updateOwnership(request, group, new String[] { "joe" }, null);

    List<String> values = captor.getAllValues();
    assertTrue(values.contains("jeff"));
    assertTrue(values.contains("jack"));
    assertTrue(values.contains("john"));
    assertTrue(values.contains("joe"));
    assertEquals(4, values.size());
  }

  @Test
  public void testDeleteManager() throws Exception {
    // Remove jeff, add jack
    when(request.getParameterValues(":manager")).thenReturn(new String[] { "jack" });
    when(request.getParameterValues(":manager@Delete")).thenReturn(
        new String[] { "jeff" });

    Value value = mock(Value.class);
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    when(valueFactory.createValue(captor.capture())).thenReturn(value);

    servlet.updateOwnership(request, group, new String[0], null);

    List<String> values = captor.getAllValues();
    assertTrue(values.contains("jack"));
    assertEquals(1, values.size());
  }
  
  @Test
  public void testNonJoinableGroup() throws Exception {
    when(group.isGroup()).thenReturn(true);
    when(group.hasProperty(UserConstants.PROP_JOINABLE_GROUP)).thenReturn(true);
    Value[] joinableArray = new Value[1];
    Value joinable = mock(Value.class);
    when(joinable.getString()).thenReturn("no");
    joinableArray[0] = joinable;
    when(group.getProperty(UserConstants.PROP_JOINABLE_GROUP)).thenReturn(joinableArray);
    
    when(request.getRemoteUser()).thenReturn("abc");
    when(jrSession.getUserID()).thenReturn("abc");
    when(resourceResolver.adaptTo(Session.class)).thenReturn(jrSession);
    when(jrSession.getUserManager()).thenReturn(userManager);
    when(request.getParameterValues(":member")).thenReturn(
        new String[] { "abc" });
    when(userAuth.getID()).thenReturn("abc");
    when(userManager.getAuthorizable("abc")).thenReturn(userAuth);
    
    when(group.addMember(userAuth)).thenThrow(new RepositoryException());
    
    
    ArrayList<Modification> changes = new ArrayList<Modification>();
    try{
      servlet.updateGroupMembership(request, group, changes);
    }catch (RepositoryException e) {
      
    }
    assertTrue(changes.size() == 0);
  }
  
  @Test
  public void testJoinableGroup() throws Exception {
    when(group.isGroup()).thenReturn(true);
    when(group.getID()).thenReturn("g-foo");
    when(group.hasProperty(UserConstants.PROP_JOINABLE_GROUP)).thenReturn(true);
    Value[] joinableArray = new Value[1];
    Value joinable = mock(Value.class);
    when(joinable.getString()).thenReturn("yes");
    joinableArray[0] = joinable;
    when(group.getProperty(UserConstants.PROP_JOINABLE_GROUP)).thenReturn(joinableArray);
    
    when(request.getRemoteUser()).thenReturn("abc");
    when(jrSession.getUserID()).thenReturn("abc");
    when(resourceResolver.adaptTo(Session.class)).thenReturn(jrSession);
    when(jrSession.getUserManager()).thenReturn(userManager);
    when(request.getParameterValues(":member")).thenReturn(
        new String[] { "abc" });
    when(userAuth.getID()).thenReturn("abc");
    when(userManager.getAuthorizable("abc")).thenReturn(userAuth);
    
    servlet.repository = repository;  
    when(adminSession.getUserManager()).thenReturn(adminUserManager);
    when(repository.loginAdministrative(null)).thenReturn(adminSession);
    when(adminUserManager.getAuthorizable("g-foo")).thenReturn(group);
    when(group.addMember(userAuth)).thenReturn(true);
    
    
    adminSession.logout();
    
    ArrayList<Modification> changes = new ArrayList<Modification>();
    
    servlet.updateGroupMembership(request, group, changes);
    
    assertTrue(changes.size() > 0);
  }

}
