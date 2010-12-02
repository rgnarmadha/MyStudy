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
package org.sakaiproject.nakamura.eventexplorer.ui;

import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.ColumnParent;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.KeyRange;
import org.apache.cassandra.thrift.SlicePredicate;
import org.apache.cassandra.thrift.SliceRange;
import org.apache.cassandra.thrift.SuperColumn;
import org.apache.cassandra.thrift.Cassandra.Client;
import org.apache.commons.lang.time.FastDateFormat;
import org.json.JSONException;
import org.json.JSONWriter;

import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 *
 */
public class EventsWriter {

  private FastDateFormat format = FastDateFormat.getInstance(
      "EEE, dd MMM yyyy HH:mm:ss Z", new Locale("en"));

  private Client client;

  public EventsWriter(Client client) {
    this.client = client;
  }

  /**
   * Writes all the stored data of a user.
   * @param userid
   * @param writer
   */
  public void writeUser(String userid, JSONWriter writer) {
    try {
      KeyRange keyRange = new KeyRange(3);
      keyRange.setStart_key("");
      keyRange.setEnd_key("");

      SliceRange sliceRange = new SliceRange();
      sliceRange.setStart(new byte[] {});
      sliceRange.setFinish(new byte[] {});
      sliceRange.setCount(10000);

      SlicePredicate predicate = new SlicePredicate();
      predicate.setSlice_range(sliceRange);

      ColumnParent parent = new ColumnParent("Users");

      List<ColumnOrSuperColumn> cols = client.get_slice("SAKAI", userid, parent,
          predicate, ConsistencyLevel.ONE);

      for (ColumnOrSuperColumn colOrSuperCol : cols) {
        SuperColumn superColumn = colOrSuperCol.getSuper_column();
        writeSuperColumn(superColumn, writer);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param superColumn
   * @param writer
   * @throws JSONException
   */
  private void writeSuperColumn(SuperColumn superColumn, JSONWriter writer) throws JSONException {
    GregorianCalendar cal = new GregorianCalendar();
    String time = new String(superColumn.getName());
    cal.setTimeInMillis(Long.parseLong(time));
    
    writer.object();
    writer.key("start").value(format.format(cal));
    Iterator<Column> columns = superColumn.getColumnsIterator();
    String topic = "";
    String path = "";
    while (columns.hasNext()) {
      Column col = columns.next();
      String colName = new String(col.getName());
      if (colName.equals("resourceType")) {
        colName = "title";
      }
      if (colName.equals("path")) {
        path = new String(col.getValue());
      }
      if (colName.equals("event.topics")) {
        topic = new String(col.getValue());
      }
      writer.key(colName);
      writer.value(new String(col.getValue()));
    }
    writer.key("description");
    writer.value(topic + "<br />\n" + path);
    writer.endObject();
  }
}
