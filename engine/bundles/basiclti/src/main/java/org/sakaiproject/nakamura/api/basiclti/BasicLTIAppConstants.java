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
package org.sakaiproject.nakamura.api.basiclti;

public class BasicLTIAppConstants {
  // local service/widget semantics
  public static final String LTI_VTOOL_ID = "lti_virtual_tool_id";
  public static final String LTI_URL = "ltiurl";
  public static final String LTI_URL_LOCK = "ltiurl_lock";
  public static final String LTI_SECRET = "ltisecret";
  public static final String LTI_SECRET_LOCK = "ltisecret_lock";
  public static final String LTI_KEY = "ltikey";
  public static final String LTI_KEY_LOCK = "ltikey_lock";
  public static final String FRAME_HEIGHT = "frame_height";
  public static final String FRAME_HEIGHT_LOCK = "frame_height_lock";
  public static final String DEBUG = "debug";
  public static final String DEBUG_LOCK = "debug_lock";
  public static final String RELEASE_NAMES = "release_names";
  public static final String RELEASE_NAMES_LOCK = "release_names_lock";
  public static final String RELEASE_EMAIL = "release_email";
  public static final String RELEASE_EMAIL_LOCK = "release_email_lock";
  public static final String RELEASE_PRINCIPAL_NAME = "release_principal_name";
  public static final String RELEASE_PRINCIPAL_NAME_LOCK = "release_principal_name_lock";
  /**
   * The location used to store admin level settings which cannot be overridden by user
   * placements.
   */
  public static final String ADMIN_CONFIG_PATH = "/var/basiclti";

  /**
   * The path to the node that will contain the system wide settings.
   */
  public static final String GLOBAL_SETTINGS = ADMIN_CONFIG_PATH + "/" + "globalSettings";
  /**
   * The name of the child node that stores sensitive LTI information.
   */
  public static final String LTI_ADMIN_NODE_NAME = "ltiKeys";

  /**
   * A new sakai/basiclti node is created.
   */
  public static final String TOPIC_BASICLTI_ADDED = "org/sakaiproject/nakamura/basiclti/added";
  /**
   * A sakai/basiclti node is changed.
   */
  public static final String TOPIC_BASICLTI_CHANGED = "org/sakaiproject/nakamura/basiclti/changed";
  /**
   * A sakai/basiclti node is removed.
   */
  public static final String TOPIC_BASICLTI_REMOVED = "org/sakaiproject/nakamura/basiclti/removed";
  /**
   * A sakai/basiclti node is accessed.
   */
  public static final String TOPIC_BASICLTI_ACCESSED = "org/sakaiproject/nakamura/basiclti/accessed";
  /**
   * A sakai/basiclti node is launched.
   */
  public static final String TOPIC_BASICLTI_LAUNCHED = "org/sakaiproject/nakamura/basiclti/launched";
}
