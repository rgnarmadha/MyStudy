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

/**
 * Represents the routing information for a message. Implementations of this interface are
 * added to the the {@link MessageRoutes} for by the {@link MessageRouterManager} or
 * {@link MessageRoute} implementations and inspected by {@link MessageTransport}
 * implementations to perform routing. The content and format of {@ling
 * MessageRoute#getRcpt()} is dependant on the transport of the {@link MessageRoute}
 */
public interface MessageRoute {

  /**
   * @return The transport that this route binds to.
   */
  String getTransport();

  /**
   * @return The target recipient, understood by the transport.
   */
  String getRcpt();

}
