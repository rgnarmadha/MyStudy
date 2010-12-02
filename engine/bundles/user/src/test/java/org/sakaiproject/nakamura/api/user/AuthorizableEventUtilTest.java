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
package org.sakaiproject.nakamura.api.user;

import junit.framework.Assert;

import org.apache.jackrabbit.api.security.user.Group;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.easymock.EasyMock;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.sakaiproject.nakamura.api.user.AuthorizableEvent.Operation;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;
import org.sakaiproject.nakamura.user.servlet.GroupModification;
import org.sakaiproject.nakamura.user.servlet.UserModification;

import javax.jcr.RepositoryException;


/**
 *
 */
public class AuthorizableEventUtilTest extends AbstractEasyMockTest {

  @Test
  public void testIsAuthorizableModification() {
    Modification m = new Modification(ModificationType.CREATE, "/source", "/destination");
    Assert.assertFalse(AuthorizableEventUtil.isAuthorizableModification(m));
    Assert.assertTrue(AuthorizableEventUtil.isAuthorizableModification(new GroupModification(ModificationType.CREATE, "/source", "/dest")));
    Assert.assertTrue(AuthorizableEventUtil.isAuthorizableModification(new UserModification(ModificationType.CREATE, "/source", "/dest")));
  }
  
  @Test
  public void testNewAuthorizableEvent() throws RepositoryException {
    Event e = AuthorizableEventUtil.newAuthorizableEvent(Operation.create, "ieb", "pieb", new UserModification(ModificationType.CREATE, "/source", "/dest"));
    Assert.assertEquals(Operation.create.getTopic(), e.getTopic());
    Assert.assertEquals("ieb", e.getProperty(AuthorizableEvent.USER));
    Assert.assertEquals("pieb", e.getProperty(AuthorizableEvent.PRINCIPAL_NAME));
    Assert.assertEquals(Operation.create, e.getProperty(AuthorizableEvent.OPERATION));
    e = AuthorizableEventUtil.newAuthorizableEvent(null, "ieb", "pieb", new UserModification(ModificationType.CREATE, "/source", "/dest"));
    Assert.assertEquals(Operation.unknown.getTopic(), e.getTopic());
    Assert.assertEquals("ieb", e.getProperty(AuthorizableEvent.USER));
    Assert.assertEquals("pieb", e.getProperty(AuthorizableEvent.PRINCIPAL_NAME));
    Assert.assertEquals(null, e.getProperty(AuthorizableEvent.OPERATION));

  }
  
  @Test
  public void testNewGroupEvent() throws RepositoryException {
    GroupModification gm = new GroupModification(ModificationType.CREATE, "/src", "/dest");
    Group group = createNiceMock(Group.class);
    EasyMock.expect(group.getID()).andReturn("g-ieb");
    replay();
    gm.setGroup(group);
    AuthorizableEventUtil.newGroupEvent("ieb", gm);
    verify();
  }
  
}
