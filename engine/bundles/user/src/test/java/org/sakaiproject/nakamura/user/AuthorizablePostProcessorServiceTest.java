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
package org.sakaiproject.nakamura.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor;
import org.sakaiproject.nakamura.api.user.UserConstants;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

@RunWith(MockitoJUnitRunner.class)
public class AuthorizablePostProcessorServiceTest {
  private AuthorizablePostProcessServiceImpl authorizablePostProcessService;
  @Mock
  private JackrabbitSession session;
  @Mock
  private User user;
  @Mock
  private Group group;
  @Mock
  SlingHttpServletRequest request;
  @Mock
  AuthorizablePostProcessor authorizablePostProcessor;
  @Mock
  SakaiUserProcessor sakaiUserProcessor;
  @Mock
  SakaiGroupProcessor sakaiGroupProcessor;


  @Before
  public void setUp() throws RepositoryException {
    authorizablePostProcessService = new AuthorizablePostProcessServiceImpl();
    authorizablePostProcessService.sakaiUserProcessor = sakaiUserProcessor;
    authorizablePostProcessService.sakaiGroupProcessor = sakaiGroupProcessor;
    authorizablePostProcessService.bindAuthorizablePostProcessor(authorizablePostProcessor, new HashMap<String, Object>());
    when(user.isGroup()).thenReturn(false);
    when(user.getID()).thenReturn("joe");
    when(group.isGroup()).thenReturn(true);
    when(group.getID()).thenReturn("faculty");
  }

  @SuppressWarnings("unchecked")
  @Test
  public void extractsNonpersistedRequestParameters() throws Exception {
    List<String> parameters = Arrays.asList(new String [] {"donotpass", ":dopass"});
    when(request.getParameterNames()).thenReturn(Collections.enumeration(parameters));

    RequestParameter requestParameter = mock(RequestParameter.class, RETURNS_DEEP_STUBS);
    RequestParameterMap requestParameterMap = mock(RequestParameterMap.class);
    when(requestParameterMap.keySet()).thenReturn(new HashSet<String>(parameters));
    when(requestParameterMap.getValues(anyString())).thenReturn(new RequestParameter[] {requestParameter});
    when(request.getRequestParameterMap()).thenReturn(requestParameterMap);

    ArgumentCaptor<Map> mapArgument = ArgumentCaptor.forClass(Map.class);
    authorizablePostProcessService.process(user, session, ModificationType.MODIFY, request);
    verify(authorizablePostProcessor).process(eq(user), eq(session), any(Modification.class), mapArgument.capture());
    assertTrue(mapArgument.getValue().size() == 1);
    assertNotNull(mapArgument.getValue().get(":dopass"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void parametersAreOptional() throws Exception {
    ArgumentCaptor<Map> mapArgument = ArgumentCaptor.forClass(Map.class);
    authorizablePostProcessService.process(user, session, ModificationType.MODIFY);
    verify(authorizablePostProcessor).process(eq(user), eq(session), any(Modification.class), mapArgument.capture());
    assertTrue(mapArgument.getValue().size() == 0);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void userProcessingOccursBeforeOtherNondeleteProcessing() throws Exception {
    authorizablePostProcessService.process(user, session, ModificationType.MODIFY);
    InOrder inOrder = inOrder(sakaiUserProcessor, authorizablePostProcessor);
    inOrder.verify(sakaiUserProcessor).process(eq(user), eq(session), any(Modification.class), any(Map.class));
    verify(sakaiGroupProcessor, never()).process(eq(user), eq(session), any(Modification.class), any(Map.class));
    inOrder.verify(authorizablePostProcessor).process(eq(user), eq(session), any(Modification.class), any(Map.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void userProcessingOccursAfterOtherDeleteProcessing() throws Exception {
    authorizablePostProcessService.process(user, session, ModificationType.DELETE);
    InOrder inOrder = inOrder(authorizablePostProcessor, sakaiUserProcessor);
    inOrder.verify(authorizablePostProcessor).process(eq(user), eq(session), any(Modification.class), any(Map.class));
    inOrder.verify(sakaiUserProcessor).process(eq(user), eq(session), any(Modification.class), any(Map.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void groupProcessingOccurs() throws Exception {
    authorizablePostProcessService.process(group, session, ModificationType.MODIFY);
    verify(sakaiUserProcessor, never()).process(eq(group), eq(session), any(Modification.class), any(Map.class));
    verify(sakaiGroupProcessor).process(eq(group), eq(session), any(Modification.class), any(Map.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void modificationArgumentContainsUserPath() throws Exception {
    ArgumentCaptor<Modification> modificationArgument = ArgumentCaptor.forClass(Modification.class);
    authorizablePostProcessService.process(user, session, ModificationType.CREATE);
    verify(authorizablePostProcessor).process(eq(user), eq(session), modificationArgument.capture(), any(Map.class));
    Modification modification = modificationArgument.getValue();
    assertEquals(ModificationType.CREATE, modification.getType());
    assertEquals(UserConstants.SYSTEM_USER_MANAGER_USER_PREFIX + "joe", modification.getSource());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void modificationArgumentContainsGroupPath() throws Exception {
    ArgumentCaptor<Modification> modificationArgument = ArgumentCaptor.forClass(Modification.class);
    authorizablePostProcessService.process(group, session, ModificationType.CREATE);
    verify(authorizablePostProcessor).process(eq(group), eq(session), modificationArgument.capture(), any(Map.class));
    Modification modification = modificationArgument.getValue();
    assertEquals(ModificationType.CREATE, modification.getType());
    assertEquals(UserConstants.SYSTEM_USER_MANAGER_GROUP_PREFIX + "faculty", modification.getSource());
  }
}
