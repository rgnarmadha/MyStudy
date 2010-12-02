/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006 The Sakai Foundation.
 *
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.opensource.org/licenses/ecl1.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.nakamura.ldap.liveness;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.replay;

import com.novell.ldap.LDAPConnection;

import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.ldap.PooledLDAPConnection;
import org.sakaiproject.nakamura.ldap.liveness.NativeLdapConnectionLivenessValidator;

public class NativeLdapConnectionLivenessValidatorTest {

	private NativeLdapConnectionLivenessValidator validator;
	private LDAPConnection conn;

  @Before
  public void setUp() {
		validator = new NativeLdapConnectionLivenessValidator();
    conn = EasyMock.createMock(PooledLDAPConnection.class);
	}

  @Test
	public void testDelegatesLivenessTestToConnectionsOwnLivenessTest() {
    expect(conn.isConnectionAlive()).andReturn(true);
    replay(conn);
    assertTrue(validator.isConnectionAlive(conn));
	}

  @Test
	public void testDelegatesLivenessTestToConnectionsOwnLivenessTest_Negative() {
    expect(conn.isConnectionAlive()).andReturn(false);
    replay(conn);
		assertFalse(validator.isConnectionAlive(conn));
	}


}
