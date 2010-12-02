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

package org.sakaiproject.nakamura.discussion;

import static javax.jcr.security.Privilege.JCR_READ;
import static javax.jcr.security.Privilege.JCR_REMOVE_NODE;
import static javax.jcr.security.Privilege.JCR_WRITE;
import static org.apache.sling.jcr.base.util.AccessControlUtil.replaceAccessControlEntry;
import static org.sakaiproject.nakamura.api.discussion.DiscussionConstants.TOPIC_DISCUSSION_MESSAGE;
import static org.sakaiproject.nakamura.api.message.MessageConstants.BOX_INBOX;
import static org.sakaiproject.nakamura.api.message.MessageConstants.PROP_SAKAI_FROM;
import static org.sakaiproject.nakamura.api.message.MessageConstants.PROP_SAKAI_ID;
import static org.sakaiproject.nakamura.api.message.MessageConstants.PROP_SAKAI_MESSAGEBOX;
import static org.sakaiproject.nakamura.api.message.MessageConstants.PROP_SAKAI_SENDSTATE;
import static org.sakaiproject.nakamura.api.message.MessageConstants.PROP_SAKAI_TO;
import static org.sakaiproject.nakamura.api.message.MessageConstants.PROP_SAKAI_TYPE;
import static org.sakaiproject.nakamura.api.message.MessageConstants.STATE_NOTIFIED;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.discussion.DiscussionConstants;
import org.sakaiproject.nakamura.api.locking.LockManager;
import org.sakaiproject.nakamura.api.locking.LockTimeoutException;
import org.sakaiproject.nakamura.api.message.MessageRoute;
import org.sakaiproject.nakamura.api.message.MessageRoutes;
import org.sakaiproject.nakamura.api.message.MessageTransport;
import org.sakaiproject.nakamura.api.message.MessagingException;
import org.sakaiproject.nakamura.api.message.MessagingService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.sakaiproject.nakamura.util.osgi.EventUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Handler for messages that are sent locally and intended for local delivery. Needs to be
 * started immediately to make sure it registers with JCR as soon as possible.
 */
@Component(immediate = true, label = "%discussion.messageTransport.label", description = "%discussion.messageTransport.desc")
@Service
public class DiscussionMessageTransport implements MessageTransport {
  private static final Logger LOG = LoggerFactory
      .getLogger(DiscussionMessageTransport.class);
  private static final String TYPE = DiscussionConstants.TYPE_DISCUSSION;

  @Reference
  protected transient SlingRepository slingRepository;

  @Reference
  protected transient MessagingService messagingService;
  
  @Reference
  protected transient LockManager lockManager;

  @Reference
  protected transient EventAdmin eventAdmin;

  @org.apache.felix.scr.annotations.Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  /**
   * Due to the fact that we are setting ACLs it is hard to unit test this class.
   * If this variable is set to true, than the ACL settings will be omitted.
   */
  private boolean testing = false;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.message.MessageTransport#send(org.sakaiproject.nakamura.api.message.MessageRoutes,
   *      org.osgi.service.event.Event, javax.jcr.Node)
   */
  public void send(MessageRoutes routes, Event event, Node originalMessage) {
    Session session = null;
    try {
      // Login as admin.
      session = slingRepository.loginAdministrative(null);

      for (MessageRoute route : routes) {
        if (DiscussionConstants.TYPE_DISCUSSION.equals(route.getTransport())) {
          String rcpt = route.getRcpt();
          // the path were we want to save messages in.
          String messageId = originalMessage.getProperty(PROP_SAKAI_ID).getString();
          String toPath = messagingService.getFullPathToMessage(rcpt, messageId, session);
          
          try {
            lockManager.waitForLock(toPath);
          } catch (LockTimeoutException e) {
            throw new MessagingException("Unable to lock discussion widget message store");
          }
          // Copy the node to the destination
          Node newMessageNode = JcrUtils.deepGetOrCreateNode(session, toPath);

          PropertyIterator pi = originalMessage.getProperties();
          while (pi.hasNext()) {
            Property p = pi.nextProperty();
            if (!p.getName().contains("jcr:"))
              newMessageNode.setProperty(p.getName(), p.getValue());
          }

          // Add some extra properties on the just created node.
          newMessageNode.setProperty(PROP_SAKAI_TYPE, route.getTransport());
          newMessageNode.setProperty(PROP_SAKAI_TO, route.getRcpt());
          newMessageNode.setProperty(PROP_SAKAI_MESSAGEBOX, BOX_INBOX);
          newMessageNode.setProperty(PROP_SAKAI_SENDSTATE, STATE_NOTIFIED);

          if (!testing) {
            // This will probably be saved in a site store. Not all the users will have
            // access to their message. So we add an ACL that allows the user to edit and
            // delete it later on.
            String from = originalMessage.getProperty(PROP_SAKAI_FROM).getString();
            Authorizable authorizable = AccessControlUtil.getUserManager(session)
                .getAuthorizable(from);
            replaceAccessControlEntry(session, toPath, authorizable.getPrincipal(),
                new String[] { JCR_WRITE, JCR_READ, JCR_REMOVE_NODE }, null, null, null);
          }
          if (session.hasPendingChanges()) {
            session.save();
          }

          try {
            // Send an OSGi event. The value of the selector is the last part of the event
            // topic.
            final Dictionary<String, String> properties = new Hashtable<String, String>();
            properties.put(UserConstants.EVENT_PROP_USERID, route.getRcpt());
            properties.put("from", newMessageNode.getProperty(PROP_SAKAI_FROM).getString());
            EventUtils.sendOsgiEvent(properties, TOPIC_DISCUSSION_MESSAGE, eventAdmin);
          } catch (Exception e) {
            // Swallow all exceptions, but leave a note in the error log.
            LOG.error("Failed to send OSGi event for discussion", e);
          }
        }
      }
    } catch (RepositoryException e) {
      LOG.error(e.getMessage(), e);
    } finally {
      lockManager.clearLocks();
      if (session != null) {
        session.logout();
      }
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

  /**
   * This method should only be called for unit testing purposses. It will disable the ACL
   * settings.
   */
  protected void activateTesting() {
    testing = true;
  }

}
