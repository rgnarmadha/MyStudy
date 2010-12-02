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
package org.sakaiproject.nakamura.api.rules;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;

import java.util.Map;

/**
 * Rule Execution Service, provides a mechanism to allow a named rule to be executed.
 */
public interface RuleExecutionService {

  /**
   * Execute a rule pointed to by the path.
   * 
   * @param pathToRuleSet
   *          path to the rule set node.
   * @param request
   *          the current request, needed to resolve the rule set.
   * @param ruleContext
   *          a context object that may be used by any pre processor.
   * @return a map representing the results of the rule execution.
   * @throws RuleExecutionException 
   */
  public Map<String, Object> executeRuleSet(String pathToRuleSet,
      SlingHttpServletRequest request, Resource resource, RuleContext ruleContext, RuleExecutionErrorListener errorListener) throws RuleExecutionException;

}
