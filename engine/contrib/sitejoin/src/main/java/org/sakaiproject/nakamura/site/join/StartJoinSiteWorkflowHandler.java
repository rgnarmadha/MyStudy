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
 * specific language governing permissions and limitations under
 * the License.
 */
package org.sakaiproject.nakamura.site.join;

import static org.sakaiproject.nakamura.api.message.MessageConstants.EVENT_LOCATION;
import static org.sakaiproject.nakamura.api.message.MessageConstants.PENDINGMESSAGE_EVENT;
import static org.sakaiproject.nakamura.api.message.MessageConstants.PROP_SAKAI_SENDSTATE;
import static org.sakaiproject.nakamura.api.message.MessageConstants.STATE_NOTIFIED;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.sakaiproject.nakamura.api.message.MessagingService;
import org.sakaiproject.nakamura.api.site.SiteService.SiteEvent;
import org.sakaiproject.nakamura.api.site.join.JoinRequestConstants;
import org.sakaiproject.nakamura.api.site.join.JoinRequestUtil;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 *
 */
@Component
@Service(value = EventHandler.class)
@Property(name = EventConstants.EVENT_TOPIC, value = "org/sakaiproject/nakamura/api/site/event/startJoinWorkflow")
public class StartJoinSiteWorkflowHandler implements EventHandler {
  private static final Logger logger = LoggerFactory
      .getLogger(StartJoinSiteWorkflowHandler.class);

  @Reference
  private SlingRepository repository;

  @Reference
  private MessagingService messagingService;
  
  @Reference
  protected transient EventAdmin eventAdmin;

  public void handleEvent(Event event) {
    String sitePath = (String) event.getProperty(SiteEvent.SITE);
    String userId = (String) event.getProperty(SiteEvent.USER);
    String group = (String) event.getProperty(SiteEvent.GROUP);
    String owner = (String) event.getProperty(SiteEvent.OWNER);

    try {
      Session session = repository.loginAdministrative(null);

      // #1 add user to the join requests of a site
      createPendingRequest(userId, group, sitePath, session);

      // #2 send message to site owner
      Node n = sendMessage(userId, owner, session, sitePath);
      n.setProperty(PROP_SAKAI_SENDSTATE, STATE_NOTIFIED);
      Dictionary<String, Object> messageDict = new Hashtable<String, Object>();
      // WARNING
      // We can't pass in the node, because the session might expire before the event gets handled
      // This does mean that the listener will have to get the node each time, and probably create a new session for each message
      // This might be heavy on performance.
      messageDict.put(EVENT_LOCATION, n.getPath());
      messageDict.put("user", userId);
      Event pendingMessageEvent = new Event(PENDINGMESSAGE_EVENT, messageDict);
      // KERN-790: Initiate a synchronous event.
      eventAdmin.sendEvent(pendingMessageEvent);
    } catch (RepositoryException e) {
      logger.error(e.getMessage(), e);
    }
  }

  private void createPendingRequest(String userId, String group, String sitePath,
      Session session) throws RepositoryException {
    // create a node under /sites/mysite/joinrequests/u/us/user
    String requestPath = JoinRequestUtil.getPath(sitePath, userId, session);
    Node requestNode = JcrUtils.deepGetOrCreateNode(session, requestPath);
    requestNode.setProperty(JoinRequestConstants.PROP_RESOURCE_TYPE, "sakai/joinrequest");
    requestNode.setProperty(JoinRequestConstants.PROP_REQUEST_STATE, "pending");
    requestNode.setProperty(JoinRequestConstants.PROP_TARGET_GROUP, group);
    session.save();
  }

  private Node sendMessage(String sender, String recipient, Session session, String sitePath) {
    String subject = "Site join request: " + sitePath;
    String body = "Please approve my request to be a member of this site: " + sitePath;

    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put("sling:resourceType", "sakai/message");
    props.put("sakai:type", "internal");
    props.put("sakai:sendstate", "pending");
    props.put("sakai:messagebox", "outbox");
    props.put("sakai:to", "internal:" + recipient);
    props.put("sakai:read", Boolean.TRUE);
    props.put("sakai:from", sender);
    props.put("sakai:subject", subject);
    props.put("sakai:body", body);
    props.put("_charset_", "utf-8");
    props.put("sakai:category", "invitation");
    props.put("sakai:subcategory", "joinrequest");
    props.put("sakai:sitepath", sitePath);

    return messagingService.create(session, props);
  }
}
