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

import javax.jcr.Session;

/**
 * Constants to provide support for the workflow feature.
 */
public interface WorkflowConstants {

  /**
   * The area of the repository use to store workflow sessions.
   */
  public static final String SESSION_STORAGE_PREFIX = "/var/workflow/sessions/";

  /**
   * The area of the repository used to store workflow generated events.
   */
  public static final String PROCESS_INSTANCE_EVENT_STORAGE_PREFIX = "/var/workflow/signal/";

  /**
   * The area of the repository used to store work items.
   */
  public static final String WORK_ITEM_STORAGE_PREFIX = "/var/workflow/item/";

  /**
   * The area of the repository used to store process instances.
   */
  public static final String PROCESS_INSTANCE_STORAGE_PREFIX = "/var/workflow/process/";

  /**
   * The node property that identifies a instance id.
   */
  public static final String PR_PROCESS_INSTANCE_ID = "sakai:workflow-process-instance-id";

  /**
   * The node property that identifies a process id.
   */
  public static final String PR_PROCESS_ID = "sakai:workflow-process-id";

  /**
   * The name of the property that stores the start date of the start of the workflow.
   */
  public static final String PR_STARTDATE = "sakai:workflow-start-date";

  /**
   * The name of the property that stores the date the workflow was last read from the
   * repository.
   */
  public static final String PR_LAST_READ_DATE = "sakai:workflow-lastread-date";

  /**
   * The name of the property that stores date the process instance was last modified.
   */
  public static final String PR_PROCESS_INSTANCE_LAST_MODIFICATION_DATE = "sakai:workflow-lastmodified-date";

  /**
   * The name of the property that stores the state of the process instance (a number).
   */
  public static final String PR_PROCESS_INSTANCE_STATE = "sakai:workflow-process-instance-state";

  /**
   * The name of the property that stores the event type when of an event associated with
   * a process instance.
   */
  public static final String PR_PROCESS_INSTANCE_EVENT_TYPE = "sakai:workflow-event-type";

  /**
   * The name of the property that stores the event types that a process responds to.
   */
  public static final String PR_PROCESS_INSTANCE_EVENT_TYPES = "sakai:workflow-event-types";

  /**
   * The name of the property that stores the name of a workflow item.
   */
  public static final String PR_WORKITEM_NAME = "sakai:workflow-item-name";

  /**
   * The name of the property that stores the date a workflow item was created.
   */
  public static final String PR_WORKITEM_CREATION_DATE = "sakai:workflow-create-date";

  /**
   * The name of the property that stores the state of a workitem.
   */
  public static final String PR_WORKITEM_STATE = "sakai:workflow-item-state";

  /**
   * The name of the JCR session property in the workflow environment.
   */
  public static final String SESSION_IDENTIFIER = Session.class.getName();

}
