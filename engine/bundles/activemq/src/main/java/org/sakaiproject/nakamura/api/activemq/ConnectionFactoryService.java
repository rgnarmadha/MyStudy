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
package org.sakaiproject.nakamura.api.activemq;

import java.net.URI;

import javax.jms.ConnectionFactory;

/**
 *
 */
public interface ConnectionFactoryService {

  /**
   * Use this method to create a connection to another JMS network, use the use
   * getDefaultPooledConnectionFactory() to get a connection factory for internal use.
   * 
   * It is the responsibility of the caller to start, stop and close the connection factory.
   * 
   * @param brokerURL
   * @return
   */
  ConnectionFactory createFactory(String brokerURL);

  /**
   * @param brokerURL
   * @return
   */
  ConnectionFactory createFactory(URI brokerURL);

  /**
   * The default pooled connection factory uses pooled connections and can be regot, and
   * reused. It should not be started, stoped or closed since that is the responsibility
   * of the service.
   * 
   * @return a default pooled factory connecting to the JMS infrastructure.
   */
  ConnectionFactory getDefaultPooledConnectionFactory();
  
  /**
   * @return a standard connection factory.
   */
  ConnectionFactory getDefaultConnectionFactory();

}
