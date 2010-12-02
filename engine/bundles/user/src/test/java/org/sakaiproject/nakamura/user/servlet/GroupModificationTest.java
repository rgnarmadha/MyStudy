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
import org.junit.Test;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;


/**
 *
 */
public class GroupModificationTest extends AbstractEasyMockTest {
  @Test
  public void testOnJoin() {
    
    Group mainGroup = createNiceMock(Group.class);
    Group group = createNiceMock(Group.class);
    
    replay();
    GroupModification gm = (GroupModification) GroupModification.onGroupJoin("/somepath", mainGroup, group);
    Assert.assertEquals(mainGroup, gm.getMainGroup());
    Assert.assertEquals(group, gm.getGroup());
    Assert.assertTrue(gm.isJoin());
    verify();
  }
  @Test
  public void testOnLeave() {
    
    Group mainGroup = createNiceMock(Group.class);
    Group group = createNiceMock(Group.class);
    
    replay();
    GroupModification gm = (GroupModification) GroupModification.onGroupLeave("/somepath", mainGroup, group);
    Assert.assertEquals(mainGroup, gm.getMainGroup());
    Assert.assertEquals(group, gm.getGroup());
    Assert.assertFalse(gm.isJoin());
    verify();
  }
  
}
