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
package org.sakaiproject.nakamura.api.activity;

/**
 * Constants that are used throughout the activity bundle.
 */
public interface ActivityConstants {
  
  // Request parameters, node property names
  
  /**
   * The property/parameter name for the application id.
   */
  public static final String PARAM_APPLICATION_ID = "sakai:activity-appid";
  /**
   * The property/parameter name for the template id.
   */
  public static final String PARAM_TEMPLATE_ID = "sakai:activity-templateid";
  /**
   * The property name for the authorizable who generated the event.
   */
  public static final String PARAM_ACTOR_ID = "sakai:activity-actor";
  /**
   * The property name for the source of the activity.
   */
  public static final String PARAM_SOURCE = "sakai:activity-source";
  
  
  // Node names
  
  /**
   * The name for the big store where the original activities will be stored.
   */
  public static final String ACTIVITY_STORE_NAME = "activity";
  /**
   * The name for the big store where the original activities will be copied to.
   */
  public static final String ACTIVITY_FEED_NAME = "activityFeed";

  /**
   * JCR folder name for templates.
   */
  public static final String TEMPLATE_ROOTFOLDER = "/var/activity/templates";
  
  // Sling:resourceTypes
  
  /**
   * The sling:resourceType for an activity store. 
   * The node with this resourceType will
   * hold the original activity items.
   */
  public static final String ACTIVITY_STORE_RESOURCE_TYPE = "sakai/activityStore";
  /**
   * The sling:resourceType for an activity feed.
   * The node with this resourceType will be the store 
   * where the activities are copied to.
   */
  public static final String ACTIVITY_FEED_RESOURCE_TYPE = "sakai/activityFeed";
  /**
   * The sling:resourceType for an activity item.
   */
  public static final String ACTIVITY_ITEM_RESOURCE_TYPE = "sakai/activity";
  
  // Events
  
  /**
   * OSGi event that gets triggered when an activity occurs.
   */
  public static final String EVENT_TOPIC = "org/sakaiproject/nakamura/activity";
  /**
   * The property in the event which will hold the location to the original activity.
   */
  public static final String EVENT_PROP_PATH = "sakai:activity-item-path";

}
