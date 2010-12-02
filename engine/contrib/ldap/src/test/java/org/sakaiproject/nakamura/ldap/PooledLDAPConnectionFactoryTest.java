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
package org.sakaiproject.nakamura.ldap;

import static org.easymock.EasyMock.isA;

import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPConstraints;
import com.novell.ldap.LDAPException;

import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionLivenessValidator;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManager;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManagerConfig;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

/**
 * Currently verifies a subset of {@link PooledLDAPConnectionFactory} features.
 * Specifically, is concerned with verifying fixes to object validation which
 * were causing stale connections to remain in the pool indefinitely.
 *
 *
 * @author Dan McCallum (dmccallum@unicon.net)
 * @author "Carl Hall" <carl.hall@gatech.edu>
 */
public class PooledLDAPConnectionFactoryTest {

  private PooledLDAPConnectionFactory factory;
  private PooledLDAPConnection conn;
  private LdapConnectionLivenessValidator livenessValidator;
  private LdapConnectionManager connMgr;
  private LdapConnectionManagerConfig connMgrConfig;

  @Before
  public void setUp() throws Exception {

    conn = createMock(PooledLDAPConnection.class);

    livenessValidator = createMock(LdapConnectionLivenessValidator.class);
    List<LdapConnectionLivenessValidator> validators = new LinkedList<LdapConnectionLivenessValidator>();
    validators.add(livenessValidator);

    connMgr = createMock(LdapConnectionManager.class);
    connMgrConfig = new LdapConnectionManagerConfig();
    setConnectionManagerConfigExpectations();
    
    factory = new PooledLDAPConnectionFactory(connMgr, validators) {
      @Override
      protected PooledLDAPConnection newConnection() {
        return conn;
      }
    };
    factory.setLivenessValidators(validators);
  }

  @Test
  public void testValidateNullObject() {
    assertFalse(factory.validateObject(null));
  }

  @Test
  public void testMakeObjectConnectsAndOtherwiseInitializesConnections() throws LDAPException,
      UnsupportedEncodingException {


    factory.setConnectionManager(connMgr);
    conn.setConnectionManager(connMgr);

    conn.setConstraints(isA(LDAPConstraints.class));
    expectLastCall().once();
    // make more specific
    conn.connect(connMgrConfig.getLdapHost(), connMgrConfig.getLdapPort());
    expectLastCall().once();
    conn.startTLS();
    expectLastCall().once();
    conn.bind(anyInt(), (String) anyObject(), (byte[]) anyObject());
    expectLastCall().once();
    conn.setBindAttempted(false);
    expectLastCall().once();

    replay(conn);

    // the actual code exercise
    assertEquals(conn, factory.makeObject());

    // TODO how to test socket factory assignment (static call)
  }

  private void setConnectionManagerConfigExpectations() {

    final String LDAP_HOST = "ldap-host";
    final int LDAP_PORT = 389;
    final String LDAP_USER = "ldap-user";
    final String LDAP_PASS = "ldap-pass";
    final int LDAP_TIMEOUT = 5000;
    final boolean LDAP_FOLLOW_REFERRALS = true;

    expect(connMgr.getConfig()).andReturn(connMgrConfig).anyTimes();
    connMgrConfig.setLdapHost(LDAP_HOST);
    connMgrConfig.setLdapHost(LDAP_HOST);
    connMgrConfig.setLdapPort(LDAP_PORT);
    connMgrConfig.setAutoBind(true);
    connMgrConfig.setLdapUser(LDAP_USER);
    connMgrConfig.setLdapPassword(LDAP_PASS);
    connMgrConfig.setSecureConnection(true);
    connMgrConfig.setOperationTimeout(LDAP_TIMEOUT);
    connMgrConfig.setFollowReferrals(LDAP_FOLLOW_REFERRALS);
    connMgrConfig.setTLS(false);
    
    replay(connMgr);
  }

  /**
   * Verifies that {@link PooledLDAPConnectionFactory#activateObject(Object)}
   * passes constraints and an active flag to the given
   * {@link PooledLDAPConnection}. Does not actually verify constraint values.
   *
   * @throws LDAPException
   *           test error
   */
  @Test
  public void testActivateObject() throws LDAPException {

    // TODO validate constraint assignment
    conn.setConstraints((LDAPConstraints) anyObject());
    expectLastCall().once();
    conn.setActive(true);
    expectLastCall().once();
    replay(conn);
    factory.activateObject(conn);

  }

  /**
   * If the client has bound the connection but the factory is not running in
   * auto-bind mode, the connection must be invalidated.
   */
  @Test
  public void testValidateObjectLowersActiveFlagIfConnectionHasBeenBoundButAutoBindIsNotSet() throws Exception {
    expect(conn.isBindAttempted()).andReturn(true);
    // we assume autoBind defaults to false (we'd rather not go through the
    // whole process of mocking up the connection manager again)
    conn.setActive(false);
    expectLastCall().once();
    conn.bind(anyInt(), isA(String.class), isA(byte[].class));
    expectLastCall().once();
    conn.setBindAttempted(false);
    expectLastCall().once();
    replay(conn);
    assertFalse(factory.validateObject(conn));
  }

  /**
   * Verifies that the {@link PooledLdapConnection} to be validated is re-bound
   * as the system user prior to testing the connection for liveness. This is
   * only relevant if the client has bound the connection as another user and
   * the autoBind behavior has been enabled.
   */
  @Test
  public void testValidateObjectRebindsAsAutoBindUserIfNecessaryPriorToTestingConnectionLiveness()
      throws Exception {

    factory.setConnectionManager(connMgr);

    expect(conn.isBindAttempted()).andReturn(true);
    conn.bind(anyInt(), (String) anyObject(), (byte[]) anyObject());
    expectLastCall().once();
    conn.setBindAttempted(false);
    expectLastCall().once();

    expect(livenessValidator.isConnectionAlive((LDAPConnection) anyObject())).andReturn(true);
    replay(conn, livenessValidator);
    assertTrue(factory.validateObject(conn));

  }

  /**
   * Verifies that a failed rebind during connection validation marks the
   * connection as "inactive". This ensures that the connection is not returned
   * to the pool by {@link PooledLDAPConnection#finalize()}. Technically, a
   * failed rebind does not mean the connection is stale but failing to bind as
   * the system user should indicate that something is very much wrong, so
   * reallocating a connection is not a bad idea. Certainly better than finding
   * oneself caught in an endless loop of validating stale connections.
   *
   */
  @Test
  public void testValidateObjectLowersActiveFlagIfRebindFails() throws Exception {

    factory.setConnectionManager(connMgr);
    LDAPException bindFailure = new LDAPException();

    expect(conn.isBindAttempted()).andReturn(true);
    conn.bind(anyInt(), (String) anyObject(), (byte[]) anyObject());
    expectLastCall().andThrow(bindFailure);
    conn.setActive(false);
    expectLastCall().once();
    replay(conn);

    assertFalse(factory.validateObject(conn));

  }

  /**
   * Verifies that {@link PooledLDAPConnectionFactory#validateObject(Object)}
   * does not adjust a {@link PooledLDAPConnection}'s active flag if the
   * connection appears to be "alive". Probably overkill, but we've had problems
   * that we think may be related to stale connections being returned to the
   * pool, so we want to be sure the validate operation is doing exactly what we
   * think it should be doing. Also verifies that a {@link PooledLDAPConnection}
   * 's search method returns a LDAPEntry.
   *
   * @throws LDAPException
   */
  @Test
  public void testValidateObjectKeepsActiveFlagUpIfConnectionIsAlive() throws LDAPException {

    // will fail if the factory attempts to monkey with the active flag
    expect(conn.isBindAttempted()).andReturn(false);
    expect(livenessValidator.isConnectionAlive((LDAPConnection) anyObject())).andReturn(true);
    replay(conn, livenessValidator);

    assertTrue(factory.validateObject(conn));

  }

  /**
   * Verifies that {@link PooledLDAPConnectionFactory#validateObject(Object)}
   * lowers {@link PooledLDAPConnection}'s active flag if the current
   * {@link LdapConnectionLivenessValidator} reports that the connection is not
   * live.
   *
   * @throws LDAPException
   *           test failure
   */
  @Test
  public void testValidateObjectLowersActiveFlagIfConnectionIsNotAlive() throws LDAPException {

    expect(conn.isBindAttempted()).andReturn(false);
    expect(livenessValidator.isConnectionAlive((LDAPConnection) anyObject())).andReturn(false);
    conn.setActive(false);
    expectLastCall().once();
    replay(conn, livenessValidator);
    assertFalse(factory.validateObject(conn));

  }

  @Test
  public void testValidateObjectLowersActiveFlagIfLivenessValidationThrowsException() {

    expect(conn.isBindAttempted()).andReturn(false);
    expect(livenessValidator.isConnectionAlive((LDAPConnection) anyObject())).andThrow(
        new RuntimeException("catch me"));
    conn.setActive(false);
    expectLastCall().once();
    replay(conn, livenessValidator);
    assertFalse(factory.validateObject(conn));

  }

  @Test
  public void testInvalidatesNullObjects() {

    assertFalse(factory.validateObject(null));

  }

}
