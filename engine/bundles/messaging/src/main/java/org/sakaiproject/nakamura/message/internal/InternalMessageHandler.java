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

package org.sakaiproject.nakamura.message.internal;

import static javax.jcr.security.Privilege.JCR_READ;
import static javax.jcr.security.Privilege.JCR_REMOVE_NODE;
import static javax.jcr.security.Privilege.JCR_WRITE;
import static org.apache.sling.jcr.base.util.AccessControlUtil.replaceAccessControlEntry;
import static org.sakaiproject.nakamura.api.message.MessageConstants.PROP_SAKAI_FROM;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Services;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.osgi.service.event.Event;
import org.sakaiproject.nakamura.api.locking.LockManager;
import org.sakaiproject.nakamura.api.locking.LockTimeoutException;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessageProfileWriter;
import org.sakaiproject.nakamura.api.message.MessageRoute;
import org.sakaiproject.nakamura.api.message.MessageRoutes;
import org.sakaiproject.nakamura.api.message.MessageTransport;
import org.sakaiproject.nakamura.api.message.MessagingException;
import org.sakaiproject.nakamura.api.message.MessagingService;
import org.sakaiproject.nakamura.api.presence.PresenceService;
import org.sakaiproject.nakamura.api.presence.PresenceUtils;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.site.SiteException;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

/**
 * Handler for messages that are sent locally and intended for local delivery. Needs to be
 * started immediately to make sure it registers with JCR as soon as possible.
 */
@Component(immediate = true, label = "InternalMessageHandler", description = "Handler for internally delivered messages.")
@Services(value = { @Service(value = MessageTransport.class),
    @Service(value = MessageProfileWriter.class) })
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Handler for internally delivered messages.") })
public class InternalMessageHandler implements MessageTransport, MessageProfileWriter {
  private static final Logger LOG = LoggerFactory.getLogger(InternalMessageHandler.class);
  private static final String TYPE = MessageConstants.TYPE_INTERNAL;

  private boolean testing = false;

  @Reference
  protected transient SlingRepository slingRepository;

  @Reference
  protected transient MessagingService messagingService;

  @Reference
  protected transient SiteService siteService;

  @Reference
  protected transient PresenceService presenceService;

  @Reference
  protected transient ProfileService profileService;
  
  @Reference
  protected transient LockManager lockManager;

  /**
   * Default constructor
   */
  public InternalMessageHandler() {
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

      session = slingRepository.loginAdministrative(null);

      //recipients keeps track of who have already received the message, to avoid duplicate messages
      List<String> recipients = new ArrayList<String>();
      UserManager um = AccessControlUtil.getUserManager(session);
      for (MessageRoute route : routes) {
        if (MessageTransport.INTERNAL_TRANSPORT.equals(route.getTransport())) {
          LOG.info("Started handling a message.");
          String rcpt = route.getRcpt();
          // the path were we want to save messages in.
          String messageId = originalMessage.getProperty(MessageConstants.PROP_SAKAI_ID)
          .getString();
          sendHelper(recipients, rcpt, originalMessage, session, messageId, um);
        }
      }
    } catch (RepositoryException e) {
      LOG.error(e.getMessage(), e);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
  }

  private void sendHelper(List<String> recipients, String rcpt, Node originalMessage, Session session, String messageId, UserManager um){
    try {
      Authorizable au = um.getAuthorizable(rcpt);
      if(au != null && au.isGroup() && au instanceof Group){
        Group group = (Group) au;
        //user must be in the group directly to send a message:
        for (Iterator<Authorizable> iterator = group.getDeclaredMembers(); iterator.hasNext();) {
          Authorizable auth = iterator.next();
          if(!recipients.contains(auth.getID())){
            //call back to itself: this allows for groups to be in groups and future extensions
            sendHelper(recipients, auth.getID(), originalMessage, session, messageId, um);
            recipients.add(auth.getID());
          }
        }
      }else{
        //only send a message to a user who hasn't already received one:
        if(!recipients.contains(rcpt)){

          String toPath = messagingService.getFullPathToMessage(rcpt, messageId, session);
          
          try {
            lockManager.waitForLock(toPath);
          } catch (LockTimeoutException e1) {
            throw new MessagingException("Unable to lock destination message store");
          }

          // Copy the node into the user his folder.
          JcrUtils.deepGetOrCreateNode(session, toPath.substring(0, toPath.lastIndexOf("/")));
          session.save();
          session.getWorkspace().copy(originalMessage.getPath(), toPath);
          Node n = JcrUtils.deepGetOrCreateNode(session, toPath);

          // Add some extra properties on the just created node.
          n.setProperty(MessageConstants.PROP_SAKAI_READ, false);
          n.setProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX, MessageConstants.BOX_INBOX);
          n.setProperty(MessageConstants.PROP_SAKAI_SENDSTATE,
              MessageConstants.STATE_NOTIFIED);
          

          if (session.hasPendingChanges()) {
            session.save();
          }
          recipients.add(rcpt);
        }
      }
    } catch (ValueFormatException e) {
      LOG.error(e.getMessage(), e);
    } catch (PathNotFoundException e) {
      LOG.error(e.getMessage(), e);
    } catch (RepositoryException e) {
      LOG.error(e.getMessage(), e);
    } finally {
      lockManager.clearLocks();
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
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.message.MessageProfileWriter#writeProfileInformation(javax.jcr.Session,
   *      java.lang.String, org.apache.sling.commons.json.io.JSONWriter)
   */
  public void writeProfileInformation(Session session, String recipient, JSONWriter write) {
    try {
      if (recipient.startsWith("s-")) {
        // This is a site.
        recipient = recipient.substring(2);
        Node siteNode = siteService.findSiteByName(session, recipient);
        ExtendedJSONWriter.writeNodeToWriter(write, siteNode);
      } else {
        // Look up the recipient and check if it is an authorizable.
        UserManager um = AccessControlUtil.getUserManager(session);
        Authorizable au = um.getAuthorizable(recipient);
        if (au != null) {
          write.object();
          ValueMap map = profileService.getCompactProfileMap(au, session);
          ((ExtendedJSONWriter)write).valueMapInternals(map);
          if (au instanceof User) {
            // Pass in the presence.
            PresenceUtils.makePresenceJSON(write, au.getID(), presenceService, true);
          }
          write.endObject();
        } else {
          // No idea what this recipient is.
          // Just output it.
          write.value(recipient);
        }

      }
    } catch (SiteException e) {
      LOG.error(e.getMessage(), e);
    } catch (JSONException e) {
      LOG.error(e.getMessage(), e);
    } catch (RepositoryException e) {
      LOG.error(e.getMessage(), e);
    }
  }

  /**
   * This method should only be called for unit testing purposes. It will disable the ACL
   * settings.
   */
  protected void activateTesting() {
    testing = true;
  }

}
