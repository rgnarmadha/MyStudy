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
package org.sakaiproject.nakamura.auth.trusted;

/**
 * This interface allows other systems to provide cookie encoding, in a cluster
 * environment avoiding the need to use sessions, or to have session affinity. There is no
 * implemention of this interface in the default distribution of K2. (lack of time and
 * resource)
 */
public interface ClusterCookieServer {

  /**
   * Encodes a cookie from a user ID
   * 
   * @param userId
   * @return
   */
  String encodeCookie(String userId);

  /**
   * Decodes a userId from a cookie.
   * 
   * @param value
   * @return
   */
  String decodeCookie(String value);

}
