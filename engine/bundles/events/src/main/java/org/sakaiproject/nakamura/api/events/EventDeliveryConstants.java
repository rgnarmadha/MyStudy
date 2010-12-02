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
package org.sakaiproject.nakamura.api.events;


/**
 * Constants associated with Event Delivery
 */
public interface EventDeliveryConstants {

  /**
   * Acknowledgment mode for the delivery of the event message from the dispatch thread
   * to the Message transport fabric.
   */
  public enum EventAcknowledgeMode {
    /**
     * Same meaning as {@see Session#AUTO_ACKNOWLEDGE}
     */
    AUTO_ACKNOWLEDGE(),
    /**
     * Same meaning as {@see Session#CLIENT_ACKNOWLEDGE}
     */
    CLIENT_ACKNOWLEDGE(),
    /**
     * Same meaning as {@see Session#DUPS_OK_ACKNOWLEDGE}
     */
    DUPS_OK_ACKNOWLEDGE();

  }

  /**
   * Storage mode for the event message.
   */
  public enum EventMessageMode {
    /**
     * The event message is persistent and will be stored for later delviery if
     * listener/delivery criteria are not met.
     */
    PERSISTENT(),
    /**
     * The event message is not persistent and if not delivered immediately will be thrown
     * away.
     */
    NON_PERSISTENT();

  }

  /**
   * The delivery mode used for the event message.
   */
  public enum EventDeliveryMode {
    /**
     * Delivery mode is broadcast (ie in a JMS Topic)
     */
    BROADCAST(),
    /**
     * Delviery mode is point to point (ie in a JMS Queue)
     */
    P2P();

  }

  /**
   * The name of the property for specifying the delivery mode, the object associated with
   * this property must be a {@link EventDeliveryMode} enum value.
   */
  public static final String DELIVERY_MODE = "sakai:event-deliverymode";
  /**
   * The name of the property for specifying the message storage mode, the object value of
   * this property must he a {@link EventMessageMode} enum value.
   */
  public static final String MESSAGE_MODE = "sakai:event-messagemode";
  /**
   * The name ofthe property for specifying the acknowledgement from the message fabric.
   * The object value of this property must be a {@link EventAcknowledgeMode} enum value.
   */
  public static final String ACKNOWLEDGE_MODE = "sakai:event-acknowledgemode";

}
