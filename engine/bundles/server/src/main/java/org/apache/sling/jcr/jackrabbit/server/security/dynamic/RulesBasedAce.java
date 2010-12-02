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

  import org.apache.jackrabbit.core.security.authorization.acl.RulesPrincipal;

  import java.security.Principal;

  /**
   *
   */
  public class RulesBasedAce {

    /**
     * Prefix for rules principals
     */
    public static final String SAKAI_RULES = "sakai-rules:";
    /**
     * a multivalue property of IOS8601 formatted date ranges in the form 19991225T230000Z,
     * if the time now is inside the range, the ACE is active, if the property is not
     * present the ace is active.
     */
    public static final String P_ACTIVE_RANGE = "sakai:period-active";
    /**
     * a multivalue property of IOS8601 formatted date ranges in the form 19991225T230000Z,
     * if the time now is inside the range, the ACE is inactive.
     */
    public static final String P_INACTIVE_RANGE = "sakai:period-inactive";
    /**
     * The name of a rule processor to consult, rule also has to be active.
     */
    public static final String P_RULEPROCESSOR = "sakai:rule-processor";

    /**
     * Generate a principal for the principal name
     * @param name the name of the principal to targe.
     * @return
     */
    public static Principal createPrincipal(String name) {
      return new RulesPrincipal(SAKAI_RULES + name + "." + System.currentTimeMillis());
    }

    /**
     * @param principal
     * @return
     */
    public static boolean isRulesBasedPrincipal(Principal principal) {
      return (principal instanceof RulesPrincipal);
    }

  }
