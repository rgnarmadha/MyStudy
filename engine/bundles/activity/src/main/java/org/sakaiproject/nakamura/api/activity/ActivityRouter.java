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
package org.sakaiproject.nakamura.api.activity;

import java.util.List;

import javax.jcr.Node;

/**
 * A router can add/delete/modify an {@link ActivityRoute} in the list. * All the
 */
public interface ActivityRouter {

  /**
   * Route an activity.
   * 
   * @param activity
   *          The activity node that should be routed. This node will be retrieved trough
   *          the admin session.
   * @param routes
   *          The list of {@link ActivityRoute} that have already been routed.
   */
  public void route(Node activity, List<ActivityRoute> routes);

  /**
   * @return The priority of this router. The higher priority routers will be executed
   *         first.
   */
  public int getPriority();

}
