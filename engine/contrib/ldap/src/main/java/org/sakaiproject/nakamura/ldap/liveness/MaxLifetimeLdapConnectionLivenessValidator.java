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

import com.novell.ldap.LDAPConnection;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionLivenessValidator;
import org.sakaiproject.nakamura.ldap.PooledLDAPConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dmccallum
 * @author "Carl Hall" <carl.hall@gatech.edu>
 */
@Component(enabled = false)
@Service
public class MaxLifetimeLdapConnectionLivenessValidator implements LdapConnectionLivenessValidator {

  /** Class-specific logger */
  private static Logger log = LoggerFactory
      .getLogger(MaxLifetimeLdapConnectionLivenessValidator.class);

  public static final long DEFAULT_MAX_TTL = 14400000L; // 4 hours

  /**
   * Max connection life in millis
   */
  private long maxTtl;

  /**
   * Tests if the allowable lifetime of the given connection has already
   * elapsed. Edge cases:
   *
   * <ol>
   * <li>Non-{@link PooledLDAPConnection} - returns <code>true</code></li>
   * <li><code>maxTtl</code> &lt;= 0 - returns <code>true</code></li>
   * <li>Connection birthdate in the future - returns <code>true</code></li>
   * </ol>
   *
   *
   */
  public boolean isConnectionAlive(LDAPConnection connectionToTest) {
    if (!(connectionToTest instanceof PooledLDAPConnection)) {
      if (log.isDebugEnabled()) {
        log.debug("isConnectionAlive(): connection not of expected type ["
            + (connectionToTest == null ? "null" : connectionToTest.getClass().getName())
            + "], returning true");
      }
      return true;
    }
    if (maxTtl <= 0) {
      if (log.isDebugEnabled()) {
        log.debug("isConnectionAlive(): maxTtl set to infinite [" + maxTtl + "], returning true");
      }
      return true;
    }
    long now = System.currentTimeMillis();
    long then = ((PooledLDAPConnection) connectionToTest).getBirthdate();
    long elapsed = now - then;
    boolean isAlive = elapsed <= maxTtl;
    if (log.isDebugEnabled()) {
      log.debug("isConnectionAlive(): [now = " + now + "][then = " + then + "][elapsed = "
          + elapsed + "][max TTL = " + maxTtl + "][isAlive = " + isAlive + "]");
    }
    return isAlive;
  }

  /**
   * Get the max connection lifetime, in millis. Values less than or equals to
   * zero are considered infinite, i.e. no TTL.
   *
   * @return
   */
  public long getMaxTtl() {
    return maxTtl;
  }

  /**
   * Assign the max connection lifetime, in millis. Values less than or equal to
   * zero are considered infinite, i.e. no TTL.
   *
   * @param maxTtl
   */
  public void setMaxTtl(long maxTtl) {
    this.maxTtl = maxTtl;
  }
}
