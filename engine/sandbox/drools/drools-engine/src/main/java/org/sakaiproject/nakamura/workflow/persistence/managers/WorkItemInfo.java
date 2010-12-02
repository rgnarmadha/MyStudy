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

import org.drools.marshalling.impl.InputMarshaller;
import org.drools.marshalling.impl.MarshallerReaderContext;
import org.drools.marshalling.impl.MarshallerWriteContext;
import org.drools.marshalling.impl.OutputMarshaller;
import org.drools.process.instance.WorkItem;
import org.sakaiproject.nakamura.api.workflow.WorkflowConstants;
import org.sakaiproject.nakamura.workflow.persistence.AbstractIdBasedObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class WorkItemInfo extends AbstractIdBasedObject {

    private static final String WORKITEM_INSTANCE_STORAGE_PREFIX = "/var/drools/workinstance/";
    private String name;
    private Date   creationDate;
    private long   processInstanceId;
    private long   state;
    private 
    byte[]         workItemByteArray;
    private transient
    WorkItem       workItem;

    public WorkItemInfo(Session session) throws RepositoryException, IOException {
      super(session, -1);
    }

    public WorkItemInfo(Session session, WorkItem workItem) throws RepositoryException, IOException {
        super(session, workItem.getId());
        this.workItem = workItem;
        this.name = workItem.getName();
        this.creationDate = new Date();
        this.processInstanceId = workItem.getProcessInstanceId();
    }
    
    
    public WorkItemInfo(Session session, long id) throws RepositoryException, IOException {
      super(session, id);
      getWorkItem();
      this.name = workItem.getName();
      this.creationDate = new Date();
      this.processInstanceId = workItem.getProcessInstanceId();
    }

    
    public String getName() {
        return name;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public long getProcessInstanceId() {
        return processInstanceId;
    }

    public long getState() {
        return state;
    }

    public WorkItem getWorkItem() {
        if ( workItem == null ) {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream( workItemByteArray );
                MarshallerReaderContext context = new MarshallerReaderContext( bais,
                                                                               null,
                                                                               null,
                                                                               null );
                workItem = InputMarshaller.readWorkItem( context );
                context.close();
            } catch ( IOException e ) {
                e.printStackTrace();
                throw new IllegalArgumentException( "IOException while loading process instance: " + e.getMessage() );
            }
        }
        return workItem;
    }

    public void update() {
        this.state = workItem.getState();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            MarshallerWriteContext context = new MarshallerWriteContext( baos,
                                                                         null,
                                                                         null,
                                                                         null,
                                                                         null );
            OutputMarshaller.writeWorkItem( context,
                                            workItem );
            context.close();
            this.workItemByteArray = baos.toByteArray();
        } catch ( IOException e ) {
            throw new IllegalArgumentException( "IOException while storing workItem " + workItem.getId() + ": " + e.getMessage() );
        }
    }

    /**
     * {@inheritDoc}
     * @see org.sakaiproject.nakamura.workflow.persistence.AbstractIdBasedObject#createContentNode(javax.jcr.Node, java.lang.String)
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
     * @see org.sakaiproject.nakamura.workflow.persistence.AbstractIdBasedObject#getStoragePrefix()
     */
    @Override
    protected String getStoragePrefix() {
      return WORKITEM_INSTANCE_STORAGE_PREFIX;
    }

    /**
     * {@inheritDoc}
     * @see org.sakaiproject.nakamura.workflow.persistence.AbstractIdBasedObject#load()
     */
    @Override
    public void load() throws RepositoryException, IOException {
      name = getStringValue(WorkflowConstants.PR_WORKITEM_NAME, "WorkItem:"+id);
      creationDate = getDateValue(WorkflowConstants.PR_WORKITEM_CREATION_DATE, new Date());
      processInstanceId = getLongValue(WorkflowConstants.PR_PROCESS_INSTANCE_ID, 0);
      state = getLongValue(WorkflowConstants.PR_WORKITEM_STATE, 0);
      workItemByteArray = getByteArray(new byte[0]);
      workItem = null;
    }

    /**
     * {@inheritDoc}
     * @see org.sakaiproject.nakamura.workflow.persistence.AbstractIdBasedObject#save()
     */
    @Override
    public void save() throws RepositoryException, IOException {
      update();
      setProperty(WorkflowConstants.PR_WORKITEM_NAME, name);
      setProperty(WorkflowConstants.PR_WORKITEM_CREATION_DATE, creationDate);
      setProperty(WorkflowConstants.PR_PROCESS_INSTANCE_ID, processInstanceId);
      setProperty(WorkflowConstants.PR_WORKITEM_STATE,state);
      setProperty(workItemByteArray);
     }


}
