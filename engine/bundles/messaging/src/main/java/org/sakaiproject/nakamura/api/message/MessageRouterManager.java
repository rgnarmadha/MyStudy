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

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Manages the message routing process. This takes a message represented by a node, and
 * builds a MessageRoutes object for that node. There is normally only one of these
 * services implemented.
 */
public interface MessageRouterManager {

  /**
   * Get the {@link MessageRoutes} for the message represented by the node.
   * 
   * @param n
   *          the node representing the message
   * @return a {@link MessageRoutes} object ( {@link List} ) containing MessageRoute
   *         objects.
   * @throws RepositoryException
   *           if there is a problem accessing the message node.
   */
  MessageRoutes getMessageRouting(Node n) throws RepositoryException;

}
