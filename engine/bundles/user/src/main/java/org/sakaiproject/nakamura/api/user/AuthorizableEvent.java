/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.nakamura.api.user;



/**
 * A notification style event fired asynchronously with serializable properties detached
 * form the session and request.
 */
public interface AuthorizableEvent {
  /**
   * The topic that the event is sent as.
   */
  public static final String TOPIC = "org/apache/sling/jackrabbit/usermanager/event/";
  
  /**
   * Key for the operation.
   */
  public static final String OPERATION = "operation";
  /**
   * Principal name of the authorizable.
   */
  public static final String PRINCIPAL_NAME = "principal_name";
  

  public static final String MODIFICATION = "change";

  public static final String USER = "user";
  
  public static final String GROUP = "group";

  public static final String MODIFYING_USER = "modifyingUser";

  /**
   * Operations
   */
  public static enum Operation {
    delete(), update(), create(), join(), part(), unknown();
    
    public String getTopic() {
      return TOPIC+toString();
    }
  }


}
