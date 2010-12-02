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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.fail;

import com.novell.ldap.LDAPConnection;

import org.apache.commons.pool.ObjectPool;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManagerConfig;

public class PoolingLdapConnectionManagerTest {

  private ObjectPool pool;
  private LdapConnectionManagerConfig config;
  private PoolingLdapConnectionManager poolingConnMgr;

  @Before
  public void setUp() throws Exception {
    pool = createMock(ObjectPool.class);
    pool.close();

    config = new LdapConnectionManagerConfig();
    // some white box awkwardness
    config.setSecureConnection(false);

    poolingConnMgr = new PoolingLdapConnectionManager() {
      org.apache.commons.pool.ObjectPool newConnectionPool(
          org.apache.commons.pool.PoolableObjectFactory factory, int maxConns, byte whenExhausted,
          int maxWait, int maxIdle, boolean testOnBorrow, boolean testOnReturn) {
        return pool;
      };
    };
  }

  @Test
  public void testDoesNotReturnNullReferencesToPool() throws Exception {
    replay(pool);
    poolingConnMgr.init(config);
    // mockPool will throw a fit if any method called
    poolingConnMgr.returnConnection(null);
  }

  @Test
  public void testReturnConnectionException() throws Exception {
    expect(pool.borrowObject()).andReturn(new LDAPConnection());

    pool.returnObject(anyObject());
    expectLastCall().andThrow(new Exception());

    replay(pool);
    poolingConnMgr.init(config);

    LDAPConnection conn = poolingConnMgr.getConnection();
    try {
      poolingConnMgr.returnConnection(conn);
      fail("Should throw an exception if can't return object.");
    } catch (RuntimeException e) {
      // expected
    }
  }
}
