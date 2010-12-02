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
package org.sakaiproject.nakamura.api.presence;

import java.util.List;
import java.util.Map;

/**
 * Manages the presence of a user, and allows users to see who amongst their
 * connections are online.
 */
public interface PresenceService {

  public static final String PRESENCE_RESOURCE_TYPE = "sakai/presence";

  public static final String PRESENCE_SEARCH_PROP = "_presence";

  public static final String PRESENCE_STATUS_PROP = "sakai:status";

  public static final String PRESENCE_LOCATION_PROP = "sakai:location";

  public static final String PRESENCE_CLEAR = "delete";

  /**
   * keep presence for this user alive.
   * 
   * @param uuid
   *          the user id.
   * @param location
   *          the location from which the user is pinging.
   */
  void ping(String uuid, String location);

  /**
   * Update the presence status of the user.
   * 
   * @param presence
   *          the presence status
   */
  /**
   * Update the presence status of the user.
   * 
   * @param uuid
   *          the user id of the user.
   * @param status
   *          the presence status
   */
  void setStatus(String uuid, String status);

  /**
   * This will clear the status and location for the given user
   * 
   * @param uuid
   *          the user id of the user.
   */
  void clear(String uuid);

  /**
   * @param uuid
   *          the user id.
   * @return the status for the user (free text or matches key from {@link PresenceStatus})
   */
  String getStatus(String uuid);

  /**
   * @param uuid
   *          the user id.
   * @return the location of the user (null indicates none)
   */
  String getLocation(String uuid);

  /**
   * @param connections
   *          a list of connections.
   * @return a map of userid to online status.
   */
  Map<String, String> online(List<String> connections);

  /**
   * @param location
   *          the location where the users might be online.
   * @return a map of userid to online status.
   */
  Map<String, String> online(String location);
}
