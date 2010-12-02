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

import org.osgi.service.event.Event;

import javax.jcr.Node;

/**
 * MessageTransports transport a message according to the supplied {@link MessageRoutes}.
 * There are normally many {@link MessageTransport} service implementations, each
 * performing a different purpose. The order in which the MessageRoutes service
 * implementations is invoked is not specified and the routes should be considered read
 * only. Services should be implemented as OSGi Managed services.
 */
public interface MessageTransport {

  public static final String INTERNAL_TRANSPORT = "internal";

  /**
   * Potentially send a message in response to the supplied {@link Event}, and
   * {@link MessageRoutes}, The transport should only operate on the {@link MessageRoute}s
   * in the {@link MessageRoutes} object that it understands.
   * 
   * @param routes
   *          the routes for the message.
   * @param event
   *          the event that triggered the message delivery operation.
   * @param n
   *          the node representing the message.
   */
  void send(MessageRoutes routes, Event event, Node n);

}
