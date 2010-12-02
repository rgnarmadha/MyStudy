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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.user.UserConstants;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

@RunWith(MockitoJUnitRunner.class)
public class SakaiGroupProcessorTest {
  private SakaiGroupProcessor sakaiGroupProcessor;
  @Mock
  private JackrabbitSession session;
  @Mock
  private Group group;
  @Mock
  private ItemBasedPrincipal principal;
  @Mock
  private ValueFactory valueFactory;
  @Mock
  private Value pathValue;
  @Mock
  private UserManager userManager;
  @Mock
  private Group managersGroup;

  @Before
  public void setUp() throws RepositoryException {
    sakaiGroupProcessor = new SakaiGroupProcessor();
    when(group.isGroup()).thenReturn(true);
    when(group.getID()).thenReturn("faculty");
    when(group.getPrincipal()).thenReturn(principal);
    when(principal.getPath()).thenReturn(UserConstants.GROUP_REPO_LOCATION + "/faculty");
    when(session.getValueFactory()).thenReturn(valueFactory);
    when(valueFactory.createValue("/faculty")).thenReturn(pathValue);
    when(session.getUserManager()).thenReturn(userManager);
    when(userManager.createGroup(any(Principal.class))).thenReturn(managersGroup);
  }

  @Test
  public void managersGroupCreated() throws Exception {
    sakaiGroupProcessor.process(group, session, new Modification(ModificationType.CREATE, "", ""),
        new HashMap<String, Object[]>());
    verify(userManager).createGroup(any(Principal.class));
    verify(group).setProperty(eq(UserConstants.PROP_GROUP_MANAGERS), any(Value[].class));
    verify(group).setProperty(eq(UserConstants.PROP_MANAGERS_GROUP), any(Value.class));
    verify(group).addMember(managersGroup);
    verify(managersGroup).setProperty(eq(UserConstants.PROP_GROUP_MANAGERS), any(Value[].class));
    verify(managersGroup).setProperty(eq(UserConstants.PROP_MANAGED_GROUP), any(Value.class));
  }

  @Test
  public void canModifyManagersMembership() throws Exception {
    when(group.hasProperty(UserConstants.PROP_MANAGERS_GROUP)).thenReturn(true);
    Value mgrsValue = mock(Value.class);
    when(mgrsValue.getString()).thenReturn("mgrs");
    when(group.getProperty(UserConstants.PROP_MANAGERS_GROUP)).thenReturn(new Value[] {mgrsValue});
    when(userManager.getAuthorizable("mgrs")).thenReturn(managersGroup);
    User jane = mock(User.class);
    when (userManager.getAuthorizable("jane")).thenReturn(jane);
    User jean = mock(User.class);
    when (userManager.getAuthorizable("jean")).thenReturn(jean);
    User joe = mock(User.class);
    when (userManager.getAuthorizable("joe")).thenReturn(joe);
    User jim = mock(User.class);
    when (userManager.getAuthorizable("jim")).thenReturn(jim);
    Map<String, Object[]> parameters = new HashMap<String, Object[]>();
    parameters.put(SakaiGroupProcessor.PARAM_ADD_TO_MANAGERS_GROUP,
        new String[] {"jane", "jean"});
    parameters.put(SakaiGroupProcessor.PARAM_REMOVE_FROM_MANAGERS_GROUP,
        new String[] {"joe", "jim"});
    sakaiGroupProcessor.process(group, session, new Modification(ModificationType.MODIFY, "", ""),
        parameters);
    verify(managersGroup).addMember(jane);
    verify(managersGroup).addMember(jean);
    verify(managersGroup).removeMember(joe);
    verify(managersGroup).removeMember(jim);
  }

  @Test
  public void existingRightsArePreserved() throws Exception {
    when(group.hasProperty(UserConstants.PROP_GROUP_MANAGERS)).thenReturn(true);
    when(group.hasProperty(UserConstants.PROP_GROUP_VIEWERS)).thenReturn(true);
    Value accessValue = mock(Value.class);
    when(accessValue.getString()).thenReturn("thecreator");
    Value[] accessValues = new Value[] {accessValue};
    when(group.getProperty(UserConstants.PROP_GROUP_MANAGERS)).thenReturn(accessValues);
    sakaiGroupProcessor.process(group, session, new Modification(ModificationType.CREATE, "", ""),
        new HashMap<String, Object[]>());
    ArgumentCaptor<Value[]> managerAccessArgument = ArgumentCaptor.forClass(Value[].class);
    verify(group).setProperty(eq(UserConstants.PROP_GROUP_MANAGERS), managerAccessArgument.capture());
    Value[] newAccessValues = managerAccessArgument.getValue();
    assertEquals(2, newAccessValues.length);
    for (Value value : newAccessValues) {
      String stringValue = value.getString();
      if (stringValue != null) {
        if ("thecreator".equals(stringValue)) {
          return;
        }
      }
    }
    fail("Did not find the orginal access setting");
  }

  @Test
  public void managersGroupDeleted() throws Exception {
    when(group.hasProperty(UserConstants.PROP_MANAGERS_GROUP)).thenReturn(true);
    Value mgrsValue = mock(Value.class);
    when(mgrsValue.getString()).thenReturn("mgrs");
    when(group.getProperty(UserConstants.PROP_MANAGERS_GROUP)).thenReturn(new Value[] {mgrsValue});
    when(userManager.getAuthorizable("mgrs")).thenReturn(managersGroup);
    sakaiGroupProcessor.process(group, session, new Modification(ModificationType.DELETE, "", ""),
        new HashMap<String, Object[]>());
    verify(managersGroup).remove();
  }

  @Test
  public void pathIsSetOnCreation() throws Exception {
    sakaiGroupProcessor.process(group, session, new Modification(ModificationType.CREATE, "", ""), new HashMap<String, Object[]>());
    verify(group).setProperty(UserConstants.PROP_AUTHORIZABLE_PATH, pathValue);
  }

  @Test
  public void pathIsNotOverwritten() throws Exception {
    when(group.hasProperty(UserConstants.PROP_AUTHORIZABLE_PATH)).thenReturn(true);
    sakaiGroupProcessor.process(group, session, new Modification(ModificationType.CREATE, "", ""), new HashMap<String, Object[]>());
    verify(group, never()).setProperty(UserConstants.PROP_AUTHORIZABLE_PATH, pathValue);
  }

  @Test
  public void pathIsLeftAloneOnDeletion() throws Exception {
    sakaiGroupProcessor.process(group, session, new Modification(ModificationType.DELETE, "", ""), new HashMap<String, Object[]>());
    verify(group, never()).setProperty(UserConstants.PROP_AUTHORIZABLE_PATH, pathValue);
  }

}
