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

package org.sakaiproject.nakamura.chat;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Services;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.event.Event;
import org.sakaiproject.nakamura.api.chat.ChatManagerService;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessageProfileWriter;
import org.sakaiproject.nakamura.api.message.MessageRoute;
import org.sakaiproject.nakamura.api.message.MessageRoutes;
import org.sakaiproject.nakamura.api.message.MessageTransport;
import org.sakaiproject.nakamura.api.message.MessagingService;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Handler for chat messages.
 */
@Component(label = "ChatMessageHandler", description = "Handler for internally delivered chat messages.", immediate = true)
@Services(value = { @Service(value = MessageTransport.class),
    @Service(value = MessageProfileWriter.class) })
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Handler for internally delivered chat messages") })
public class ChatMessageHandler implements MessageTransport, MessageProfileWriter {
  private static final Logger LOG = LoggerFactory.getLogger(ChatMessageHandler.class);
  private static final String TYPE = MessageConstants.TYPE_CHAT;
  private static final Object CHAT_TRANSPORT = "chat";

  @Reference
  protected transient ChatManagerService chatManagerService;

  @Reference
  protected transient SlingRepository slingRepository;

  @Reference
  protected transient MessagingService messagingService;

  @Reference
  protected transient ProfileService profileService;

  /**
   * Default constructor
   */
  public ChatMessageHandler() {
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.message.MessageTransport#send(org.sakaiproject.nakamura.api.message.MessageRoutes,
   *      org.osgi.service.event.Event, javax.jcr.Node)
   */
  public void send(MessageRoutes routes, Event event, Node originalMessage) {
    Session session = null;
    try {

      session = slingRepository.loginAdministrative(null); // usage checked and Ok
      // KERN-577

      for (MessageRoute route : routes) {
        if (CHAT_TRANSPORT.equals(route.getTransport())) {
          LOG.info("Started handling a message.");
          String rcpt = route.getRcpt();
          // the path were we want to save messages in.
          String messageId = originalMessage.getProperty(MessageConstants.PROP_SAKAI_ID)
              .getString();
          String toPath = messagingService.getFullPathToMessage(rcpt, messageId, session);

          // Copy the node into the user his folder.
          JcrUtils.deepGetOrCreateNode(session, toPath.substring(0, toPath
              .lastIndexOf("/")));
          session.save();
          session.getWorkspace().copy(originalMessage.getPath(), toPath);
          Node n = JcrUtils.deepGetOrCreateNode(session, toPath);

          // Add some extra properties on the just created node.
          n.setProperty(MessageConstants.PROP_SAKAI_READ, false);
          n.setProperty(MessageConstants.PROP_SAKAI_TO, rcpt);
          n.setProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX,
              MessageConstants.BOX_INBOX);
          n.setProperty(MessageConstants.PROP_SAKAI_SENDSTATE,
              MessageConstants.STATE_NOTIFIED);
          n.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
              MessageConstants.SAKAI_MESSAGE_RT);
          session.save();

          long time = System.currentTimeMillis();
          Calendar cal = originalMessage.getProperty(MessageConstants.PROP_SAKAI_CREATED)
              .getDate();
          time = cal.getTimeInMillis();

          String from = originalMessage.getProperty(MessageConstants.PROP_SAKAI_FROM)
              .getString();

          // Set the rcpt in the cache.
          chatManagerService.put(rcpt, time);
          // Set the from in the cache
          chatManagerService.put(from, time);

        }
      }

    } catch (RepositoryException e) {
      LOG.error(e.getMessage(), e);
    } finally {
      if ( session != null ) {
        session.logout();
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.message.MessageProfileWriter#writeProfileInformation(javax.jcr.Session,
   *      java.lang.String, org.apache.sling.commons.json.io.JSONWriter)
   */
  public void writeProfileInformation(Session session, String recipient, JSONWriter write) {
    try {
      UserManager um = AccessControlUtil.getUserManager(session);
      Authorizable au = um.getAuthorizable(recipient);
      ValueMap map = profileService.getCompactProfileMap(au, session);
      ((ExtendedJSONWriter) write).valueMap(map);
    } catch (Exception e) {
      LOG.error("Failed to write profile information for " + recipient, e);
    }
  }

  /**
   * Determines what type of messages this handler will process. {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.message.MessageHandler#getType()
   */
  public String getType() {
    return TYPE;
  }

}
