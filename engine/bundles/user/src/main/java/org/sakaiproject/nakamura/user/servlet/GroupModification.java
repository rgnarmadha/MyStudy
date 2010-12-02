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
package org.sakaiproject.nakamura.user.servlet;

import org.apache.jackrabbit.api.security.user.Group;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;

public class GroupModification extends Modification {

  private Group mainGroup;
  private Group group;
  private boolean join;
  
  
  public GroupModification(ModificationType type, String source, String destination) {
    super(type, source, destination);
  }
  
  public static Modification onGroupJoin(String path, Group mainGroup, Group group) {
    GroupModification result = new GroupModification(ModificationType.MODIFY, path, null);
    result.setMainGroup(mainGroup);
    result.setGroup(group);
    result.setJoin(true);
    return result;
  }

  public static Modification onGroupLeave(String path, Group mainGroup, Group group)  {
    GroupModification result = new GroupModification(ModificationType.MODIFY, path, null);
    result.setMainGroup(mainGroup);
    result.setGroup(group);
    result.setJoin(false);
    return result;
  }

  public void setGroup(Group group) {
    this.group = group;
  }

  public Group getGroup() {
    return group;
  }

  public void setMainGroup(Group mainGroup) {
    this.mainGroup = mainGroup;
  }

  public Group getMainGroup() {
    return mainGroup;
  }

  public void setJoin(boolean join) {
    this.join = join;
  }

  public boolean isJoin() {
    return join;
  }
  
  

}
