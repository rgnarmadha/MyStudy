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
package org.sakaiproject.nakamura.cluster;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.activemq.ConnectionFactoryService;
import org.sakaiproject.nakamura.api.cluster.ClusterTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.Topic;

/**
 *
 */
@Component(immediate = true)
public class ClusterUserMessageListener implements MessageListener {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ClusterUserMessageListener.class);

  @Reference
  protected ConnectionFactoryService connFactoryService;
  @Reference
  protected ClusterTrackingService clusterTrackingService;

  private Connection connection;
  private ClusterTrackingServiceImpl clusterTrackingServiceImpl;

  protected void activate(ComponentContext componentContext) {

    String serverId = clusterTrackingService.getCurrentServerId();

    clusterTrackingServiceImpl = (ClusterTrackingServiceImpl) clusterTrackingService;

    try {
      connection = connFactoryService.getDefaultConnectionFactory().createConnection();
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      Topic dest = session.createTopic(ClusterTrackingService.EVENT_PING_CLUSTER_USER
          + "/" + serverId);
      MessageConsumer consumer = session.createConsumer(dest);
      consumer.setMessageListener(this);
      connection.start();
    } catch (JMSException e) {
      LOGGER.error(e.getMessage(), e);
      if (connection != null) {
        try {
          connection.close();
        } catch (JMSException e1) {
        }
      }
    }

  }

  protected void deactivate(ComponentContext ctx) {
    if (connection != null) {
      try {
        connection.close();
      } catch (JMSException e) {
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
      String fromServer = message
          .getStringProperty(ClusterTrackingService.EVENT_FROM_SERVER);
      String toServer = message.getStringProperty(ClusterTrackingService.EVENT_TO_SERVER);
      String trackingCookie = message
          .getStringProperty(ClusterTrackingService.EVENT_TRACKING_COOKIE);
      String remoteUser = message.getStringProperty(ClusterTrackingService.EVENT_USER);
      LOGGER.info(
          "Started handling cluster user jms message. from:{} to:{} cookie:{} user:{}",
          new Object[] { fromServer, toServer, trackingCookie, remoteUser });
      clusterTrackingServiceImpl.pingTracking(trackingCookie, remoteUser, false);
    } catch (PingRemoteTrackingFailedException e) {
      LOGGER.error(e.getMessage());
    } catch (JMSException e) {
      LOGGER.error(e.getMessage(), e);
    }
  }


}
