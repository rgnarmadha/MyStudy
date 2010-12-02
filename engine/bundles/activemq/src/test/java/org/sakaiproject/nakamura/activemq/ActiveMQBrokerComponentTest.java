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
package org.sakaiproject.nakamura.activemq;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;

/**
 *
 */
public class ActiveMQBrokerComponentTest {

  @Mock
  private BundleContext bundleContext;
  @Mock
  private ComponentContext componentContext;

  public ActiveMQBrokerComponentTest() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testActivate() throws Exception {
    String port = String.valueOf(getFreePort());
    Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
    dictionary.put(ActiveMQBrokerComponent.ACTIVEMQ_BROKER_URL,"tcp://localhost:"+port+"/");
    dictionary.put(ActiveMQBrokerComponent.ACTIVEMQ_BROKER_ENABLED,true);
    dictionary.put(ActiveMQConnectionFactoryService.BROKER_URL, "vm://localhost:" + port);
    Mockito.when(componentContext.getProperties()).thenReturn(dictionary);
    Mockito.when(componentContext.getBundleContext()).thenReturn(bundleContext);
    Mockito.when(bundleContext.getProperty("sling.home")).thenReturn("target");

    // Activate the bundle.
    ActiveMQBrokerComponent amq = new ActiveMQBrokerComponent();
    amq.activate(componentContext);

    // Start the service.
    ActiveMQConnectionFactoryService service = new ActiveMQConnectionFactoryService();
    service.activate(componentContext);
    ConnectionFactory cf = service.getDefaultConnectionFactory();
    Connection c = cf.createConnection();
    Session clientSession = c.createSession(false, 1);
    String topic = "testTopic";
    Topic emailTopic = clientSession.createTopic(topic);
    MessageProducer client = clientSession.createProducer(emailTopic);
    Message msg = clientSession.createMessage();
    // may need to set a delivery mode eg persistent for certain types of messages.
    // this should be specified in the OSGi event.
    msg.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
    msg.setJMSType(topic);
    msg.setStringProperty("testing", "testingvalue");
    client.send(msg);
    clientSession.close();
    c.close();
    service.deactivate(componentContext);
    amq.deactivate(componentContext);
  }

  public int getFreePort() {
    int port = 0;
    int attempts = 0;
    while (attempts < 100) {
      try {
        ServerSocket socket = new ServerSocket(0);
        port = socket.getLocalPort();
        socket.close();
        break;
      } catch (IOException e) {
        attempts++;
      }
    }
    return port;
  }
}
