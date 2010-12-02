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
package org.sakaiproject.nakamura.workflow.persistence;

import org.drools.KnowledgeBase;
import org.drools.SessionConfiguration;
import org.drools.process.command.CommandService;
import org.drools.process.command.impl.CommandBasedStatefulKnowledgeSession;
import org.drools.runtime.Environment;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;
import org.sakaiproject.nakamura.api.workflow.JcrKnowledgeServiceProvider;
import org.sakaiproject.nakamura.api.workflow.WorkflowConstants;
import org.sakaiproject.nakamura.workflow.persistence.managers.JcrProcessInstanceManagerFactory;
import org.sakaiproject.nakamura.workflow.persistence.managers.JcrSignalManagerFactory;
import org.sakaiproject.nakamura.workflow.persistence.managers.JcrWorkItemManagerFactory;

import java.util.Properties;

import javax.jcr.Session;

/**
 *
 */
public class JcrKnowledgeServiceProviderImpl implements JcrKnowledgeServiceProvider {

  private static final String COMMAND_SERVICE = JcrSingleSessionCommandService.class.getName();
  private static final String PROCESS_INSTANCE_MANAGER_FACTORY = JcrProcessInstanceManagerFactory.class.getName();
  private static final String WORK_ITEM_MANAGER_FACTORY = JcrWorkItemManagerFactory.class.getName();
  private static final String SIGNAL_MANAGER_FACTORY = JcrSignalManagerFactory.class.getName();

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.workflow.JcrKnowledgeServiceProvider#loadStatefulKnowledgeSession(int,
   *      org.drools.KnowledgeBase, org.drools.runtime.KnowledgeSessionConfiguration,
   *      org.drools.runtime.Environment)
   */
  public StatefulKnowledgeSession loadStatefulKnowledgeSession(int id,
      KnowledgeBase kbase, KnowledgeSessionConfiguration configuration,
      Environment environment, Session session) {
    
    
    if (configuration == null) {
      configuration = new SessionConfiguration();
    }

    if (environment == null) {
      throw new IllegalArgumentException("Environment cannot be null");
    }

    Properties props = new Properties();

    props.setProperty("drools.commandService", COMMAND_SERVICE );
    props.setProperty("drools.processInstanceManagerFactory", PROCESS_INSTANCE_MANAGER_FACTORY);
    props.setProperty("drools.workItemManagerFactory", WORK_ITEM_MANAGER_FACTORY);
    props.setProperty("drools.processSignalManagerFactory", SIGNAL_MANAGER_FACTORY);

    
    ((SessionConfiguration) configuration).addProperties(props);
    
    environment.set(WorkflowConstants.SESSION_IDENTIFIER, session);

    CommandService commandService = new JcrSingleSessionCommandService(id, kbase,
        configuration, environment, session);
    return new CommandBasedStatefulKnowledgeSession(commandService);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.workflow.JcrKnowledgeServiceProvider#newStatefulKnowledgeSession(org.drools.KnowledgeBase,
   *      org.drools.runtime.KnowledgeSessionConfiguration,
   *      org.drools.runtime.Environment)
   */
  public StatefulKnowledgeSession newStatefulKnowledgeSession(KnowledgeBase kbase,
      KnowledgeSessionConfiguration configuration, Environment environment, Session session) {
    
    
    if (configuration == null) {
      configuration = new SessionConfiguration();
    }

    if (environment == null) {
      throw new IllegalArgumentException("Environment cannot be null");
    }

    Properties props = new Properties();

    props.setProperty("drools.commandService", COMMAND_SERVICE );
    props.setProperty("drools.processInstanceManagerFactory", PROCESS_INSTANCE_MANAGER_FACTORY);
    props.setProperty("drools.workItemManagerFactory", WORK_ITEM_MANAGER_FACTORY);
    props.setProperty("drools.processSignalManagerFactory", SIGNAL_MANAGER_FACTORY);

    ((SessionConfiguration) configuration).addProperties(props);

    environment.set(WorkflowConstants.SESSION_IDENTIFIER, session);
    CommandService commandService = new JcrSingleSessionCommandService(kbase, configuration,
        environment, session);
    return new CommandBasedStatefulKnowledgeSession(commandService);

  }

  public int getStatefulKnowledgeSessionId(StatefulKnowledgeSession ksession) {
    if (ksession instanceof CommandBasedStatefulKnowledgeSession) {
      JcrSingleSessionCommandService commandService = (JcrSingleSessionCommandService) ((CommandBasedStatefulKnowledgeSession) ksession)
          .getCommandService();
      return commandService.getSessionId();
    } else {
      throw new IllegalArgumentException(
          "StatefulKnowledgeSession must be an a CommandBasedStatefulKnowledgeSession");
    }
  }

}
