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

import org.sakaiproject.nakamura.api.rules.RuleExecutionErrorListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * An internal implementation of the RuleExecutionErrorListner that chains to a user
 * supplied listener if provided.
 */
public class RuleExecutionErrorListenerImpl implements RuleExecutionErrorListener {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(RuleExecutionErrorListenerImpl.class);
  private List<String> errors = new ArrayList<String>();
  private RuleExecutionErrorListener userErrorListener;

  public RuleExecutionErrorListenerImpl(RuleExecutionErrorListener userErrorListener) {
    this.userErrorListener = userErrorListener;
  }

  public void error(String error) {
    errors.add(error);
    if (userErrorListener != null) {
      userErrorListener.error(error);
    }
  }

  public List<String> getErrorMessages() {
    return errors;
  }

  public boolean hasErrorMessages() {
    return errors.size() > 0;
  }

  public void listErrorMessages() {
    for (String err : errors) {
      LOGGER.error(err);
    }

  }

}
