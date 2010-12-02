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
package org.sakaiproject.nakamura.api.connections;


/**
 * These are the constants related to contacts / connections for users
 */
public interface ConnectionConstants {

  /**
   * This marks the base contacts node,
   */
  public static final String SAKAI_CONTACTSTORE_RT = "sakai/contactstore";

  /**
   * This marks the contact user store nodes (1 per user)
   */
  public static final String SAKAI_CONTACT_RT = "sakai/contact";

  /**
   * This marks the nodename for the contact store's folder.
   */
  public static final String CONTACT_STORE_NAME = "contacts";
  
  public static final String SAKAI_CONNECTION_STATE = "sakai:state";

  public static final String SEARCH_PROP_CONNECTIONSTORE = "_connectionstore";

  /**
   * The multi-valued property used to tell the user what sorts of personal connection this is.
   */
  public static final String SAKAI_CONNECTION_TYPES = "sakai:types";

  /**
   * The parameter used to specify the relationship from the requester's point of view.
   */
  public static final String PARAM_FROM_RELATIONSHIPS = "fromRelationships";

  /**
   * The parameter used to specify the relationship from the target's point of view.
   */
  public static final String PARAM_TO_RELATIONSHIPS = "toRelationships";

  /**
   * The base of the connections event topic. It should be appended by one of the
   * {@link ConnectionOperation}.
   */
  public static final String EVENT_TOPIC_BASE = "org/sakaiproject/nakamura/connections/";

}
