/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.nakamura.mailman;

import org.sakaiproject.nakamura.api.message.MessageRoute;
import org.sakaiproject.nakamura.mailman.impl.MailmanException;

import java.util.List;

public interface MailmanManager {
  public boolean isServerActive() throws MailmanException;
  public List<String> getLists() throws MailmanException;
  public void createList(String listName, String ownerEmail, String password) throws MailmanException;
  public void deleteList(String listName, String listPassword) throws MailmanException;
  public boolean listHasMember(String listName, String listPassword, String memberEmail) throws MailmanException;
  public boolean addMember(String listName, String listPassword, String userEmail) throws MailmanException;
  public boolean removeMember(String listName, String listPassword, String userEmail) throws MailmanException;
  public MessageRoute generateMessageRouteForGroup(String groupName);
}
