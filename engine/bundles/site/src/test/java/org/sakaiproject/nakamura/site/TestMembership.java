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
package org.sakaiproject.nakamura.site;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 *
 */
@RunWith(value = MockitoJUnitRunner.class)
public class TestMembership {
  @Mock
  private Authorizable parent;

  @Mock
  private Authorizable member;

  private Membership membership;

  @Before
  public void setUp() {
    membership = new Membership(parent, member);
  }

  @Test
  public void testHashCode() {
    int hashCode = parent.hashCode() + member.hashCode();
    assertEquals(hashCode, membership.hashCode());
  }

  @Test
  public void testGetParent() {
    assertEquals(parent, membership.getParent());
  }

  @Test
  public void testGetMember() {
    assertEquals(member, membership.getMember());
  }

  @Test
  public void testEquals() {
    assertTrue(membership.equals(membership));
  }

  @Test
  public void testNullEquals() {
    assertFalse(membership.equals(null));
  }

  @Test
  public void testNullParentEquals() {
    Membership m = new Membership(null, member);
    assertFalse(membership.equals(m));

    membership = new Membership(null, member);
    assertTrue(membership.equals(m));

    m = new Membership(parent, member);
    assertFalse(membership.equals(m));
  }
}
