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
package org.apache.sling.jcr.jackrabbit.server.security.dynamic;

import java.util.List;

import javax.jcr.Node;

/**
 * Provides dynamic resolution of principals based on the current context of the request.
 * This should be implemented by declarative services to enable installations to integrate
 * with dynamic sources of principals.
 */
public interface DynamicPrincipalManager {
  /**
   * Returns true if the current session has the principal in the current context.
   * 
   * @param principalName
   *          the name of the principal
   * @param aclNode
   *          the acl node which has triggered the request.
   * @param contextNode
   *          the node which the acl is being applied to, normally a decendent of the parent node of the acl node.
   * @param userId
   *          the user id making the request (note this is *not* the same as the user id
   *          bound to the session), may be null
   * @return true if the user has the principal.
   */
  boolean hasPrincipalInContext(String principalName, Node aclNode, Node contextNode, String userId);

  /**
   * Get the members of the supplied principal, if that is a user, it may have no members,
   * if it is a group it will may have dynamic members. If the principal is not managed by
   * the DynamicPrincipalManager implementation they should return null.
   * 
   * @param principalName
   *          the principal name identifying the Authorizable for which the caller wants a
   *          list of Members.
   * @return A list of principalNames.
   */
  List<String> getMembersOf(String principalName);

  /**
   * Get a list of principal names that this supplied principalName has membership of. (ie
   * if the principalName is "ieb", then this will return the membership of ieb)
   * 
   * @param principalName
   *          the principalName for which membership is required.
   * @return a list of groups the user is a member of, null if the question is not
   *         relevant to the implementation.
   */
  List<String> getMembershipFor(String principalName);

}
