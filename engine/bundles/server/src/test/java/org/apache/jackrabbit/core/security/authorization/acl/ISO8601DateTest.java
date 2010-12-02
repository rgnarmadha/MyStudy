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
package org.apache.jackrabbit.core.security.authorization.acl;

import org.apache.sling.jcr.jackrabbit.server.security.dynamic.ISO8601Date;
import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;


/**
 *
 */
public class ISO8601DateTest {

  private static final long PERIOD = 24L*24L*60L*60L*1000L+2000L;

  @Test
  public void testParseUTC() {
    long t = 0;
    // london may not be correct
    TimeZone london =  new SimpleTimeZone(0,
        "Europe/London",
        Calendar.MARCH, -1, Calendar.SUNDAY,
        3600000, SimpleTimeZone.UTC_TIME,
        Calendar.OCTOBER, -1, Calendar.SUNDAY,
        3600000, SimpleTimeZone.UTC_TIME,
        3600000);
    TimeZone paris =  new SimpleTimeZone(3600000,
        "Europe/Paris",
        Calendar.MARCH, -1, Calendar.SUNDAY,
        3600000, SimpleTimeZone.UTC_TIME,
        Calendar.OCTOBER, -1, Calendar.SUNDAY,
        3600000, SimpleTimeZone.UTC_TIME,
        3600000);
    TimeZone la = new SimpleTimeZone(-28800000,
        "America/Los_Angeles",
        Calendar.APRIL, 1, -Calendar.SUNDAY,
        7200000,
        Calendar.OCTOBER, -1, Calendar.SUNDAY,
        7200000,
        3600000);
    for (int i = 0; i < 1000; i++ ) {
      t += PERIOD;
      ISO8601Date g = new ISO8601Date();
      g.setTimeZone(london);
      g.setTimeInMillis(t);
      String tt = g.toString();
      ISO8601Date g2 = new ISO8601Date(tt);
      Assert.assertEquals(g.getTimeInMillis(), g2.getTimeInMillis());
      Assert.assertEquals(g.toString(), g2.toString());
    }
    for (int i = 0; i < 1000; i++ ) {
      t += PERIOD;
      ISO8601Date g = new ISO8601Date();
      g.setTimeZone(paris);
      g.setTimeInMillis(t);
      String tt = g.toString();
      ISO8601Date g2 = new ISO8601Date(tt);
      Assert.assertEquals(g.getTimeInMillis(), g2.getTimeInMillis());
      Assert.assertEquals(g.toString(), g2.toString());
    }
    for (int i = 0; i < 1000; i++ ) {
      t += PERIOD;
      ISO8601Date g = new ISO8601Date();
      g.setTimeZone(la);
      g.setTimeInMillis(t);
      String tt = g.toString();
      ISO8601Date g2 = new ISO8601Date(tt);
      Assert.assertEquals(g.getTimeInMillis(), g2.getTimeInMillis());
      Assert.assertEquals(g.toString(), g2.toString());
    }
  }
  
  @Test
  public void testDate() {
    ISO8601Date g = new ISO8601Date();
    g.set(2010, 11, 24);
    g.setDate(true);
    Assert.assertEquals("2010-12-24", g.toString());
  }
}
