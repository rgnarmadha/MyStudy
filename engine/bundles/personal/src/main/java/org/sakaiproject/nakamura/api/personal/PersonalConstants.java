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
package org.sakaiproject.nakamura.api.personal;

/**
 *
 */
public interface PersonalConstants {

  /**
   * The base location of the user space.
   */
  public static final String _USER = "/_user";
  /**
   * The base location of the group space.
   */
  public static final String _GROUP = "/_group";
  /**
   * The name of the private folder
   */
  public static final String PRIVATE = "private";
  /**
   * The name of the public folder
   */
  public static final String PUBLIC = "public";

  /**
   * The resource type of personal private stores.
   */
  public static final String USER_PRIVATE_RESOURCE_TYPE = "sakai/personalPrivate";
  /**
   * The resource type for public personal stores
   */
  public static final String USER_PUBLIC_RESOURCE_TYPE = "sakai/personalPublic";

  /**
   * The resource type for private group stores
   */
  public static final String GROUP_PRIVATE_RESOURCE_TYPE = "sakai/groupPrivate";
  /**
   * The resource type for the public group store.
   */
  public static final String GROUP_PUBLIC_RESOURCE_TYPE = "sakai/groupPublic";

  /**
  *
  */
  public static final String PERSONAL_OPERATION = "org.sakaiproject.nakamura.personal.operation";

  /**
   * The node name of the authentication profile in public space.
   */
  public static final String AUTH_PROFILE = "authprofile";

  /**
   * Property name for the e-mail property of a user's profile
   */
  public static final String EMAIL_ADDRESS = "email";

  /**
   * Property name for the user's preferred means of message delivery
   */
  public static final String PREFERRED_MESSAGE_TRANSPORT = "preferredMessageTransport";

  /**
   * Parameter which can deliver a JSON string to a content importer to configure a new profile.
   */
  public static final String PROFILE_JSON_IMPORT_PARAMETER = ":sakai:profile-import";

  /**
   * Property name for the full title/name of a group.
   * ie:
   * Title: The 2010 Mathematics 101 class.
   * Authorizable id: the-2010-mathematics-101-class
   */
  public static final String GROUP_TITLE_PROPERTY = "sakai:group-title";

  /**
   * Property name for the description of a group.
   */
  public static final String GROUP_DESCRIPTION_PROPERTY = "sakai:group-description";

  /**
   * Available default access settings for a new User or Group.
   */
  public static final String VISIBILITY_PRIVATE = "private";
  public static final String VISIBILITY_LOGGED_IN = "logged_in";
  public static final String VISIBILITY_PUBLIC = "public";
}
