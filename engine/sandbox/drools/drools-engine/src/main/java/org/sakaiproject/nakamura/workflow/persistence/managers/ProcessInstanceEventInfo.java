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

import org.sakaiproject.nakamura.api.workflow.WorkflowConstants;
import org.sakaiproject.nakamura.workflow.persistence.AbstractIdBasedObject;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class ProcessInstanceEventInfo extends AbstractIdBasedObject {


  private String eventType;

  ProcessInstanceEventInfo(Session session) throws RepositoryException, IOException {
    super(session, -1);
  }

  public ProcessInstanceEventInfo(Session session, long processInstanceId,
      String eventType) throws RepositoryException, IOException {
    super(session, processInstanceId);
    this.eventType = eventType;
  }

  public long getProcessInstanceId() {
    return getId();
  }

  public String getEventType() {
    return eventType;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.workflow.persistence.AbstractIdBasedObject#createContentNode(javax.jcr.Node, java.lang.String)
   */
  @Override
  protected Node createContentNode(Node parentNode, String nodeName)
      throws RepositoryException {
    Node n = parentNode.addNode(nodeName);
    n.addMixin("mix:created");
    return n;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.workflow.persistence.AbstractIdBasedObject#getStoragePrefix()
   */
  @Override
  protected String getStoragePrefix() {
    return WorkflowConstants.PROCESS_INSTANCE_EVENT_STORAGE_PREFIX;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.workflow.persistence.AbstractIdBasedObject#load()
   */
  @Override
  public void load() throws RepositoryException, IOException {
    this.eventType = getStringValue(WorkflowConstants.PR_PROCESS_INSTANCE_EVENT_TYPE, "");
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.workflow.persistence.AbstractIdBasedObject#save()
   */
  @Override
  public void save() throws RepositoryException, IOException {
    setProperty(WorkflowConstants.PR_PROCESS_INSTANCE_EVENT_TYPE, eventType);
    if (session.hasPendingChanges()) {
      session.save();
    }
  }

}
