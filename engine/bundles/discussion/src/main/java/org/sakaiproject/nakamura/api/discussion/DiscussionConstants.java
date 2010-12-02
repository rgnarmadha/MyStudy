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

package org.sakaiproject.nakamura.api.discussion;

public interface DiscussionConstants {

  /**
   * The type of a discussion.
   */
  public static final String TYPE_DISCUSSION = "discussion";
  
  /**
   * A marker on a message. This identifies a thread.
   */
  public static final String PROP_MARKER = "sakai:marker";

  /**
   * If a post has been deleted.
   */
  public static final String PROP_DELETED = "sakai:deleted";

  /**
   * Holds the value of people who editted this message.
   */
  public static final String PROP_EDITEDBY = "sakai:editedby";
  /**
   * Holds the profile info for the editters.
   */
  public static final String PROP_EDITEDBYPROFILES = "sakai:editedbyprofiles";

  /**
   * Holds the ID of the message we want to reply on.
   */
  public static final String PROP_REPLY_ON = "sakai:replyon";


  /**
   * The property that marks a post as an initial post.
   */
  public static final String PROP_INITIAL_POST = "sakai:initialpost";

  /**
   * The property that determines if an email should be sent when someone leaves a post.
   */
  public static final String PROP_NOTIFICATION = "sakai:notification";

  public static final String PROP_NOTIFY_ADDRESS = "sakai:notificationaddress";

  public static final String TOPIC_DISCUSSION_MESSAGE = "org/sakaiproject/nakamura/message/discussion";

}
