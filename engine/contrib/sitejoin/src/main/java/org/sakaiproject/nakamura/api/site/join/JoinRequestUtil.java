/*
 * Licensed to the Sakai Foundation (SF) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The SF licenses this file to you under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.api.site.join;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.util.PathUtils;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class JoinRequestUtil {
  public static final String JOIN_REQUESTS = "/joinrequests";

  public static String getPath(String sitePath, String userId, Session session)
      throws RepositoryException {
    sitePath = PathUtils.normalizePath(sitePath);
    UserManager userManager = AccessControlUtil.getUserManager(session);
    Authorizable authorizable = userManager.getAuthorizable(userId);
    String userPath = PathUtils.normalizePath(PathUtils.getSubPath(authorizable));
    String requestPath = sitePath + JOIN_REQUESTS + userPath;
    return requestPath;
  }

  public static Node getRequest(String sitePath, String userId, Session session)
      throws PathNotFoundException, RepositoryException {
    String requestPath = JoinRequestUtil.getPath(sitePath, userId, session);
    Node request = session.getNode(requestPath);
    return request;
  }
}
