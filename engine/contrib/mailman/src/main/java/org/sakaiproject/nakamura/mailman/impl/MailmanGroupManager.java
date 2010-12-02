/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.nakamura.mailman.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.api.user.AuthorizableEvent;
import org.sakaiproject.nakamura.api.user.AuthorizableEvent.Operation;
import org.sakaiproject.nakamura.mailman.MailmanManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

@Component(immediate = true, metatype = true, label = "%mail.manager.impl.label", description = "%mail.manager.impl.desc")
@Service(value = EventHandler.class)
public class MailmanGroupManager implements EventHandler, ManagedService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MailmanGroupManager.class);

  @SuppressWarnings("unused")
  @Property(value = "The Sakai Foundation")
  private static final String SERVICE_VENDOR = "service.vendor";

  @SuppressWarnings("unused")
  @Property(value = "Handles management of mailman integration")
  private static final String SERVICE_DESCRIPTION = "service.description";

  @SuppressWarnings("unused")
  @Property(value = { "org/apache/sling/jackrabbit/usermanager/event/create",
      "org/apache/sling/jackrabbit/usermanager/event/delete",
      "org/apache/sling/jackrabbit/usermanager/event/join",
      "org/apache/sling/jackrabbit/usermanager/event/part" })
  private static final String EVENT_TOPICS = "event.topics";

  @Property(value = "password")
  private static final String LIST_MANAGEMENT_PASSWORD = "mailman.listmanagement.password";

  @Reference
  private MailmanManager mailmanManager;

  @Reference
  private SlingRepository slingRepository;

  private String listManagementPassword;

  public MailmanGroupManager() {
  }

  public MailmanGroupManager(MailmanManager mailmanManager, SlingRepository slingRepository) {
    this.mailmanManager = mailmanManager;
    this.slingRepository = slingRepository;
  }

  public void handleEvent(Event event) {
    LOGGER.info("Got event on topic: " + event.getTopic());
    Operation operation = (Operation) event.getProperty(AuthorizableEvent.OPERATION);
    String principalName = event.getProperty(AuthorizableEvent.PRINCIPAL_NAME).toString();
    switch (operation) {
      case create:
        LOGGER.info("Got authorizable creation: " + principalName);
        if (principalName.startsWith("g-")) {
          LOGGER.info("Got group creation. Creating mailman list");
          try {
            mailmanManager.createList(principalName, principalName + "@example.com", listManagementPassword);
          } catch (Exception e) {
            LOGGER.error("Unable to create mailman list for group", e);
          }
        }
        break;
      case delete:
        LOGGER.info("Got authorizable deletion: " + principalName);
        if (principalName.startsWith("g-")) {
          LOGGER.info("Got group deletion. Deleting mailman list");
          try {
            mailmanManager.deleteList(principalName, listManagementPassword);
          } catch (Exception e) {
            LOGGER.error("Unable to delete mailman list for group", e);
          }
        }
        break;
      case join:
      {
        LOGGER.info("Got group join event");
        Group group = (Group)event.getProperty(AuthorizableEvent.GROUP);
        User user = (User)event.getProperty(AuthorizableEvent.USER);
        try {
          String emailAddress = getEmailForUser(user);
          if (emailAddress != null) {
            LOGGER.info("Adding " + user.getID() + " to mailman group " + group.getID());
            mailmanManager.addMember(group.getID(), listManagementPassword, emailAddress);
          } else {
            LOGGER.warn("No email address recorded for user: " + user.getID() + ". Not adding to mailman list");
          }
        } catch (RepositoryException e) {
          LOGGER.error("Repository exception adding user to mailman group", e);
        } catch (MailmanException e) {
          LOGGER.error("Mailman exception adding user to mailman group", e);
        }
      }
        break;
      case part:
      {
        LOGGER.info("Got group join event");
        Group group = (Group)event.getProperty(AuthorizableEvent.GROUP);
        User user = (User)event.getProperty(AuthorizableEvent.USER);
        try {
          String emailAddress = getEmailForUser(user);
          if (emailAddress != null) {
            LOGGER.info("Adding " + user.getID() + " to mailman group " + group.getID());
            mailmanManager.removeMember(group.getID(), listManagementPassword, emailAddress);
          } else {
            LOGGER.warn("No email address recorded for user: " + user.getID() + ". Not removing from mailman list");
          }
        } catch (RepositoryException e) {
          LOGGER.error("Repository exception removing user from mailman group", e);
        } catch (MailmanException e) {
          LOGGER.error("Mailman exception removing user from mailman group", e);
        }
      }
        break;
    }
  }

  private String getEmailForUser(User user) throws RepositoryException {
    Session session = slingRepository.loginAdministrative(null);
    Node profileNode = (Node)session.getItem(PersonalUtils.getProfilePath(user));
    String emailAddress = PersonalUtils.getPrimaryEmailAddress(profileNode);
    session.logout();
    return emailAddress;
  }

  @SuppressWarnings("unchecked")
  public void updated(Dictionary config) throws ConfigurationException {
    LOGGER.info("Got config update");
    listManagementPassword = (String) config.get(LIST_MANAGEMENT_PASSWORD);
  }

  protected void activate(ComponentContext componentContext) {
    LOGGER.info("Got component initialization");
    listManagementPassword = (String)componentContext.getProperties().get(LIST_MANAGEMENT_PASSWORD);
  }

}
