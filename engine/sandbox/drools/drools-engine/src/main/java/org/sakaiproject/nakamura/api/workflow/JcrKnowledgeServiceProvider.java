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
package org.sakaiproject.nakamura.api.workflow;

import org.drools.KnowledgeBase;
import org.drools.runtime.Environment;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;

import javax.jcr.Session;

/**
 * A JcrKnowledgeService provider ,mechanisms for creating new sessions and loading existing sessions by ID.
 */
public interface JcrKnowledgeServiceProvider {

  /**
   * Create a new Stateful Knowledge Session backed up by JCR. 
   * @param kbase the knowledge base.
   * @param configuration the Session configuration.
   * @param environment The environment.
   * @param session a JCR session to use
   * @return
   */
  public StatefulKnowledgeSession newStatefulKnowledgeSession(KnowledgeBase kbase,
      KnowledgeSessionConfiguration configuration,
      Environment environment, Session session);

  /**
   * Load an existing Statefull Knowledge session from JCR.
   * @param id the id of the session.
   * @param kbase
   * @param configuration
   * @param environment
   * @param session
   * @return
   */
  public StatefulKnowledgeSession loadStatefulKnowledgeSession(int id,
       KnowledgeBase kbase,
       KnowledgeSessionConfiguration configuration,
       Environment environment, Session session );
}
