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
package org.sakaiproject.nakamura.api.site;

/**
 * Constants that will be used troughout the site templating engine.
 */
public interface SiteConstants {

  /**
   * The name for the top node that will contain all the authorizables that are part of
   * this site.
   */
  public static final String AUTHORIZABLES_SITE_NODENAME = "authorizables";

  /**
   * The name for the the node that represents a single authorizable in the {@see
   * SiteConstants.AUTHORIZABLES_SITE_NODENAME}.
   */
  public static final String AUTHORIZABLES_SITE_NODENAME_SINGLE = "authorizable";

  /*
   * Template variables.
   */

  /**
   * The resource type of a group node defined in the template.
   */
  public static final String RT_GROUPS = "sakai/template-group";

  /**
   * The property that defines what the name of a group should be when it gets created.
   */
  public static final String AUTHORIZABLES_SITE_PRINCIPAL_NAME = "sakai:template-group-principalname";

  /**
   * The property that defines what the managers of a group should be when it gets
   * created.
   */
  public static final String GROUPS_PROPERTY_MANAGERS = "sakai:template-group-managers";

  /**
   * The property that defines what the viewers of a group should be when it gets created.
   */
  public static final String GROUPS_PROPERTY_VIEWERS = "sakai:template-group-viewers";

  /**
   * The property that defines if this group will be a maintainer or not.
   */
  public static final String AUTHORIZABLES_SITE_IS_MAINTAINER = "sakai:template-group-issitemanager";

  /**
   * The resource type of an ACE node defined in the template.
   */
  public static final String RT_ACE = "sakai/template-ace";

  /**
   *
   */
  public static final String RT_SITE_AUTHORIZABLE = "sakai/site-group";

  /**
   * The property name of the site template.
   */
  public static final String SAKAI_SITE_TEMPLATE = "sakai:site-template";
  /**
   * The property name for the skin of a website.
   */
  public static final String SAKAI_SKIN = "sakai:skin";
  /**
   * The sling resource type for a site.
   */
  public static final String SITE_RESOURCE_TYPE = "sakai/site";
  /**
   * The property name that defines if this is a site template or not.
   */
  public static final String SAKAI_IS_SITE_TEMPLATE = "sakai:is-site-template";
  /**
   * The property used to store the joinable status of the site.
   */
  public static final String JOINABLE = "sakai:joinable";

  /**
   * The property used to store the type of site.(e.g. portfolio, course, other)
   */
  public static final String SAKAI_SITE_TYPE = "sakai:site-type";

  /**
   * The property used to store the sites a group is associated with.
   */
  public static final String SITES = "sakai:site";
  /**
   * The property returned with the groups info in the members listing (this is not and
   * should not actually be stored on the node, it is generated on demand)
   */
  public static final String MEMBER_GROUPS = "member:groups";

  /**
   * The multivalued property to store the list of associated authorizables.
   */
  public static final String AUTHORIZABLE = "sakai:authorizables";
  /**
   * The request parameter indicating the target group for join and unjoin operations.
   */
  public static final String PARAM_GROUP = "targetGroup";
  public static final String PARAM_START = "start";
  public static final String PARAM_ITEMS = "items";
  public static final String PARAM_SORT = "sort";
  public static final String PARAM_ADD_GROUP = "addauth";
  public static final String PARAM_REMOVE_GROUP = "removeauth";

  /**
   * Request parameters used when creating a site.
   */
  public static final String PARAM_SITE_PATH = ":sitepath";
  public static final String PARAM_MOVE_FROM = ":moveFrom";
  public static final String PARAM_COPY_FROM = ":copyFrom";
  public static final String SITES_CONTAINER_RESOURCE_TYPE = "sakai/sites";

}