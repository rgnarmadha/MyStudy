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

package org.sakaiproject.nakamura.workflow.persistence.managers;

import org.drools.WorkingMemory;
import org.drools.process.command.CommandService;
import org.drools.process.command.SignalEventCommand;
import org.drools.process.instance.event.DefaultSignalManager;
import org.sakaiproject.nakamura.api.workflow.WorkflowConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Session;

public class JcrSignalManager extends DefaultSignalManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(JcrSignalManager.class);
  private CommandService commandService;
  @SuppressWarnings("unused")
  private Session session;

  public JcrSignalManager(WorkingMemory workingMemory) {
    super(workingMemory);
    session = (Session) workingMemory.getEnvironment().get(
        WorkflowConstants.SESSION_IDENTIFIER);

  }

  public void setCommandService(CommandService commandService) {
    this.commandService = commandService;
  }

  public void signalEvent(String type, Object event) {
    for (long id : getProcessInstancesForEvent(type)) {
      getWorkingMemory().getProcessInstance(id);
    }
    super.signalEvent(type, event);
  }

  public void signalEvent(long processInstanceId, String type, Object event) {
    SignalEventCommand command = new SignalEventCommand();
    command.setProcessInstanceId(processInstanceId);
    command.setEventType(type);
    command.setEvent(event);
    commandService.execute(command);
  }

  private List<Long> getProcessInstancesForEvent(String type) {
    // TODO: sort out how this should work, I think it will impact storage.
    LOGGER
        .warn("Events are not propagating, need to implement a query, and sort out ProcessInstancEventInfo ");
    // need to perform a query

    // EntityManager em = (EntityManager) getWorkingMemory().getEnvironment().get(
    // EnvironmentName.ENTITY_MANAGER );

    // Query processInstancesForEvent = em.createNamedQuery(
    // "ProcessInstancesWaitingForEvent" );
    // processInstancesForEvent.setParameter( "type",
    // type );
    // List<Long> list = (List<Long>) processInstancesForEvent.getResultList();

    return new ArrayList<Long>();
  }

}