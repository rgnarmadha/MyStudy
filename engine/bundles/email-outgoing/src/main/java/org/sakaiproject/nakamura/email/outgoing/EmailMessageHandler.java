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
package org.sakaiproject.nakamura.email.outgoing;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.events.EventDeliveryConstants;
import org.sakaiproject.nakamura.api.events.EventDeliveryConstants.EventDeliveryMode;
import org.sakaiproject.nakamura.api.events.EventDeliveryConstants.EventMessageMode;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessageRoute;
import org.sakaiproject.nakamura.api.message.MessageRoutes;
import org.sakaiproject.nakamura.api.message.MessageTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Handler for messages that are intended for email delivery. Needs to be started
 * immediately to make sure it registers with JCR as soon as possible.
 */
@Component(label = "%external.message.handler.name", description = "%external.message.handler.description", immediate = true)
@Service
public class EmailMessageHandler implements MessageTransport {

  private static final Logger LOGGER = LoggerFactory.getLogger(EmailMessageHandler.class);

  @Reference
  protected EventAdmin eventAdmin;

  private static final String TYPE = MessageConstants.TYPE_SMTP;

  public String getType() {
    return TYPE;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.message.MessageTransport#send(org.sakaiproject.nakamura.api.message.MessageRoutes,
   *      org.osgi.service.event.Event, javax.jcr.Node)
   */
  public void send(MessageRoutes routes, Event event, Node n) {
    LOGGER.debug("Started handling an email message");

    // delay list instantiation to save object creation when not needed.
    List<String> recipients = null;
    for (MessageRoute route : routes) {
      if (TYPE.equals(route.getTransport())) {
        if (recipients == null) {
          recipients = new ArrayList<String>();
        }
        recipients.add(route.getRcpt());
      }
    }

    if (recipients != null) {
      Properties props = new Properties();
      try {
        if ( event != null ) {
          for( String propName : event.getPropertyNames()) {
            Object propValue = event.getProperty(propName);
            props.put(propName, propValue);
          }
        }
        // make the message deliver to one listener, that means the desination must be a queue.
        props.put(EventDeliveryConstants.DELIVERY_MODE, EventDeliveryMode.P2P);
        // make the message persistent to survive restarts.
        props.put(EventDeliveryConstants.MESSAGE_MODE, EventMessageMode.PERSISTENT);
        props.put(OutgoingEmailMessageListener.RECIPIENTS, recipients);
        props.put(OutgoingEmailMessageListener.NODE_PATH_PROPERTY, n.getPath());
        Event emailEvent = new Event(OutgoingEmailMessageListener.QUEUE_NAME, props);

        LOGGER.debug("Sending event [" + emailEvent + "]");
        eventAdmin.postEvent(emailEvent);
      } catch (RepositoryException e) {
        LOGGER.error(e.getMessage(), e);
      }
    }
  }

  protected void bindEventAdmin(EventAdmin eventAdmin) {
    this.eventAdmin = eventAdmin;
  }
}
