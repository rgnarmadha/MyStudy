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

package org.sakaiproject.nakamura.remotefiles;

import java.util.List;
import java.util.Map;


/**
 */
public interface RemoteFilesRepository  {
  /**
   * Create a new directory for the specified user in the specified location
   * @param username
   * @param virtualServerName
   * @param homeDirectory
   * @param directoryName
   */
  void createDirectory (String username, String virtualServerName, 
      String homeDirectory, String directoryName);
  
  Map<String, Object> getDocument(String path, String userId);
  
  byte[] getFileContent(String path, String userId);
  
  List<Map<String, Object>> doSearch(Map<String, Object> searchProperties, String userId);
  
  void updateFile(String path, byte[] fileData, Map<String, Object>properties, String userId);
  
  /**
   * Either adds or removes the specified member from the specified group.
   * If they're in the group, remove them
   * If they're not in the group, add them
   * 
   * @param groupId
   * @param userId
   */
  void toggleMember(String groupId, String userId);
  
  
  void createGroup(String groupId, String userId);
  
  void removeDocument(String path, String userId);
  
  /**
   * Request to share the specified file path, e.g. /zach/party.png, with the specified group, e.g. partytime
   * 
   * @param groupId
   * @param filePath
   * @param userId
   * @return whether or not the share was a success 
   */
  boolean shareFileWithGroup(String groupId, String filePath, String userId);
  

}
