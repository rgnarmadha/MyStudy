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
package org.sakaiproject.nakamura.rules;

/**
 * Identifies Rules objects specifying the input and output parameters.
 */
public class RulesObjectIdentifier {
  private String inIdentifier;
  private String outIdentifier;

  /**
   * Create a rule Object Identifier with in and out parameters.
   * 
   * @param inIdentifier
   *          the input parameter name.
   * @param outIdentifier
   *          the output parameter name, may be null if not bound to an output parameter.
   */
  public RulesObjectIdentifier(String inIdentifier, String outIdentifier) {
    this.inIdentifier = inIdentifier;
    this.outIdentifier = outIdentifier;
  }

  /**
   * @return the input identifier used to bind to the object.
   */
  public String getInIdentifier() {
    return inIdentifier;
  }

  /**
   * @return the output identifier used to bind to the object, if null the object is not
   *         bound to an output parameter in the rules execution model.
   */
  public String getOutIdentifier() {
    return outIdentifier;
  }
}
