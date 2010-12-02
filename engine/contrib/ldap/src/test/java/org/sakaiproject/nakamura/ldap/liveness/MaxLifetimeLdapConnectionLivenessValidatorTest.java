/*
 * Licensed to the Sakai Foundation (SF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The SF licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.sakaiproject.nakamura.ldap.liveness;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.novell.ldap.LDAPConnection;

import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.ldap.PooledLDAPConnection;
import org.sakaiproject.nakamura.ldap.liveness.MaxLifetimeLdapConnectionLivenessValidator;

import java.util.Calendar;

public class MaxLifetimeLdapConnectionLivenessValidatorTest {
  private MaxLifetimeLdapConnectionLivenessValidator validator;
  private PooledLDAPConnection conn;

  @Before
  public void setUp() {
    validator = new MaxLifetimeLdapConnectionLivenessValidator();
    conn = createMock(PooledLDAPConnection.class);
  }

  @Test
  public void testInvalidatesLongLivedConnection() {
    final long ONE_HOUR = 1000 * 60 * 60;
    validator.setMaxTtl(ONE_HOUR);
    Calendar connBirthdateCalendar = Calendar.getInstance();
    connBirthdateCalendar.add(Calendar.HOUR_OF_DAY, -2);
    final long CONN_BIRTHDATE = connBirthdateCalendar.getTimeInMillis();
    // mockConn.expects(once()).method("getBirthdate").will(returnValue(CONN_BIRTHDATE));
    expect(conn.getBirthdate()).andReturn(CONN_BIRTHDATE);
    replay(conn);
    assertFalse(validator.isConnectionAlive(conn));
  }

  @Test
  public void testTreatsZeroTtlAsInfinite() {
    Calendar epoch = Calendar.getInstance();
    epoch.setTimeInMillis(0L); // highly unlikely this will fall within the TTL
    validator.setMaxTtl(0);
    replay(conn);
    assertTrue(validator.isConnectionAlive(conn));
  }

  @Test
  public void testTreatsNegativeTtlAsInfinite() {
    Calendar epoch = Calendar.getInstance();
    epoch.setTimeInMillis(0L); // highly unlikely this will fall within the TTL
    validator.setMaxTtl(-1);
    replay(conn);
    assertTrue(validator.isConnectionAlive(conn));
  }

  @Test
  public void testDefaultsToInifiniteTtl() {
    Calendar epoch = Calendar.getInstance();
    epoch.setTimeInMillis(0L); // highly unlikely this will fall within the TTL
    validator.setMaxTtl(-1);
    replay(conn);
    assertTrue(validator.isConnectionAlive(conn));
  }

  @Test
  public void testAlwaysValidatesUnpooledLDAPConnections() {
    LDAPConnection conn = new LDAPConnection();
    assertTrue(validator.isConnectionAlive(conn));
  }

}
