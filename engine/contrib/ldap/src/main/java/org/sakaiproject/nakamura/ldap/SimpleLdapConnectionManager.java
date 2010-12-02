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
package org.sakaiproject.nakamura.ldap;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPConstraints;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSocketFactory;

import org.sakaiproject.nakamura.api.ldap.LdapConnectionManager;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManagerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.UnsupportedEncodingException;

/**
 * Allocates connected, constrained, and optionally bound and secure
 * <code>LDAPConnections</code>
 *
 * @see LdapConnectionManagerConfig
 * @author Dan McCallum, Unicon Inc
 * @author John Lewis, Unicon Inc
 */
public class SimpleLdapConnectionManager implements LdapConnectionManager {

  /** Class-specific logger */
  private static Logger log = LoggerFactory.getLogger(SimpleLdapConnectionManager.class);

  /** connection allocation configuration */
  protected LdapConnectionManagerConfig config;

  /**
   * {@inheritDoc}
   */
  public void init(LdapConnectionManagerConfig config) {

    log.debug("init()");

    this.config = config;

    verifySetup();
  }

  /**
   * {@inheritDoc}
   */
  public LDAPConnection getConnection() throws LDAPException {
    log.debug("getConnection()");

    verifySetup();

    LDAPConnection conn = newLDAPConnection();
    applyConstraints(conn);
    connect(conn);

    if (config.isAutoBind()) {
      log.debug("getConnection(): auto-binding");
      bind(conn, config.getLdapUser(), config.getLdapPassword());
    }

    return conn;
  }

  public LDAPConnection getBoundConnection(String dn, String pass) throws LDAPException {
    verifySetup();

    log.debug("getBoundConnection(): [dn = {}]", config.getLdapUser());

    LDAPConnection conn = newLDAPConnection();
    applyConstraints(conn);
    connect(conn);
    bind(conn, dn, pass);

    return conn;
  }

  protected LDAPConnection newLDAPConnection() {
    verifySetup();

    LDAPSocketFactory ldapSocketFactory = LdapSecurityUtil.initLDAPSocketFactory(config);
    LDAPConnection conn = new LDAPConnection(ldapSocketFactory);
    return conn;
  }

  private void bind(LDAPConnection conn, String dn, String pw) throws LDAPException {
    log.debug("bind(): binding [dn = {}]", dn);

    try {
      byte[] password = pw.getBytes("UTF8");
      conn.bind(LDAPConnection.LDAP_V3, dn, password);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Failed to encode user password", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void returnConnection(LDAPConnection conn) {
    try {
      if (conn != null) {
        conn.disconnect();
      }
    } catch (LDAPException e) {
      log.error("returnConnection(): failed on disconnect: ", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @param config
   *          a reference to a {@link LdapConnectionManagerConfig}. Should be cacheable
   *          without defensive copying.
   */
  public void setConfig(LdapConnectionManagerConfig config) {
    this.config = config;
  }

  /**
   * {@inheritDoc}
   */
  public LdapConnectionManagerConfig getConfig() {
    return config;
  }

  /**
   * Applies {@link LDAPConstraints} to the specified {@link LDAPConnection}.
   * Implemented to assign <code>timeLimit</code> and
   * <code>referralFollowing</code> constraint values retrieved from the
   * currently assigned {@link LdapConnectionManagerConfig}.
   *
   * @param conn
   */
  protected void applyConstraints(LDAPConnection conn) {
    verifySetup();

    int timeout = config.getOperationTimeout();
    boolean followReferrals = config.isFollowReferrals();
    log.debug("applyConstraints(): values [timeout = {}][follow referrals = {}]", timeout,
        followReferrals);
    LDAPConstraints constraints = new LDAPConstraints();
    constraints.setTimeLimit(timeout);
    constraints.setReferralFollowing(followReferrals);
    conn.setConstraints(constraints);
  }

  /**
   * Connects the specified <code>LDAPConnection</code> to the currently
   * configured host and port.
   *
   * @param conn
   *          an <code>LDAPConnection</code>
   * @throws LDAPConnection
   *           if the connect attempt fails
   */
  protected void connect(LDAPConnection conn) throws LDAPException {
    log.debug("connect()");

    verifySetup();

    conn.connect(config.getLdapHost(), config.getLdapPort());

    try {
      postConnect(conn);
    } catch (LDAPException e) {
      log.error("Failed to completely initialize a connection [host = " + config.getLdapHost()
          + "][port = " + config.getLdapPort() + "]", e);
      try {
        conn.disconnect();
      } catch (LDAPException ee) {
      }

      throw e;
    } catch (Throwable e) {
      log.error("Failed to completely initialize a connection [host = " + config.getLdapHost()
          + "][port = " + config.getLdapPort() + "]", e);
      try {
        conn.disconnect();
      } catch (LDAPException ee) {
      }

      if (e instanceof Error) {
        throw (Error) e;
      }
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }

      throw new RuntimeException("LDAPConnection allocation failure", e);
    }

  }

  protected void postConnect(LDAPConnection conn) throws LDAPException {

    log.debug("postConnect()");

    verifySetup();

    if (config.isSecureConnection() && config.isTLS()) {
      log.debug("postConnect(): starting TLS");
      conn.startTLS();
    }
  }

  private void verifySetup() throws IllegalStateException {
    if (config == null) {
      throw new IllegalStateException("Configuration not available for this connection manager.");
    }

    if (config.getKeystoreLocation() != null && config.getKeystoreLocation().length() > 0
        && !(new File(config.getKeystoreLocation()).exists())) {
      throw new IllegalStateException("Keystore not found at specified location ["
          + config.getKeystoreLocation() + "]");
    }
  }
}
