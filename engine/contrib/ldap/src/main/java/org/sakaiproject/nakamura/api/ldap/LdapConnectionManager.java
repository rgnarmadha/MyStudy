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
package org.sakaiproject.nakamura.api.ldap;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPException;

/**
 * Implementations manage <code>LDAPConnection</code> allocation.
 * 
 * @see LdapConnectionManager
 * @author Dan McCallum, Unicon Inc
 * @author John Lewis, Unicon Inc
 * 
 */
public interface LdapConnectionManager {

  /**
   * Retrieve an <code>LDAPConnection</code> -- the connection may already be bound
   * depending on the configuration
   * 
   * @return a connected <code>LDAPConnection</code>
   * @throws LDAPException
   *           if the <code>LDAPConnection</code> allocation fails
   */
  LDAPConnection getConnection() throws LDAPException;

  /**
   * Retrieve a bound <code>LDAPConnection</code> using the indicated credentials. If null
   * is passed for the dn, the default dn and password will be used mimicking
   * getConnection() with autobind = true.
   * 
   * @param dn
   *          The distinguished name for binding.
   * @param pass
   *          the password for binding
   * @return a connected <code>LDAPConnection</code>
   * @throws LDAPException
   *           if the <code>LDAPConnection</code> allocation fails
   */
  LDAPConnection getBoundConnection(String dn, String pass) throws LDAPException;

  /**
   * Return an <code>LDAPConnection</code>. This can allow for connections to be pooled
   * instead of just destroyed.
   * 
   * @param conn
   *          an <code>LDAPConnection</code> that you no longer need
   */
  void returnConnection(LDAPConnection conn);

  /**
   * Retrieve the currently assigned {@link LdapConnectionManagerConfig}.
   * 
   * @return the currently assigned {@link LdapConnectionManagerConfig}, if any
   */
  LdapConnectionManagerConfig getConfig();
}
