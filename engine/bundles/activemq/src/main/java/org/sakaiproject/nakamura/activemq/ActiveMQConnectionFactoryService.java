/*
 * Licensed to the Sakai Foundation (SF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The SF licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.sakaiproject.nakamura.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.activemq.ConnectionFactoryService;

import java.net.URI;
import java.util.Dictionary;

import javax.jms.ConnectionFactory;

@Component(immediate=true, metatype=true, label="%amqf.name", description="%amqf.description")
@Service(value=ConnectionFactoryService.class)
public class ActiveMQConnectionFactoryService implements ConnectionFactoryService {

  private ActiveMQConnectionFactory defaultConnectionFactory;

  private PooledConnectionFactory pooledConnectionFactory;

  @Property(value = "vm://localhost:61616")
  public static final String BROKER_URL = "jms.brokerUrl";

  public void activateForTest(ComponentContext componentContext) {
    activate(componentContext);
  }

  @SuppressWarnings("unchecked")
  protected void activate(ComponentContext componentContext) {
    Dictionary<String, Object> props = componentContext.getProperties();
    String brokerURL = (String) props.get(BROKER_URL);
    defaultConnectionFactory = new ActiveMQConnectionFactory(brokerURL);
    pooledConnectionFactory = new PooledConnectionFactory(brokerURL);
    pooledConnectionFactory.start();
  }

  protected void deactivate(ComponentContext ctx) {
    pooledConnectionFactory.stop();
  }


  public ConnectionFactory createFactory(String brokerURL) {
    return new ActiveMQConnectionFactory(brokerURL);
  }

  public ConnectionFactory createFactory(URI brokerURL) {
    return new ActiveMQConnectionFactory(brokerURL);
  }


  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.activemq.ConnectionFactoryService#getDefaultFactory()
   */
  public ConnectionFactory getDefaultPooledConnectionFactory() {
    return pooledConnectionFactory;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.activemq.ConnectionFactoryService#getDefaultConnectionFactory()
   */
  public ConnectionFactory getDefaultConnectionFactory() {
    return defaultConnectionFactory;
  }
}
