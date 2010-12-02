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

import org.sakaiproject.nakamura.api.cluster.ClusterServer;

import java.io.Serializable;

/**
 * An object, stored in a shared cluster that represents a server.
 */
public class ClusterServerImpl implements ClusterServer, Serializable {

  /**
   * The object must be Serializable to replicate.
   */
  private static final long serialVersionUID = -5670594640931349540L;
  /**
   * The ID of the server.
   */
  private String serverId;
  /**
   * The time the server object was updated.
   */
  private long lastModified;
  private int serverNum;

  /**
   * A URL where servers can make rest request to other servers
   */
  private String secureUrl;

  /**
   * @param serverId
   * @param serverNumber
   */
  public ClusterServerImpl(String serverId, int serverNumber, String secureUrl) {
    this.serverId = serverId;
    this.lastModified = System.currentTimeMillis();
    this.serverNum = serverNumber;
    this.secureUrl = secureUrl;
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

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.cluster.ClusterServer#getServerNumber()
   */
  public int getServerNumber() {
    return serverNum;
  }

  public String getSecureUrl() {
    return secureUrl;
  }

}
