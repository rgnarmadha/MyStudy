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
package org.sakaiproject.nakamura.api.message;

import javax.jcr.Node;

/**
 * A MessageRouter edits the {@link MessageRoutes} for the message converting the
 * {@link MessageRoute}s that it contains as appropriate. This interface represents a
 * provider service (ie an OSGi Managed service registering this interface) implemented by
 * bundles that want to influence the routing of messages. There are no default
 * implementations since Message routing is direct in the default implementation,
 * recipients binding directly to the transport.
 */
public interface MessageRouter {

  /**
   * Modify the routing list represented as a {@link MessageRoutes} object for the message
   * represented by the {@link Node}
   * 
   * @param n
   *          a node representing the message being routed.
   * @param routing
   *          the routing information, which may be modified as a result of the call.
   */
  public void route(Node n, MessageRoutes routing);

  /**
   * @return The priority of this router. MessageRouters with a higher priority are
   *         invoked first, lower priority routers are invoked last.
   */
  public int getPriority();

}
