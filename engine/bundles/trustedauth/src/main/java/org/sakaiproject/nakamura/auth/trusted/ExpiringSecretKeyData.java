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

import java.io.Serializable;

/**
 * A serializable representation of an expiring secret key, using in the cache.
 */
public class ExpiringSecretKeyData implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = -5023151831344430827L;
  protected long expires;
  protected byte[] encoded;
  protected String algorithm;
  private String serverId;

  /**
   * Default constructor, for serialization.
   */
  public ExpiringSecretKeyData() {
  }
  /**
   * @param expires
   * @param encoded
   */
  public ExpiringSecretKeyData(long expires, String algorithm,  byte[] encoded, String serverId) {
    this.expires = expires;
    this.encoded = encoded;
    this.algorithm = algorithm;
    this.serverId = serverId;
  }
  
  /**
   * @return the algorithm.
   */
  public String getAlgorithm() {
    return algorithm;
  }
  /**
   * @return the encoded secret key data.
   */
  public byte[] getEncoded() {
    return encoded;
  }
  /**
   * @return when the key expires.
   */
  public long getExpires() {
    return expires;
  }
  /**
   * @return the server ID the key was generated on.
   */
  public String getServerId() {
    return serverId;
  }
}
