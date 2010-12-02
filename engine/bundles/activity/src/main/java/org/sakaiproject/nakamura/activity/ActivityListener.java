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
package org.sakaiproject.nakamura.activity;

import static org.sakaiproject.nakamura.api.activity.ActivityConstants.PARAM_ACTOR_ID;
import static org.sakaiproject.nakamura.api.activity.ActivityConstants.PARAM_SOURCE;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.activemq.ConnectionFactoryService;
import org.sakaiproject.nakamura.api.activity.ActivityConstants;
import org.sakaiproject.nakamura.api.activity.ActivityRoute;
import org.sakaiproject.nakamura.api.activity.ActivityRouterManager;
import org.sakaiproject.nakamura.api.activity.ActivityUtils;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Topic;

@Component(immediate = true)
public class ActivityListener implements MessageListener {

  // References/properties need for JMS
  @Reference
  protected ConnectionFactoryService connFactoryService;

  // References needed to actually deliver the activity.
  @Reference
  protected SlingRepository slingRepository;
  @Reference
  protected ActivityRouterManager activityRouterManager;

  public static final Logger LOG = LoggerFactory
      .getLogger(ActivityListener.class);

  private Connection connection = null;

  /**
   * Start a JMS connection.
   */
  public void activate(ComponentContext componentContext) {
    try {
      connection = connFactoryService.getDefaultConnectionFactory().createConnection();
      javax.jms.Session session = connection.createSession(false,
          javax.jms.Session.AUTO_ACKNOWLEDGE);
      Topic dest = session.createTopic(ActivityConstants.EVENT_TOPIC);
      MessageConsumer consumer = session.createConsumer(dest);
      consumer.setMessageListener(this);
      connection.start();
    } catch (JMSException e) {
      LOG.error(e.getMessage(), e);
      if (connection != null) {
        try {
          connection.close();
        } catch (JMSException e1) {
        }
      }
    }
  }

  /**
   * Close the JMS connection
   */
  protected void deactivate(ComponentContext ctx) {
    if (connection != null) {
      try {
        connection.close();
      } catch (JMSException e) {
        LOG.error("Cannot close the activity JMS connection.", e);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
   */
  public void onMessage(Message message) {
    try {
      final String activityItemPath = message
          .getStringProperty(ActivityConstants.EVENT_PROP_PATH);
      LOG.info("Processing activity: {}", activityItemPath);
      Session session = slingRepository.loginAdministrative(null); // usage checked and Ok KERN-577
      // usage is NOT ok. whoever made the comment above, sessions must be logged out or they leak.
      try {
        Node activity = (Node) session.getItem(activityItemPath);
        if (!activity.hasProperty(PARAM_ACTOR_ID)) {
          // we must know the actor
          throw new IllegalStateException(
              "Could not determine actor of activity: " + activity);
        }
  
        // Get all the routes for this activity.
        List<ActivityRoute> routes = activityRouterManager
            .getActivityRoutes(activity);
  
        // Copy the activity items to each endpoint.
        for (ActivityRoute route : routes) {
          deliverActivityToFeed(session, activity, route.getDestination());
        }
      } finally {
        try { 
          session.logout(); 
        } catch ( Exception e) {
          LOG.warn("Failed to logout of administrative session {} ",e.getMessage());
        }
      }

    } catch (JMSException e) {
      LOG.error("Got a JMS exception in the activity listener.", e);
    } catch (RepositoryException e) {
      LOG.error("Got a repository exception in the activity listener.", e);
    }
  }

  /**
   * Delivers an activity to a feed.
   *
   * @param session
   *          The session that should be used to do the delivering.
   * @param activity
   *          The node that represents the activity.
   * @param activityFeedPath
   *          The path that holds the feed where the activity should be delivered.
   * @throws RepositoryException
   */
  protected void deliverActivityToFeed(Session session, Node activity,
      String activityFeedPath) throws RepositoryException {
    // ensure the activityFeed node with the proper type
    Node activityFeedNode = JcrUtils.deepGetOrCreateNode(session,
        activityFeedPath);
    if (activityFeedNode.isNew()) {
      activityFeedNode.setProperty(
          JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
          ActivityConstants.ACTIVITY_FEED_RESOURCE_TYPE);
      session.save();
    }
    // activityFeed exists, let's continue with delivery
    // activityFeed is a BigStore, get the hashed (real) path
    final String deliveryPath = ActivityUtils.getPathFromId(activity.getName(),
        activityFeedPath);
    // ensure the parent path exists before we copy source activity
    final String parentPath = deliveryPath.substring(0, deliveryPath
        .lastIndexOf("/"));
    final Node parentNode = JcrUtils.deepGetOrCreateNode(session, parentPath);
    if (parentNode.isNew()) {
      session.save();
    }
    // now copy the activity from the store to the feed
    copyActivityItem(session, activity.getPath(), deliveryPath);
  }

  /**
   * Copies an activity over.
   *
   * @param session
   *          The session that should be used to do the copying.
   * @param source
   *          The path of the original activity.
   * @param destination
   *          The path where the activity should be copied to.
   * @throws RepositoryException
   */
  protected void copyActivityItem(Session session, String source,
      String destination) throws RepositoryException {
    LOG.debug("copyActivityItem(Session {}, String {}, String {})",
        new Object[] { session, source, destination });
    // now copy the activity from the source to the destination
    session.getWorkspace().copy(source, destination);
    // next let's create a source property to refer back to the original item
    // in the ActivityStore
    Node feedItem = (Node) session.getItem(destination);
    feedItem.setProperty(PARAM_SOURCE, source);
    if (session.hasPendingChanges()) {
      session.save();
    }
  }

}
