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
package org.sakaiproject.nakamura.api.calendar;

/**
 *
 */
public interface CalendarConstants {

  /**
   * The resource type value for a calendar.
   */
  public static final String SAKAI_CALENDAR_RT = "sakai/calendar";

  /**
   * The nodename for the default calendar the gets created in a user his home folder.
   */
  public static final String SAKAI_CALENDAR_NODENAME = "calendar";

  /**
   * The resource type value for a vevent.
   */
  public static final String SAKAI_CALENDAR_EVENT_RT = "sakai/calendar-event";

  public static final String SAKAI_CALENDAR_PROPERTY_PREFIX = "sakai:vcal-";

  /*
   * Signup related constants*******
   */

  /**
   * The node name for the signup node that is located under a calendar event node
   */
  public static final String SIGNUP_NODE_NAME = "signup";
  /**
   * The sling resource type value for a signup node.
   */
  public static final String SIGNUP_NODE_RT = "sakai/calendar-signup";

  /**
   * The node name for the particpants node that is located under signup node
   */
  public static final String PARTICIPANTS_NODE_NAME = "participants";

  /**
   * The sling resourceType for a participant.
   */
  public static final String SAKAI_EVENT_SIGNUP_PARTICIPANT_RT = "sakai/event-signup-participant";

  /**
   * The property name for the signed up user.
   */
  public static final String SAKAI_USER = "sakai:user";

  /**
   * The property name for the signed up date.
   */
  public static final String SAKAI_SIGNEDUP_DATE = "sakai:event-signupdate";
  /**
   * The property name for the original event that a user signs up for. This property will
   * hold the uuid of that original node.
   */
  public static final String SAKAI_SIGNEDUP_ORIGINAL_EVENT = "sakai:original-event";
  
  public static final String TOPIC_CALENDAR_SIGNUP = "org/sakaiproject/nakamura/calendar/signup";
}
