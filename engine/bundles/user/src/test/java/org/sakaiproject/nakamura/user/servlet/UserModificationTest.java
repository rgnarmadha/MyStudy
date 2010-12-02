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

import junit.framework.Assert;

import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.junit.Test;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;


/**
 *
 */
public class UserModificationTest extends AbstractEasyMockTest {
  @Test
  public void testOnJoin() {
    
    Group mainGroup = createNiceMock(Group.class);
    User user = createNiceMock(User.class);
    
    replay();
    UserModification gm = (UserModification) UserModification.onGroupJoin("/somepath", mainGroup, user);
    Assert.assertEquals(mainGroup, gm.getGroup());
    Assert.assertEquals(user, gm.getUser());
    Assert.assertTrue(gm.isJoin());
    Assert.assertNotNull(gm.toString());
    verify();
  }
  @Test
  public void testOnLeave() {
    
    Group mainGroup = createNiceMock(Group.class);
    User user = createNiceMock(User.class);
    
    replay();
    UserModification gm = (UserModification) UserModification.onGroupLeave("/somepath", mainGroup, user);
    Assert.assertEquals(mainGroup, gm.getGroup());
    Assert.assertEquals(user, gm.getUser());
    Assert.assertFalse(gm.isJoin());
    Assert.assertNotNull(gm.toString());
    verify();
  }
  
}
