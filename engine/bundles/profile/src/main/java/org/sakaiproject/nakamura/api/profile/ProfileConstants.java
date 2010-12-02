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
package org.sakaiproject.nakamura.api.profile;

import org.apache.sling.jcr.resource.JcrResourceConstants;

/**
 *
 */
public interface ProfileConstants {

  /**
   * The value for the {@link JcrResourceConstants#SLING_RESOURCE_TYPE_PROPERTY} of a
   * group's authprofile.
   */
  public static final String GROUP_PROFILE_RT = "sakai/group-profile";

  /**
   * The prefix for the group storage (without hashing.)
   */
  public static final String GROUP_JCR_PATH_PREFIX = "/_group";

  /**
   * The name of the property that holds the value of identifier (authorizable ID) for
   * this profile.
   */
  public static final String GROUP_IDENTIFIER_PROPERTY = "rep:groupId";

  /**
   * Property name for the full title/name of a group. ie: Title: The 2010 Mathematics 101
   * class. Authorizable id: the-2010-mathematics-101-class
   */
  public static final String GROUP_TITLE_PROPERTY = "sakai:group-title";

  /**
   * Property name for the description of a group.
   */
  public static final String GROUP_DESCRIPTION_PROPERTY = "sakai:group-description";

  /**
   * The value for the {@link JcrResourceConstants#SLING_RESOURCE_TYPE_PROPERTY} of a
   * user's authprofile.
   */
  public static final String USER_PROFILE_RT = "sakai/user-profile";

  /**
   * The prefix for the user storage (without hashing.)
   */
  public static final String USER_JCR_PATH_PREFIX = "/_user";
  /**
   * The name of the property that holds the value of identifier (authorizable ID) for
   * this profile.
   */
  public static final String USER_IDENTIFIER_PROPERTY = "rep:userId";

  /**
   * The property name that holds the given name of a user.
   */
  public static final String USER_FIRSTNAME_PROPERTY = "firstName";

  /**
   * The property name that holds the family name of a user.
   */
  public static final String USER_LASTNAME_PROPERTY = "lastName";

  /**
   * The property name that holds the email of a user.
   */
  public static final String USER_EMAIL_PROPERTY = "email";
  
  /**
   * The property name that holds the picture location for a user.
   */
  public static final String USER_PICTURE = "picture";
  
  /**
   * The property name that holds the basic nodes for a user.
   */
  public static final String USER_BASIC = "basic";
  
  /**
   * The property name that holds the basic nodes for a user.
   */
  public static final String USER_BASIC_ELEMENTS = "elements";
  
  /**
   * The property name that holds the access information for a user.
   */
  public static final String USER_BASIC_ACCESS = "access";
}
