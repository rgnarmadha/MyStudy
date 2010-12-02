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
package org.sakaiproject.nakamura.api.profile;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.resource.ValueMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 *
 */
public interface ProfileService {

  /**
   * @param authorizable
   *          The authorizable for which the home folder should be looked up.
   * @return The JCR path that represents the home folder of an authorizable.
   */
  String getHomePath(Authorizable authorizable);

  /**
   * @param authorizable
   *          The authorizable for which the public folder should be looked up.
   * @return The JCR path that represents the public folder of an authorizable.
   */
  String getPublicPath(Authorizable authorizable);

  /**
   * @param authorizable
   *          The authorizable for which the private folder should be looked up.
   * @return The JCR path that represents the private folder of an authorizable.
   */
  String getPrivatePath(Authorizable authorizable);

  /**
   * @param authorizable
   *          The authorizable for which the private folder should be looked up.
   * @return The JCR path that represents the profile of an authorizable.
   */

  String getProfilePath(Authorizable authorizable);

  /**
   * Gets profile information from JCR and expands external resources efficiently.
   *
   * @param authorizable
   *          The profile of this authorizable will be written out.
   * @param session
   *          A JCR Session that can be used to access the necessary nodes.
   *
   * @return A Map that represents the profile, or null if no profile was found.
   */
  ValueMap getProfileMap(Authorizable authorizable, Session session)
      throws RepositoryException;

  /**
   * Gets profile information from JCR and expands external resources efficiently.
   *
   * @param profileNode
   *          The node that represents the top level profile node.
   *
   * @return A Map that represents the profile.
   */
  ValueMap getProfileMap(Node profileNode) throws RepositoryException;

  /**
   * Gets the compact profile information from JCR and expands external resources
   * efficiently.
   *
   * @param authorizable
   *          The profile of this authorizable will be written out.
   * @param session
   *          A JCR Session that can be used to access the necessary nodes.
   *
   * @return A Map that represents the profile, or null if no profile was found.
   */
  ValueMap getCompactProfileMap(Authorizable authorizable, Session session)
      throws RepositoryException;

  /**
   * Gets the compact profile information from JCR and expands external resources
   * efficiently.
   *
   * @param profileNode
   *          The node that represents the top level profile node.
   *
   * @return A Map that represents the profile.
   */
  ValueMap getCompactProfileMap(Node profileNode) throws RepositoryException;
}
