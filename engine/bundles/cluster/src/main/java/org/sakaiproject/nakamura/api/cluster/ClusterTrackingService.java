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
package org.sakaiproject.nakamura.api.cluster;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public interface ClusterTrackingService {

  /**
   *
   */
  public static final String EVENT_USER = "user";
  /**
   *
   */
  public static final String EVENT_TRACKING_COOKIE = "tracking-cookie";
  /**
   *
   */
  public static final String EVENT_TO_SERVER = "to-server";
  /**
   *
   */
  public static final String EVENT_FROM_SERVER = "from-server";

  public static final String EVENT_PING_CLUSTER_USER = "org/sakaiproject/nakamura/cluster/user/ping";

  /**
   * @param request
   * @param response
   */
  void trackClusterUser(HttpServletRequest request, HttpServletResponse response);

  /**
   * @param trackingCookie
   * @return
   */
  ClusterUser getUser(String trackingCookie);

  /**
   * @return a list of all servers in the cluster.
   */
  List<ClusterServer> getAllServers();


  /**
   * @return get the ID of the current server.
   */
  String getCurrentServerId();


  /**
   * @return generate an ID that will be unique in the cluster.
   */
  String getClusterUniqueId();

  /**
   * @param trackingCookie
   * @return
   */
  ClusterServer getServer(String trackingCookie);

}
