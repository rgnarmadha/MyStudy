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
package org.sakaiproject.nakamura.calendar;

import static org.junit.Assert.assertEquals;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Uid;

import org.junit.Test;

import java.text.ParseException;

/**
 *
 */
public class CalendarSubPathProducerTest {

  @Test
  public void test() throws ParseException {
    // 1271245657000L = 2010-04-14T11:47:37+00:00  NB this is GMT,UTC,Z
    DateTime dateTime = new DateTime(1271245657000L);
    DtStart start = new DtStart(dateTime);
    VEvent event = new VEvent();
    event.getProperties().add(start);
    CalendarSubPathProducer producer = new CalendarSubPathProducer(event);
    String actual = producer.getSubPath();
    assertEquals("/2010/04/14/11/47-37", actual); // in GMT/UTC/Z
    assertEquals("vevent", producer.getType());

    // Test with a UID  
    Uid uid = new Uid("AAAAAOZGb44G2IVChIfhT3gnN6ZkoCAA");
    event.getProperties().add(uid);
    producer = new CalendarSubPathProducer(event);
    actual = producer.getSubPath();
    assertEquals("/2010/04/14/11/47-37-AAAAAOZGb44G2IVChIfhT3gnN6ZkoCAA", actual);
    assertEquals("vevent", producer.getType());
  }
}
