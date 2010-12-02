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

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.SuperColumn;
import org.apache.cassandra.thrift.Cassandra.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.Message;
import javax.jms.MessageListener;

/**
 * Our JMS listener who picks up JMS messages and stores them in Cassandra.
 */
public class SakaiMessageListener implements MessageListener {
  static final Logger LOGGER = LoggerFactory.getLogger(SakaiMessageListener.class);

  private Cassandra.Client client;

  private final String KEYSPACE = "SAKAI";
  private final String COLUMN_FAMILY_USERS = "Users";
  private final String ENCODING = "utf-8";

  /**
   * @param client
   *          A Cassandra client.
   */
  public SakaiMessageListener(Client client) {
    this.client = client;
  }

  /**
   * @param message
   *          The JMS message.
   */
  @SuppressWarnings("unchecked")
  public void onMessage(Message message) {
    try {
      Map<String, List<ColumnOrSuperColumn>> job = new HashMap<String, List<ColumnOrSuperColumn>>();
      List<ColumnOrSuperColumn> columns = new ArrayList<ColumnOrSuperColumn>();
      List<Column> column_list = new ArrayList<Column>();

      Enumeration<String> en = message.getPropertyNames();
      String user = "system";
      while (en.hasMoreElements()) {
        String prop_name = en.nextElement();
        Object obj = message.getObjectProperty(prop_name);
        String obj_val = obj.toString();

        long timestamp = System.currentTimeMillis();
        Column col = new Column(prop_name.getBytes(ENCODING), obj_val.getBytes(ENCODING),
            timestamp);
        column_list.add(col);

        if ("userid".equals(prop_name)) {
          user = obj_val;
        }
      }

      long id = System.currentTimeMillis();
      SuperColumn column = new SuperColumn(("" + id).getBytes(ENCODING), column_list);
      ColumnOrSuperColumn columnOrSuperColumn = new ColumnOrSuperColumn();
      columnOrSuperColumn.setSuper_column(column);
      columns.add(columnOrSuperColumn);
      job.put(COLUMN_FAMILY_USERS, columns);
      client.batch_insert(KEYSPACE, user, job, ConsistencyLevel.ALL);
      LOGGER.info("Inserted message for {}.", user);
    } catch (Exception e) {
      LOGGER.warn("Failed to insert the JMS message in the cassandra store.", e);
    }
  }
}