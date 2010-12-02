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

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.osgi.CamelContextFactory;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

import javax.jms.ConnectionFactory;

@Component(immediate = true, enabled = true, metatype = true)
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Activates the JMS listener.") })
public class SakaiActivator {

  @Property(description = "The URL endpoint to listen to.", value = "tcp://localhost:61616")
  static final String JMS_BROKER_URL = "jms.broker.url";

  private String brokerURL = "tcp://localhost:61616";

  static final Logger LOGGER = LoggerFactory.getLogger(SakaiActivator.class);

  @SuppressWarnings("unchecked")
  @Activate
  public void activate(ComponentContext componentContext) throws Exception {
    Dictionary properties = componentContext.getProperties();
    brokerURL = (String) properties.get(JMS_BROKER_URL);

    // Define Camel.
    // CamelContextFactory factory = new CamelContextFactory();
    // factory.setBundleContext(componentContext.getBundleContext());
    // CamelContext context = factory.createContext();
    CamelContext context = new DefaultCamelContext();

    // Create the ActiveMQ factor with the defined endpoint.
    // Also create a Camel Component for it
    ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerURL);
    JmsComponent jms = JmsComponent.jmsComponentAutoAcknowledge(connectionFactory);

    // Add that jms component to the camel context so we can use it for our routing.
    context.addComponent("sakai-jms", jms);

    // Add the route filtering.
    JmsRouteBuilder jmsRoutesBuilder = new JmsRouteBuilder();
    context.addRoutes(jmsRoutesBuilder);

    // Start listening.
    context.start();

    LOGGER.info("Started listening for JMS messages.");
  }
}