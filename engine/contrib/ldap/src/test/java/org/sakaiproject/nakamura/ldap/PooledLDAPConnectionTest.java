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

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPException;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManager;

/**
 * Verifies (a subset of) extensions to {@link LDAPConnection} behaviors.
 *
 * @author Dan McCallum (dmccallum@unicon.net)
 *
 */
public class PooledLDAPConnectionTest {

  private PooledLDAPConnection conn;
  private LdapConnectionManager connMgr;

  @Before
  public void setUp() throws Exception {
    conn = new PooledLDAPConnection();
    connMgr = createMock(LdapConnectionManager.class);
    conn.setConnectionManager(connMgr);
  }

  /**
   * Verifies that active {@link PooledLDAPConnection}s are returned to the
   * assigned {@link LdapConnectionManager}.
   *
   * @see #testFinalizeAbandonsInactiveConnections() for handling of inactive
   *      connections
   * @throws LDAPException
   *           test error
   */
  @Test
  @SuppressWarnings(justification="Why are we calling finalize ? Is it for testing purposes ?", value={"FI_EXPLICIT_INVOCATION"})
  public void testFinalizeReturnsActiveConnectionToTheConnectionManager() throws LDAPException {
    conn.setActive(true);
    connMgr.returnConnection(conn);
    expectLastCall();
    replay(connMgr);
    conn.finalize();
  }

  /**
   * Verifies the inverse of
   * {@link #testFinalizeReturnsActiveConnectionToTheConnectionManager()}
   *
   * @throws LDAPException
   *           test error
   */
  @Test
  @SuppressWarnings(justification="Why are we calling finalize ? Is it for testing purposes ?", value={"FI_EXPLICIT_INVOCATION"})
  public void testFinalizeDoesNotReturnInactiveConnectionToTheConnectionManager()
      throws LDAPException {
    conn.setActive(false); // just to be sure
    conn.finalize();
    // rely on jMock to refuse any calls to the connection mgr
  }

  @Test
  @SuppressWarnings(justification="Why are we calling finalize ? Is it for testing purposes ?", value={"FI_EXPLICIT_INVOCATION"})
  public void testFinalizeNullConnectionManager() throws LDAPException {
    conn.setActive(true);
    conn.setConnectionManager(null);
    conn.finalize();
  }

  @Test
  public void testSetsGets() {
    conn.setActive(false);
    assertFalse(conn.isActive());

    conn.setBindAttempted(true);
    assertTrue(conn.isBindAttempted());

    assertNotNull(conn.getBirthdate());

    assertNotNull(conn.getConnectionManager());
  }
}
