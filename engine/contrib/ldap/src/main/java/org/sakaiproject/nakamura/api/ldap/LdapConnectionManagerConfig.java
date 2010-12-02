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

/**
 * Bean for collecting {@link LdapConnectionManager} configuration.
 */
public class LdapConnectionManagerConfig {
  /** Whether to create secure connections. */
  private boolean secureConnection = false;

  /** Where to connect using TLS. */
  private boolean tls;

  /** Where the keystore is located. */
  private String keystoreLocation;

  /** The password to the keystore. */
  private String keystorePassword;

  /** Timeout (in milliseconds) for an operation. */
  private int operationTimeout = 5000;

  /** The host to which to connect. */
  private String ldapHost;

  /** The port on which to connect. */
  private int ldapPort = LDAPConnection.DEFAULT_PORT;

  /** The user/account to use for connections. */
  private String loginUser;

  /** The password to use for connection logins. */
  private String loginPassword;

  /** Whether to follow referrals. */
  private boolean followReferrals;

  /** Whether to connection allocation should include a bind attempt. */
  private boolean autoBind;

  /** Whether connection pooling is used. */
  private boolean pooling = true;

  /** Maximum number of connections to allow, if pooling is used. */
  private int poolMaxConns = 10;

  public LdapConnectionManagerConfig() {
  }

  public LdapConnectionManagerConfig(boolean secureConnection, boolean tls,
      String keystoreLocation, String keystorePassword, int operationTimeout, String ldapHost,
      int ldapPort, String loginUser, String loginPassword, boolean followReferrals,
      boolean autoBind, boolean pooling, int poolMaxConns) {
    this.secureConnection = secureConnection;
    this.tls = tls;
    this.keystoreLocation = keystoreLocation;
    this.keystorePassword = keystorePassword;
    this.operationTimeout = operationTimeout;
    this.ldapHost = ldapHost;
    this.ldapPort = ldapPort;
    this.loginUser = loginUser;
    this.loginPassword = loginPassword;
    this.followReferrals = followReferrals;
    this.autoBind = autoBind;
    this.pooling = pooling;
    this.poolMaxConns = poolMaxConns;
  }

  public LdapConnectionManagerConfig copy() {
    LdapConnectionManagerConfig config = new LdapConnectionManagerConfig(secureConnection, tls,
        keystoreLocation, keystorePassword, operationTimeout, ldapHost, ldapPort, loginUser,
        loginPassword, followReferrals, autoBind, pooling, poolMaxConns);
    return config;
  }

  /**
   * If <code>true</code>, connect to LDAP over a secure protocol.
   */
  public boolean isSecureConnection() {
    return secureConnection;
  }

  /**
   * Set to <code>true</code> if LDAP connections should occur over a secure
   * protocol.
   */
  public void setSecureConnection(boolean secureConnection) {
    this.secureConnection = secureConnection;
  }

  public boolean isTLS() {
    return tls;
  }

  public void setTLS(boolean tls) {
    this.tls = tls;
  }

  /**
   * @return location of a SSL keystore
   */
  public String getKeystoreLocation() {
    return keystoreLocation;
  }

  /**
   * @param keystoreLocation
   *          the location of an SSL keystore
   */
  public void setKeystoreLocation(String keystoreLocation) {
    this.keystoreLocation = keystoreLocation;
  }

  /**
   * @return SSL keystore password
   */
  public String getKeystorePassword() {
    return keystorePassword;
  }

  /**
   * @param keystoreLocation
   *          SSL keystore password
   */
  public void setKeystorePassword(String keystorePassword) {
    this.keystorePassword = keystorePassword;
  }

  /**
   * @return the directory operation timeout
   */
  public int getOperationTimeout() {
    return operationTimeout;
  }

  /**
   * @param operationTimeout
   *          the directory operation timeout to set.
   */
  public void setOperationTimeout(int operationTimeout) {
    this.operationTimeout = operationTimeout;
  }

  /**
   * @return the LDAP host address or name.
   */
  public String getLdapHost() {
    return ldapHost;
  }

  /**
   * @param ldapHost
   *          The LDAP host address or name.
   */
  public void setLdapHost(String ldapHost) {
    this.ldapHost = ldapHost;
  }

  /**
   * @return the LDAP connection port.
   */
  public int getLdapPort() {
    return ldapPort;
  }

  /**
   * @param ldapPort
   *          The LDAP connection port to set.
   */
  public void setLdapPort(int ldapPort) {
    if (ldapPort <= 0) {
      if (secureConnection) {
        this.ldapPort = LDAPConnection.DEFAULT_SSL_PORT;
      } else {
        this.ldapPort = LDAPConnection.DEFAULT_PORT;
      }
    } else {
      this.ldapPort = ldapPort;
    }
  }

  /**
   * @return the LDAP user to bind as, typically a manager acct.
   */
  public String getLdapUser() {
    return loginUser;
  }

  /**
   * @param ldapUser
   *          The user to bind to LDAP as, typically a manager acct, leave blank
   *          for anonymous.
   */
  public void setLdapUser(String ldapUser) {
    this.loginUser = ldapUser;
  }

  /**
   * @see #getLdapUser()
   * @return Returns the LDAP password corresponding to the current default
   *         bind-as user.
   */
  public String getLdapPassword() {
    return loginPassword;
  }

  /**
   * @param ldapPassword
   *          the LDAP password corresponding to the current default bind-as
   *          user.
   */
  public void setLdapPassword(String ldapPassword) {
    this.loginPassword = ldapPassword;
  }

  /**
   * Access LDAP referral following configuration
   *
   * @return if <code>true</code>, directory accesses will follow referrals
   */
  public boolean isFollowReferrals() {
    return followReferrals;
  }

  /**
   * Configures LDAP referral following
   *
   * @param followReferrals
   *          if <code>true</code>, directory accesses will follow referrals
   */
  public void setFollowReferrals(boolean followReferrals) {
    this.followReferrals = followReferrals;
  }

  /**
   * Access the LDAP auto-bind configuration
   *
   * @return if <code>true</code> connection allocation (
   *         {@link LdapConnectionManager#getConnection()}) will include a bind
   *         attempt
   */
  public boolean isAutoBind() {
    return autoBind;
  }

  /**
   * Configure the LDAP auto-bind configuration
   *
   * param autoBind if <code>true</code> connection allocation (
   * {@link LdapConnectionManager#getConnection()}) will include a bind attempt
   */
  public void setAutoBind(boolean autoBind) {
    this.autoBind = autoBind;
  }

  /**
   * Access the LDAP pooling configuration
   *
   * @return if <code>true</code> connections will be maintained in a connection
   *         pool.
   */
  public boolean isPooling() {
    return pooling;
  }

  /**
   * Configure the LDAP connection pooling configuration
   *
   * param pooling if <code>true</code> connections will be maintained in a
   * connection pool. This automatically sets autoBind to true as well
   */
  public void setPooling(boolean pooling) {
    this.pooling = pooling;
  }

  /**
   * @return The maximum number of physical connections in the pool
   */
  public int getPoolMaxConns() {
    return poolMaxConns;
  }

  /**
   * @param maxConns
   *          The maximum number of physical connections in the pool
   */
  public void setPoolMaxConns(int maxConns) {
    this.poolMaxConns = maxConns;
  }
}
