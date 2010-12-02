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
package org.sakaiproject.nakamura.api.message;

/**
 *
 */
public interface MessageConstants {

  /**
   *
   */
  public static final String SAKAI_MESSAGESTORE_RT = "sakai/messagestore";

  /**
  *
  */
  public static final String PROP_SAKAI_MESSAGEBOX = "sakai:messagebox";
  /**
  *
  */
  public static final String SAKAI_MESSAGE_RT = "sakai/message";
  /**
   * The property wether this message has been read or not.
   */
  public static final String PROP_SAKAI_READ = "sakai:read";
  /**
 *
 */
  public static final String PROP_SAKAI_SENDSTATE = "sakai:sendstate";

  /**
   * This property will hold the value for the id of this message.
   */
  public static final String PROP_SAKAI_ID = "sakai:id";

  /**
   * The value for this property will define what kind of message this is. ex: internal,
   * email, ..
   */
  public static final String PROP_SAKAI_TYPE = "sakai:type";
  /**
   * This property will hold the value to send the message to.
   */
  public static final String PROP_SAKAI_TO = "sakai:to";
  /**
   * This property will hold the value for whom this message sent.
   */
  public static final String PROP_SAKAI_FROM = "sakai:from";
  /**
   * This property will hold the value for the subject.
   */
  public static final String PROP_SAKAI_SUBJECT = "sakai:subject";
  /**
   * This property will hold the value for the body.
   */
  public static final String PROP_SAKAI_BODY = "sakai:body";
  /**
   * This property will hold the path to the previous message (starts after the message
   * store.)
   */
  public static final String PROP_SAKAI_PREVIOUS_MESSAGE = "sakai:previousmessage";
  /**
   * This property will hold the number of times message delivery has been retried
   */
  public static final String PROP_SAKAI_RETRY_COUNT = "sakai:retrycount";


  /**
   * Value for a date.
   */
  public static final String PROP_SAKAI_CREATED = "sakai:created";

  /**
   * The name for the outbox box.
   */
  public static final String BOX_OUTBOX = "outbox";
  /**
   * The name for the inbox box.
   */
  public static final String BOX_INBOX = "inbox";
  /**
   * The name for the sent box.
   */
  public static final String BOX_SENT = "sent";
  /**
  *
  */
  public static final String STATE_NONE = "none";
  /**
  *
  */
  public static final String STATE_NOTIFIED = "notified";
  /**
  *
  */
  public static final String STATE_PENDING = "pending";
  /**
   *
   */
  public static final String PENDINGMESSAGE_EVENT = "org/sakaiproject/nakamura/message/pending";
  /**
   *
   */
  public static final String EVENT_LOCATION = "location";
  
  /**
   * JCR folder name for messages.
   */
  public static final String FOLDER_MESSAGES = "message";
  
  /**
   * JCR folder name for chat logs.
   */
  public static final String FOLDER_CHATS = "chatlogs";

  /**
   * Identifier for an internal message.
   */
  public static final String TYPE_INTERNAL = "internal";

  /**
   * Identifier for a chat message.
   */
  public static final String TYPE_CHAT = "chat";

  /**
   * Identifier for a email message.
   */
  public static final String TYPE_SMTP = "smtp";


  /**
   * The user message path.
   */
  public static final String SEARCH_PROP_MESSAGESTORE = "_userMessagePath";
  public static final String SEARCH_PROP_MESSAGEROOT = "_messageStoreRoot";

  public static final String PROP_SAKAI_MESSAGEERROR = "sakai:messageError";

  public static final String PROP_SAKAI_CONTENT_TYPE = "sakai:contentType";

  public static final String PROP_SAKAI_ATTACHMENT_DESCRIPTION = "sakai:attachmentDescription";

  public static final String PROP_SAKAI_ATTACHMENT_CONTENT = "sakai:attachmentContent";



  /**
   * Values for the preprocessor
   */
  public static final String REG_PROCESSOR_NAMES = "sakai.message.createpreprocessor";
  public static final String MESSAGE_CREATE_PREPROCESSOR = "CreateMessagePreProcessor";

  public static final int CLEAUNUP_EVERY_X_SECONDS = 7200;
}
