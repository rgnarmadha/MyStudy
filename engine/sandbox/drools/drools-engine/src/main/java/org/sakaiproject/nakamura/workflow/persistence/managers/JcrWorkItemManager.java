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
import org.drools.process.instance.ProcessInstance;
import org.drools.process.instance.WorkItem;
import org.drools.process.instance.WorkItemManager;
import org.drools.process.instance.impl.WorkItemImpl;
import org.drools.runtime.process.WorkItemHandler;
import org.sakaiproject.nakamura.api.workflow.WorkflowConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class JcrWorkItemManager implements WorkItemManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(JcrWorkItemManager.class);
  private WorkingMemory workingMemory;
  private Map<String, WorkItemHandler> workItemHandlers = new HashMap<String, WorkItemHandler>();
  private transient Map<Long, WorkItemInfo> workItems;
  private Session session;

  public JcrWorkItemManager(WorkingMemory workingMemory) {
    this.workingMemory = workingMemory;
    this.session = (Session) workingMemory.getEnvironment().get(
        WorkflowConstants.SESSION_IDENTIFIER);

  }

  public void internalExecuteWorkItem(WorkItem workItem) {
    try {
      WorkItemInfo workItemInfo = new WorkItemInfo(session, workItem);
      workItemInfo.save();
      workItem = workItemInfo.getWorkItem();

      if (this.workItems == null) {
        this.workItems = new HashMap<Long, WorkItemInfo>();
      }
      workItems.put(workItem.getId(), workItemInfo);

      WorkItemHandler handler = (WorkItemHandler) this.workItemHandlers.get(workItem
          .getName());
      if (handler != null) {
        handler.executeWorkItem(workItem, this);
      } else {
        LOGGER.info("Could not find work item handler for " + workItem.getName());
      }
    } catch (RepositoryException e) {
      LOGGER.info("Failed to execute work item " + workItem, e);
    } catch (IOException e) {
      LOGGER.info("Failed to execute work item " + workItem, e);
    }

  }

  public void internalAbortWorkItem(long id) {

    try {
      WorkItemInfo workItemInfo = new WorkItemInfo(session, id);
      // work item may have been aborted
      if (workItemInfo != null) {
        WorkItemImpl workItem = (WorkItemImpl) workItemInfo.getWorkItem();
        WorkItemHandler handler = (WorkItemHandler) this.workItemHandlers.get(workItem
            .getName());
        if (handler != null) {
          handler.abortWorkItem(workItem, this);
        } else {
          LOGGER.info("Could not find work item handler for " + workItem.getName());
        }
        workItems.remove(id);
        workItemInfo.remove();
      }
    } catch (RepositoryException e) {
      LOGGER.info("Failed to remove work item from repository " + id, e);
    } catch (IOException e) {
      LOGGER.info("Failed to remove work item from repository " + id, e);
    }
  }

  public void internalAddWorkItem(WorkItem workItem) {
  }

  public void completeWorkItem(long id, Map<String, Object> results) {
    try {
      WorkItemInfo workItemInfo = null;
      if (this.workItems != null) {
        workItemInfo = this.workItems.get(id);
        if (workItemInfo != null) {
          workItemInfo.save();
        }
      }

      if (workItemInfo == null) {
        workItemInfo = new WorkItemInfo(session, id);
      }

      // work item may have been aborted
      if (workItemInfo != null) {
        WorkItem workItem = (WorkItemImpl) workItemInfo.getWorkItem();
        workItem.setResults(results);
        ProcessInstance processInstance = workingMemory.getProcessInstance(workItem
            .getProcessInstanceId());
        workItem.setState(WorkItem.COMPLETED);
        // process instance may have finished already
        if (processInstance != null) {
          processInstance.signalEvent("workItemCompleted", workItem);
        }
        workItemInfo.remove();
        if (workItems != null) {
          this.workItems.remove(workItem.getId());
        }
        workingMemory.fireAllRules();
      }
    } catch (RepositoryException e) {
      LOGGER.info("Failed to complete work item " + id, e);
    } catch (IOException e) {
      LOGGER.info("Failed to complete work item " + id, e);
    }

  }

  public void abortWorkItem(long id) {
    try {
      WorkItemInfo workItemInfo = null;
      if (this.workItems != null) {
        workItemInfo = this.workItems.get(id);
        workItemInfo.save();
      }

      if (workItemInfo == null) {
        workItemInfo = new WorkItemInfo(session, id);
      }

      // work item may have been aborted
      if (workItemInfo != null) {
        WorkItem workItem = (WorkItemImpl) workItemInfo.getWorkItem();
        ProcessInstance processInstance = workingMemory.getProcessInstance(workItem
            .getProcessInstanceId());
        workItem.setState(WorkItem.ABORTED);
        // process instance may have finished already
        if (processInstance != null) {
          processInstance.signalEvent("workItemAborted", workItem);
        }
        workItemInfo.remove();
        if (workItems != null) {
          workItems.remove(workItem.getId());
        }
        workingMemory.fireAllRules();
      }
    } catch (RepositoryException e) {
      LOGGER.info("Failed to abort work item " + id, e);
    } catch (IOException e) {
      LOGGER.info("Failed to abort work item " + id, e);
    }
  }

  public WorkItem getWorkItem(long id) {
    WorkItemInfo workItemInfo = null;
    if (this.workItems != null) {
      workItemInfo = this.workItems.get(id);
    }

    if (workItemInfo == null) {
      try {
        workItemInfo = new WorkItemInfo(session, id);
      } catch (RepositoryException e) {
        LOGGER.info("Failed to load Work Item " + id, e);
      } catch (IOException e) {
        LOGGER.info("Failed to load Work Item " + id, e);
      }
    }

    if (workItemInfo == null) {
      return null;
    }
    return workItemInfo.getWorkItem();
  }

  public Set<WorkItem> getWorkItems() {
    return new HashSet<WorkItem>();
  }

  public void registerWorkItemHandler(String workItemName, WorkItemHandler handler) {
    this.workItemHandlers.put(workItemName, handler);
  }

  public void clearWorkItems() {
    if (workItems != null) {
      workItems.clear();
    }
  }

}
