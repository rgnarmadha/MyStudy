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
package org.sakaiproject.nakamura.eventexplorer;

import org.apache.cassandra.thrift.Cassandra.Client;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.sakaiproject.nakamura.api.activemq.ConnectionFactoryService;
import org.sakaiproject.nakamura.eventexplorer.api.cassandra.CassandraService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;

@Component(enabled = true, metatype = true)
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Handles incoming JMS messages.") })
public class JmsRouteBuilder {

  // OSGi Properties
  @Property(description = "The URL endpoint to listen to.", value = "tcp://localhost:61616")
  static final String CONNECTION_URL = "jms.url";
  @Property(description = "The name of the queue", value = {
      "org/apache/sling/api/resource/Resource/ADDED, org/apache/sling/api/resource/Resource/REMOVED",
      "org/apache/sling/api/resource/Resource/CHANGED",
      "org/apache/sling/api/resource/ResourceProvider/ADDED",
      "org/apache/sling/api/resource/ResourceProvider/REMOVED",
      "org/sakaiproject/nakamura/message/pending" })
  static final String JMS_TOPIC_NAMES = "jms.topic.names";

  @Reference
  protected transient CassandraService cassandraService;

  @Reference
  protected transient ConnectionFactoryService connectionFactoryService;

  private String[] topics;
  private String connectionURL;

  private transient Connection connection;
  static final Logger LOGGER = LoggerFactory.getLogger(JmsRouteBuilder.class);

  @Activate
  protected void activate(Map<?, ?> properties) throws JMSException {
    topics = (String[]) properties.get(JMS_TOPIC_NAMES);
    connectionURL = (String) properties.get(CONNECTION_URL);

    // Get a factory
    ConnectionFactory factory = connectionFactoryService.createFactory(connectionURL);

    // Create a connection and start it.
    connection = factory.createConnection();
    connection.start();
    LOGGER.info("Started an ActiveMQ Connection to: {}", connectionURL);

    // Create a session that listens to some events.
    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

    // Construct our JMS listener.
    Client client = cassandraService.getClient();
    SakaiMessageListener processor = new SakaiMessageListener(client);

    // Loop over all the topics that are configured in the admin console and listen for
    // them.
    for (String topic : topics) {
      Destination destination = session.createTopic(topic);
      MessageConsumer messageConsumer = session.createConsumer(destination);
      messageConsumer.setMessageListener(processor);
    }
  }

  @Deactivate
  protected void deactivate(Map<?, ?> properties) throws JMSException {
    if (connection != null) {
      connection.stop();
    }
  }

}
