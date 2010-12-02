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

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.network.NetworkConnector;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

@Component(immediate = true, label = "%activemqbroker.name", description = "%activemqbroker.description", metatype = true)
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "AMQ Broker Component.") })
public class ActiveMQBrokerComponent {

  @Property
  protected static final String ACTIVEMQ_FEDERATED_BROKER_URL = "federated.broker.url";

  @Property(value = "tcp://localhost:61616")
  protected static final String ACTIVEMQ_BROKER_URL = "broker.url";

  @Property(boolValue = true)
  protected static final String ACTIVEMQ_BROKER_ENABLED = "broker.enabled";

  private static final Logger LOG = LoggerFactory
      .getLogger(ActiveMQBrokerComponent.class);

  private BrokerService broker;

  @Activate
  protected void activate(ComponentContext componentContext) throws Exception {
    try {
      Dictionary<?, ?> properties = componentContext.getProperties();
      if ((Boolean) properties.get(ACTIVEMQ_BROKER_ENABLED)) {
        LOG.info("Starting Activation of AMQ Broker ");
        BundleContext bundleContext = componentContext.getBundleContext();
        String brokerUrl = (String) properties.get(ACTIVEMQ_BROKER_URL);

        broker = new BrokerService();

        // generate a full path
        String slingHome = bundleContext.getProperty("sling.home");
        String dataPath = slingHome + "/activemq-data";
        LOG.info("Setting Data Path to  [{}] [{}] ", new Object[] { slingHome, dataPath });
        broker.setDataDirectory(dataPath);

        String federatedBrokerUrl = (String) properties
            .get(ACTIVEMQ_FEDERATED_BROKER_URL);

        if (federatedBrokerUrl != null && federatedBrokerUrl.length() > 0) {
          LOG.info("Federating ActiveMQ  [" + federatedBrokerUrl + "]");
          NetworkConnector connector = broker.addNetworkConnector(federatedBrokerUrl);
          connector.setDuplex(true);
        }

        // configure the broker
        LOG.info("Adding ActiveMQ connector [" + brokerUrl + "]");
        broker.addConnector(brokerUrl);

        broker.start();
      } else {
        LOG.info("AMQ Embeded Broker disabled, any connections must be made to a external broker. ");
      }
    } catch (Exception e) {
      LOG.debug(e.getMessage(), e);
      throw e;
    }
  }

  @Deactivate
  protected void deactivate(ComponentContext componentContext) {
    try {
      if (broker != null && broker.isStarted()) {
        broker.stop();
      }
      broker = null;
    } catch (Exception ex) {
      LOG.info("Error Shutting down AMQ Broker {} ", ex.getMessage());
    }
  }
}
