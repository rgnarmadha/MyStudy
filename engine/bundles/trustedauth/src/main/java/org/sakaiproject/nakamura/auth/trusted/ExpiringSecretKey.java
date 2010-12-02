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
package org.sakaiproject.nakamura.auth.trusted;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * A Secret Key that expires.
 */
public class ExpiringSecretKey {

  private long expires;
  private SecretKey secretKey;
  private ExpiringSecretKeyData secretKeyData;

  /**
   * @param b a byte array defining the secret key
   * @param algorithm the algorithm ie "HmacSHA1"
   * @param expires the time of expiry epoch in ms.
   */
  public ExpiringSecretKey(byte[] b, String algorithm, long expires, String serverId) {
    secretKey = new SecretKeySpec(b, algorithm);
    this.expires = expires;
    this.secretKeyData = new ExpiringSecretKeyData(expires, algorithm, b, serverId);
  }
  
  /**
   * @param expiringSecretKeyData construct from a serializable data.
   */
  public ExpiringSecretKey(ExpiringSecretKeyData expiringSecretKeyData) {
    this.secretKeyData = expiringSecretKeyData;
    this.secretKey = new SecretKeySpec(expiringSecretKeyData.getEncoded(), expiringSecretKeyData.getAlgorithm());
    this.expires = expiringSecretKeyData.getExpires();
  }

  /**
   * @return when the key expires
   */
  public long getExpires() {
    return expires;
  }
  
  /**
   * @return the secretKey
   */
  public SecretKey getSecretKey() {
    return secretKey;
  }

  /**
   * @return the a serialzable representation.
   */
  public ExpiringSecretKeyData getSecretKeyData() {
    return secretKeyData;
  }

  public String getServerId() {
    return secretKeyData.getServerId();
  }
}
