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

package org.sakaiproject.nakamura.connections;

import static org.sakaiproject.nakamura.api.connections.ConnectionConstants.CONTACT_STORE_NAME;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.util.PathUtils;

/**
 * Simple utils which help us when working with connections
 * 
 */
public class ConnectionUtils {
  /**
   * The root of the personal connections path. Needed because several ConnectionManager
   * clients supply only a user ID from which to retrieve information.
   */
  public static final String CONNECTION_PATH_ROOT = "/_user/contacts";



  /**
   * Builds the path to the connection node.
   * 
   * @param user
   *          the user who owns the connection
   * @param targetUser
   *          the target user of the connection
   * @param remainderPath
   *          any path after the name of the node, including selectors eg .accept.html
   *          would results in /_users/connect/xx/yy/zz/ieb/xz/xy/zy/nico.accept.html,
   *          this is not an absolute path fragment and may start half way through an
   *          element. / is not used to separate.
   * @return the path to the connection node or subtree node.
   */
  public static String getConnectionPath(Authorizable user, Authorizable targetUser,
      String remainderPath) {
    if (remainderPath == null) {
      remainderPath = "";
    }

    StringBuilder sb = new StringBuilder();
    sb.append(getConnectionPathBase(user));
    sb.append(PathUtils.getSubPath(targetUser)).append(remainderPath);
    return PathUtils.normalizePath(sb.toString());
  }

  /**
   * Builds the path to the connection node.
   * 
   * @param user
   *          the user who owns the connection
   * @param targetUser
   *          the target user of the connection
   * @return the path to the connection node or subtree node.
   */
  public static String getConnectionPath(Authorizable user, Authorizable targetUser) {
    return getConnectionPath(user, targetUser, null);
  }

  /**
   * @param au
   *          The <code>authorizable</code> to get the connection folder for.
   * @return The absolute path to the connection folder in a user his home folder. ex:
   *         /_user/j/jo/joh/john/johndoe/contacts
   */
  public static String getConnectionPathBase(Authorizable au) {
    return PersonalUtils.getHomeFolder(au) + "/" + CONTACT_STORE_NAME;
  }

}
