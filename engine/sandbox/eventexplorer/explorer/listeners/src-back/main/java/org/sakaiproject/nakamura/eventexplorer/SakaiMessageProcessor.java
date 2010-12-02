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

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.jms.JmsMessage;
import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.SuperColumn;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.Message;

/**
 *
 */
public class SakaiMessageProcessor implements Processor {

  static final Logger LOGGER = LoggerFactory.getLogger(SakaiMessageProcessor.class);

  protected static final String KEYSPACE = "SAKAI";
  protected static final String COLUMN_FAMILY_USERS = "Users";

  protected static final String ENCODING = "utf-8";
  static long timestamp;
  protected static TTransport tr = null;

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.camel.Processor#process(org.apache.camel.Exchange)
   */
  @SuppressWarnings("unchecked")
  public void process(Exchange exchange) throws Exception {
    try {
      LOGGER.info("Got a message!");
      Cassandra.Client client = setupConnection();
      Map<String, List<ColumnOrSuperColumn>> job = new HashMap<String, List<ColumnOrSuperColumn>>();
      List<ColumnOrSuperColumn> columns = new ArrayList<ColumnOrSuperColumn>();
      List<Column> column_list = new ArrayList<Column>();

      Message message = ((JmsMessage) exchange.getIn()).getJmsMessage();

      Enumeration en = message.getPropertyNames();
      System.out.println("Message");
      while (en.hasMoreElements()) {
        String prop_name = (String) en.nextElement();
        Object obj = message.getObjectProperty(prop_name);
        String obj_val = obj.toString();
        System.out.println(prop_name + "        " + obj_val);

        long timestamp = System.currentTimeMillis();
        Column col = new Column(prop_name.getBytes(ENCODING), obj_val.getBytes(ENCODING),
            timestamp);
        column_list.add(col);
      }

      long id = System.currentTimeMillis();
      SuperColumn column = new SuperColumn(("" + id).getBytes(ENCODING), column_list);
      ColumnOrSuperColumn columnOrSuperColumn = new ColumnOrSuperColumn();
      columnOrSuperColumn.setSuper_column(column);
      columns.add(columnOrSuperColumn);

      job.put(COLUMN_FAMILY_USERS, columns);
      client.batch_insert(KEYSPACE, "User1", job, ConsistencyLevel.ALL);

      closeConnection();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Open up a new connection to the Cassandra Database.
   * 
   * @return the Cassandra Client
   */
  protected static Cassandra.Client setupConnection() {
    try {
      tr = new TSocket("localhost", 9160);
      TProtocol proto = new TBinaryProtocol(tr);
      Cassandra.Client client = new Cassandra.Client(proto);
      tr.open();

      return client;
    } catch (TTransportException exception) {
      exception.printStackTrace();
    }

    return null;
  }

  /**
   * Close the connection to the Cassandra Database.
   */
  protected static void closeConnection() {
    try {
      tr.flush();
      tr.close();
    } catch (TTransportException exception) {
      exception.printStackTrace();
    }
  }

}
