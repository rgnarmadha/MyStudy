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
package org.sakaiproject.nakamura.cluster;

import org.sakaiproject.nakamura.api.cluster.ClusterUser;

/**
 * An object that represents a user somewhere in the cluster.
 */
public class ClusterUserImpl implements ClusterUser {

  /**
   * The TTL in the cache
   */
  private static final long TTL = 300000L;
  /**
   * The time the object was created.
   */
  private long lastModified;
  /**
   * The server ID where the user was last registered.
   */
  private String serverId;
  /**
   * The userID
   */
  private String remoteUser;

  /**
   * @param remoteUser
   */
  public ClusterUserImpl(String remoteUser, String serverId) {
    this.remoteUser = remoteUser;
    this.serverId = serverId;
    this.lastModified = System.currentTimeMillis();
  }

  /**
   * @param remoteUser
   * @return
   */
  public boolean expired(String remoteUser) {
    return (lastModified + TTL < System.currentTimeMillis())
        || !this.remoteUser.equals(remoteUser);
  }

  /**
   * @return
   */
  public boolean expired() {
    return (lastModified + TTL < System.currentTimeMillis());
  }

  /**
   * @return
   */
  public String getUser() {
    return remoteUser;
  }

  /**
   * @return the serverId
   */
  public String getServerId() {
    return serverId;
  }

  /**
   * @return the lastModified
   */
  public long getLastModified() {
    return lastModified;
  }

}
