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
package org.apache.jackrabbit.core.security.authorization.acl;


import org.apache.sling.jcr.jackrabbit.server.security.dynamic.RulesBasedAce;

import java.security.Principal;

/**
 *
 */
public class RulesPrincipal implements Principal {

  private String name;

  private String principalName;

  public RulesPrincipal(String name) {
    RulesPrincipal.checkValid(name);
    int i = name.lastIndexOf('.');
    if ( i < RulesBasedAce.SAKAI_RULES.length() ) {
      this.principalName = name.substring(RulesBasedAce.SAKAI_RULES.length());
    } else {
      this.principalName = name.substring(RulesBasedAce.SAKAI_RULES.length(),i);
    }
    this.name = name;
  }

  /**
   * {@inheritDoc}
   * @see java.security.Principal#getName()
   */
  public String getName() {
    return name;
  }

  /**
   * @return the principalName
   */
  public String getPrincipalName() {
    return principalName;
  }

  /**
   * @param string
   */
  public static void checkValid(String name) {
    if ( name == null || !name.startsWith(RulesBasedAce.SAKAI_RULES)) {
      throw new IllegalArgumentException("Not a Rules Principal ");
    }
  }
}
