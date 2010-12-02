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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.user.UserConstants;

import java.util.HashMap;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

@RunWith(MockitoJUnitRunner.class)
public class SakaiUserProcessorTest {
  private SakaiUserProcessor sakaiUserProcessor;
  @Mock
  private JackrabbitSession session;
  @Mock
  private User user;
  @Mock
  private ItemBasedPrincipal principal;
  @Mock
  private ValueFactory valueFactory;
  @Mock
  private Value pathValue;

  @Before
  public void setUp() throws RepositoryException {
    sakaiUserProcessor = new SakaiUserProcessor();
    when(user.getPrincipal()).thenReturn(principal);
    when(principal.getPath()).thenReturn(UserConstants.USER_REPO_LOCATION + "/joe");
    when(session.getValueFactory()).thenReturn(valueFactory);
    when(valueFactory.createValue("/joe")).thenReturn(pathValue);
  }

  @Test
  public void pathIsSetOnCreation() throws Exception {
    sakaiUserProcessor.process(user, session, new Modification(ModificationType.CREATE, "", ""), new HashMap<String, Object[]>());
    verify(user).setProperty(UserConstants.PROP_AUTHORIZABLE_PATH, pathValue);
  }

  @Test
  public void pathIsNotOverwritten() throws Exception {
    when(user.hasProperty(UserConstants.PROP_AUTHORIZABLE_PATH)).thenReturn(true);
    sakaiUserProcessor.process(user, session, new Modification(ModificationType.CREATE, "", ""), new HashMap<String, Object[]>());
    verify(user, never()).setProperty(UserConstants.PROP_AUTHORIZABLE_PATH, pathValue);
  }

  @Test
  public void pathIsLeftAloneOnDeletion() throws Exception {
    sakaiUserProcessor.process(user, session, new Modification(ModificationType.DELETE, "", ""), new HashMap<String, Object[]>());
    verify(user, never()).setProperty(UserConstants.PROP_AUTHORIZABLE_PATH, pathValue);
  }

}
