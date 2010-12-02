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
package org.apache.sling.jcr.jackrabbit.server.security.dynamic;

import javax.jcr.Node;

/**
 * Apply rules to the selection process for ACE within the system.
 */
public interface RuleProcessor {

  public static final String SERVICE_NAME = "ruleprocessor.name";

  /**
   * Check if the Ace is active by applying rules in the implementation.
   * @param aceNode the node defining the ACE
   * @param contextNode the node which the ACE is being applied to.
   * @param userId the current user
   * @return true if the ACE should be compiled for this user, false if not.
   */
  boolean isAceActive(Node aceNode, Node contextNode, String userId);

}
