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
package org.sakaiproject.nakamura.eventexplorer.cassandra;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.Cassandra.Client;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;
import org.sakaiproject.nakamura.eventexplorer.api.cassandra.CassandraService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Service(value = CassandraService.class)
@Component(immediate = true, enabled = true, metatype = true)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Is able to create Clients for Cassandra.") })
public class CassandraServiceImpl implements CassandraService {

  @Property(value = "localhost", description = "The hostname or IP address where cassandra is running.")
  static final String CASSANDRA_HOST = "cassandra.host";
  @Property(intValue = 9160, description = "The port where cassandra is listening for connections.")
  static final String CASSANDRA_PORT = "cassandra.port";

  private String cassandraHost;
  private Integer cassandraPort;
  private TSocket tr;

  private static final Logger LOGGER = LoggerFactory
      .getLogger(CassandraServiceImpl.class);

  @Activate
  protected void activate(Map<?, ?> properties) {
    cassandraHost = (String) properties.get(CASSANDRA_HOST);
    cassandraPort = (Integer) properties.get(CASSANDRA_PORT);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.eventexplorer.api.cassandra.CassandraService#getClient()
   */
  public Client getClient() {
    if (tr == null || !tr.isOpen()) {
      setupConnection();
    }
    TProtocol proto = new TBinaryProtocol(tr);
    Cassandra.Client client = new Cassandra.Client(proto);
    return client;

  }

  /**
   * Open up a new connection to the Cassandra store.
   */
  protected void setupConnection() {
    try {
      tr = new TSocket(cassandraHost, cassandraPort);
      tr.open();
    } catch (TTransportException e) {
      LOGGER.warn("Failed to open a connection to cassandra.", e);
    }
  }

  /**
   * Close the connection to the Cassandra Database.
   */
  protected void closeConnection() {
    try {
      if (tr.isOpen()) {
        tr.flush();
        tr.close();
      }
    } catch (TTransportException e) {
      LOGGER.error("Failed to close the connection to the cassandra store.", e);
    }
  }

}
