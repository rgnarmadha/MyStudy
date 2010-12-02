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
package org.sakaiproject.nakamura.api.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.novell.ldap.LDAPConnection;

import org.junit.Test;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManagerConfig;

public class LdapConnectionManagerConfigTest {
  @Test
  public void testDefaultConstructor() {
    LdapConnectionManagerConfig config = new LdapConnectionManagerConfig();

    assertNull(config.getKeystoreLocation());

    assertNull(config.getKeystorePassword());

    assertNull(config.getLdapHost());

    assertNull(config.getLdapPassword());

    assertEquals(LDAPConnection.DEFAULT_PORT, config.getLdapPort());

    assertNull(config.getLdapUser());

    assertEquals(5000, config.getOperationTimeout());

    assertEquals(10, config.getPoolMaxConns());

    assertFalse(config.isAutoBind());

    assertFalse(config.isFollowReferrals());

    assertTrue(config.isPooling());

    assertFalse(config.isSecureConnection());

    assertFalse(config.isTLS());
  }

  @Test
  public void testParameterizedConstructor() {
    LdapConnectionManagerConfig config = new LdapConnectionManagerConfig(true, true,
        "someKeystoreLocation", "someKeystorePassword", Integer.MAX_VALUE, "someLdapHost",
        LDAPConnection.DEFAULT_SSL_PORT, "someLdapUser", "someLdapPassword", true, true, true,
        Integer.MAX_VALUE);
    assertTrue(config.isAutoBind());
    assertTrue(config.isFollowReferrals());
    assertEquals("someKeystoreLocation", config.getKeystoreLocation());
    assertEquals("someKeystorePassword", config.getKeystorePassword());
    assertEquals("someLdapHost", config.getLdapHost());
    assertEquals("someLdapPassword", config.getLdapPassword());
    assertEquals(LDAPConnection.DEFAULT_SSL_PORT, config.getLdapPort());
    assertEquals("someLdapUser", config.getLdapUser());
    assertEquals(Integer.MAX_VALUE, config.getOperationTimeout());
    assertTrue(config.isPooling());
    assertEquals(Integer.MAX_VALUE, config.getPoolMaxConns());
    assertTrue(config.isSecureConnection());
    assertTrue(config.isTLS());
  }

  @Test
  public void testSetGet() {
    LdapConnectionManagerConfig config = new LdapConnectionManagerConfig();

    config.setAutoBind(true);
    assertTrue(config.isAutoBind());
    config.setAutoBind(false);
    assertFalse(config.isAutoBind());

    config.setFollowReferrals(true);
    assertTrue(config.isFollowReferrals());
    config.setFollowReferrals(false);
    assertFalse(config.isFollowReferrals());

    config.setKeystoreLocation("someKeystoreLocation");
    assertEquals("someKeystoreLocation", config.getKeystoreLocation());
    config.setKeystoreLocation(null);
    assertNull(config.getKeystoreLocation());

    config.setKeystorePassword("someKeystorePassword");
    assertEquals("someKeystorePassword", config.getKeystorePassword());
    config.setKeystorePassword(null);
    assertNull(config.getKeystorePassword());

    config.setLdapHost("someLdapHost");
    assertEquals("someLdapHost", config.getLdapHost());
    config.setLdapHost(null);
    assertNull(config.getLdapHost());

    config.setLdapPassword("someLdapPassword");
    assertEquals("someLdapPassword", config.getLdapPassword());
    config.setLdapPassword(null);
    assertNull(config.getLdapPassword());

    config.setLdapPort(LDAPConnection.DEFAULT_SSL_PORT);
    assertEquals(LDAPConnection.DEFAULT_SSL_PORT, config.getLdapPort());
    config.setLdapPort(-1000);
    assertEquals(LDAPConnection.DEFAULT_PORT, config.getLdapPort());

    config.setLdapUser("someLdapUser");
    assertEquals("someLdapUser", config.getLdapUser());
    config.setLdapUser(null);
    assertNull(config.getLdapUser());

    config.setOperationTimeout(Integer.MAX_VALUE);
    assertEquals(Integer.MAX_VALUE, config.getOperationTimeout());
    config.setOperationTimeout(Integer.MIN_VALUE);
    assertEquals(Integer.MIN_VALUE, config.getOperationTimeout());

    config.setPooling(true);
    assertTrue(config.isPooling());
    config.setPooling(false);
    assertFalse(config.isPooling());

    config.setPoolMaxConns(Integer.MAX_VALUE);
    assertEquals(Integer.MAX_VALUE, config.getPoolMaxConns());
    config.setPoolMaxConns(Integer.MIN_VALUE);
    assertEquals(Integer.MIN_VALUE, config.getPoolMaxConns());

    config.setSecureConnection(true);
    assertTrue(config.isSecureConnection());
    config.setSecureConnection(false);
    assertFalse(config.isSecureConnection());

    config.setTLS(true);
    assertTrue(config.isTLS());
    config.setTLS(false);
    assertFalse(config.isTLS());
  }
}
