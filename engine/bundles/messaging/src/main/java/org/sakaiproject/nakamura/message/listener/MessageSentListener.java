package org.sakaiproject.nakamura.message.listener;

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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessageRouterManager;
import org.sakaiproject.nakamura.api.message.MessageRoutes;
import org.sakaiproject.nakamura.api.message.MessageTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

@Component(inherit = true, label = "%sakai-event.name", immediate = true)
@Service
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Event Handler Listening to Pending Messages Events."),
    @Property(name = "event.topics", value = "org/sakaiproject/nakamura/message/pending")})
@Reference(name = "MessageTransport", referenceInterface = MessageTransport.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, bind = "addTransport", unbind = "removeTransport")
public class MessageSentListener implements EventHandler {
  private static final Logger LOG = LoggerFactory.getLogger(MessageSentListener.class);

  /**
   * This will contain all the transports.
   */
  private Map<MessageTransport, MessageTransport> transports = new ConcurrentHashMap<MessageTransport, MessageTransport>();

  @Reference
  private transient MessageRouterManager messageRouterManager;

  @Reference
  private transient SlingRepository slingRepository;

  /**
   * Default constructor for use by OSGi container.
   */
  public MessageSentListener() {
  }

  /**
   * Parameterized constructor for use in unit tests.
   *
   * @param messageRouterManager
   * @param slingRepository
   */
  MessageSentListener(MessageRouterManager messageRouterManager,
      SlingRepository slingRepository) {
    this.messageRouterManager = messageRouterManager;
    this.slingRepository = slingRepository;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
   */
  public void handleEvent(Event event) {

    // Get the message
    // get the node, call up the appropriate handler and pass off based on
    // message type
    LOG.debug("handleEvent called");
    Session session = null;
    try {
      session = slingRepository.loginAdministrative(null);
      String path = (String) event.getProperty(MessageConstants.EVENT_LOCATION);
      Node n = (Node) session.getItem(path);
      String resourceType = n.getProperty(
          JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY).getString();
      if (resourceType.equals(MessageConstants.SAKAI_MESSAGE_RT)) {

        MessageRoutes routes = messageRouterManager.getMessageRouting(n);

        for (MessageTransport transport : transports.values()) {
          transport.send(routes, event, n);
        }
      }
    } catch (LoginException e) {
      LOG.error(e.getMessage(), e);
    } catch (RepositoryException e) {
      LOG.error(e.getMessage(), e);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
  }

  /**
   * @param handler
   */
  protected void removeTransport(MessageTransport transport) {
    transports.remove(transport);
  }

  /**
   * @param handler
   */
  protected void addTransport(MessageTransport transport) {
    transports.put(transport,transport);
  }


}
