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
package org.sakaiproject.nakamura.api.discussion;

import org.sakaiproject.nakamura.api.message.MessagingException;

import javax.jcr.Node;
import javax.jcr.Session;

public interface DiscussionManager {
  /**
   * Goes trough the repository to look for a message with an id and a marker.
   * 
   * @param messageId
   *          The ID of the message to look for.
   * @param marker
   *          The marker it should have.
   * @param session
   * @param path
   *          An optional path where you want to look in.
   * @return
   * @throws MessagingException
   */
  public Node findMessage(String messageId, String marker, Session session, String path)
      throws MessagingException;

  /**
   * Looks in the entire repository to find a sakai/settings file of a certain type and
   * marker.
   * 
   * @param marker
   * @param session
   * @param type
   * @return
   */
  public Node findSettings(String marker, Session session, String type);
}
