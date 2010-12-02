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
import org.drools.common.InternalRuleBase;
import org.drools.common.InternalWorkingMemory;
import org.drools.marshalling.impl.MarshallerReaderContext;
import org.drools.marshalling.impl.MarshallerWriteContext;
import org.drools.marshalling.impl.ProcessInstanceMarshaller;
import org.drools.marshalling.impl.ProcessMarshallerRegistry;
import org.drools.process.instance.impl.ProcessInstanceImpl;
import org.drools.runtime.process.ProcessInstance;
import org.sakaiproject.nakamura.api.workflow.WorkflowConstants;
import org.sakaiproject.nakamura.workflow.persistence.AbstractIdBasedObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class ProcessInstanceInfo extends AbstractIdBasedObject {


  private String processId;
  private Date startDate;
  private Date lastReadDate;
  private Date lastModificationDate;
  private int state;
  // TODO How do I mark a process instance info as dirty when the process
  // instance
  // has changed (so that byte array is regenerated and saved) ?
  private byte[] processInstanceByteArray;
  private Set<String> eventTypes = new HashSet<String>();
  private transient ProcessInstance processInstance;

  public ProcessInstanceInfo(Session session) throws RepositoryException, IOException {
    super(session, -1);
  }

  public ProcessInstanceInfo(Session session, ProcessInstance processInstance)
      throws RepositoryException, IOException {
    super(session, processInstance.getId());
    this.processInstance = processInstance;
    this.processId = processInstance.getProcessId();
  }

  /**
   * @param session
   * @param id
   * @throws IOException 
   * @throws RepositoryException 
   */
  public ProcessInstanceInfo(Session session, long id) throws RepositoryException, IOException {
    super(session, id);
    this.processId = processInstance.getProcessId();
  }

  public String getProcessId() {
    return processId;
  }

  public Date getStartDate() {
    return startDate;
  }

  public Date getLastModificationDate() {
    return lastModificationDate;
  }

  public Date getLastReadDate() {
    return lastReadDate;
  }

  public void updateLastReadDate() {
    lastReadDate = new Date();
  }

  public int getState() {
    return state;
  }

  public ProcessInstance getProcessInstance(WorkingMemory workingMemory) {
    if (processInstance == null) {
      try {
        ByteArrayInputStream bais = new ByteArrayInputStream(processInstanceByteArray);
        MarshallerReaderContext context = new MarshallerReaderContext(bais,
            (InternalRuleBase) workingMemory.getRuleBase(), null, null);
        context.wm = (InternalWorkingMemory) workingMemory;
        ProcessInstanceMarshaller marshaller = getMarshallerFromContext(context);
        processInstance = marshaller.readProcessInstance(context);
        context.close();
      } catch (IOException e) {
        e.printStackTrace();
        throw new IllegalArgumentException("IOException while loading process instance: "
            + e.getMessage());
      }
    }
    return processInstance;
  }

  private ProcessInstanceMarshaller getMarshallerFromContext(
      MarshallerReaderContext context) throws IOException {
    ObjectInputStream stream = context.stream;
    String processInstanceType = stream.readUTF();
    return ProcessMarshallerRegistry.INSTANCE.getMarshaller(processInstanceType);
  }

  private void saveProcessInstanceType(MarshallerWriteContext context,
      ProcessInstance processInstance, String processInstanceType) throws IOException {
    ObjectOutputStream stream = context.stream;
    // saves the processInstance type first
    stream.writeUTF(processInstanceType);
  }

  public void update() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      MarshallerWriteContext context = new MarshallerWriteContext(baos, null, null, null,
          null);
      String processType = ((ProcessInstanceImpl) processInstance).getProcess().getType();
      saveProcessInstanceType(context, processInstance, processType);
      ProcessInstanceMarshaller marshaller = ProcessMarshallerRegistry.INSTANCE
          .getMarshaller(processType);
      marshaller.writeProcessInstance(context, processInstance);
      context.close();
    } catch (IOException e) {
      throw new IllegalArgumentException("IOException while storing process instance "
          + processInstance.getId() + ": " + e.getMessage());
    }
    byte[] newByteArray = baos.toByteArray();
    if (!Arrays.equals(newByteArray, processInstanceByteArray)) {
      this.state = processInstance.getState();
      this.lastModificationDate = new Date();
      this.processInstanceByteArray = newByteArray;
      this.eventTypes.clear();
      for (String type : processInstance.getEventTypes()) {
        eventTypes.add(type);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.workflow.persistence.AbstractIdBasedObject#createContentNode(javax.jcr.Node,
   *      java.lang.String)
   */
  @Override
  protected Node createContentNode(Node parentNode, String nodeName)
      throws RepositoryException {
    Node newObjectNode = parentNode.addNode(nodeName, "nt:file");
    Node contentNode = newObjectNode.addNode("jcr:content", "nt:resource");
    Binary value = contentNode.getSession().getValueFactory().createBinary(
        new ByteArrayInputStream(new byte[0]));
    contentNode.setProperty("jcr:data", value);
    return newObjectNode;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.workflow.persistence.AbstractIdBasedObject#getStoragePrefix()
   */
  @Override
  protected String getStoragePrefix() {
    return WorkflowConstants.PROCESS_INSTANCE_STORAGE_PREFIX;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.workflow.persistence.AbstractIdBasedObject#load()
   */
  @Override
  public void load() throws RepositoryException, IOException {
    processId = getStringValue(WorkflowConstants.PR_PROCESS_ID, "");
    startDate = getDateValue(WorkflowConstants.PR_STARTDATE, new Date());
    lastReadDate = getDateValue(WorkflowConstants.PR_LAST_READ_DATE, new Date());

    lastModificationDate = getDateValue(WorkflowConstants.PR_PROCESS_INSTANCE_LAST_MODIFICATION_DATE,
        new Date());
    state = getIntValue(WorkflowConstants.PR_PROCESS_INSTANCE_STATE, 0);
    processInstanceByteArray = getByteArray(new byte[0]);

    eventTypes = getStringHashSet(WorkflowConstants.PR_PROCESS_INSTANCE_EVENT_TYPES,
        new HashSet<String>());
    processInstance = null;

  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.workflow.persistence.AbstractIdBasedObject#save()
   */
  @Override
  public void save() throws RepositoryException, IOException {
    update();
    setProperty(WorkflowConstants.PR_PROCESS_ID, processId);
    setProperty(WorkflowConstants.PR_STARTDATE, startDate);
    setProperty(WorkflowConstants.PR_LAST_READ_DATE, lastReadDate);
    setProperty(WorkflowConstants.PR_PROCESS_INSTANCE_LAST_MODIFICATION_DATE, lastModificationDate);
    setProperty(WorkflowConstants.PR_PROCESS_INSTANCE_STATE, state);
    setProperty(processInstanceByteArray);

    setProperty(WorkflowConstants.PR_PROCESS_INSTANCE_EVENT_TYPES, eventTypes
        .toArray(new String[eventTypes.size()]));
    
    if (session.hasPendingChanges()) {
      session.save();
    }

  }


}
