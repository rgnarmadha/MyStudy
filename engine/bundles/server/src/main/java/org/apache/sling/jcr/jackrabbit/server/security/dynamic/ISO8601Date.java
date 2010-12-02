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
package org.apache.sling.jcr.jackrabbit.server.security.dynamic;

import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 *
 */
public class ISO8601Date extends GregorianCalendar {

  /**
   *
   */
  private static final long serialVersionUID = 5115079662422026445L;
  private boolean date;

  /*
   * 2010-03-17 Separate date and time in UTC: 2010-03-17 06:33Z Combined date and time in
   * UTC: 2010-03-17T06:33Z
   */
  /**
   *
   */
  public ISO8601Date() {
    date = false;
  }

  public ISO8601Date(String spec) {
    int l = spec.length();
    int year = -1;
    int month = -1;
    int day = -1;
    int hour = -1;
    int min = -1;
    int sec = -1;
    TimeZone z = null;
    date = false;
    switch (l) {
    case 16:// 19970714T170000Z
    case 18:// 19970714T170000+01
      year = Integer.parseInt(spec.substring(0, 4));
      month = Integer.parseInt(spec.substring(4, 6));
      day = Integer.parseInt(spec.substring(6, 8));
      hour = Integer.parseInt(spec.substring(9, 11));
      min = Integer.parseInt(spec.substring(11, 13));
      sec = Integer.parseInt(spec.substring(13, 15));
      if ('Z' == spec.charAt(l - 1)) {
        z = TimeZone.getTimeZone("GMT");
      } else {
        z = TimeZone.getTimeZone("GMT" + spec.substring(15));
      }
      break;
    case 20: // 1997-07-14T17:00:00Z // 19970714T170000+0100
      if ('Z' == spec.charAt(l - 1)) {
        year = Integer.parseInt(spec.substring(0, 4));
        month = Integer.parseInt(spec.substring(5, 7));
        day = Integer.parseInt(spec.substring(8, 10));
        hour = Integer.parseInt(spec.substring(11, 13));
        min = Integer.parseInt(spec.substring(14, 16));
        sec = Integer.parseInt(spec.substring(17, 19));
        z = TimeZone.getTimeZone("UTC");
      } else {
        year = Integer.parseInt(spec.substring(0, 4));
        month = Integer.parseInt(spec.substring(4, 6));
        day = Integer.parseInt(spec.substring(6, 8));
        hour = Integer.parseInt(spec.substring(9, 11));
        min = Integer.parseInt(spec.substring(11, 13));
        sec = Integer.parseInt(spec.substring(13, 15));
        z = TimeZone.getTimeZone("GMT" + spec.substring(15));
      }
      break;
    case 22: // 1997-07-14T17:00:00+01
    case 25: // 1997-07-14T17:00:00+01:00
      year = Integer.parseInt(spec.substring(0, 4));
      month = Integer.parseInt(spec.substring(5, 7));
      day = Integer.parseInt(spec.substring(8, 10));
      hour = Integer.parseInt(spec.substring(11, 13));
      min = Integer.parseInt(spec.substring(14, 16));
      sec = Integer.parseInt(spec.substring(17, 19));
      z = TimeZone.getTimeZone("GMT" + spec.substring(19));
      date = false;
      break;
    case 8: // 19970714
      year = Integer.parseInt(spec.substring(0, 4));
      month = Integer.parseInt(spec.substring(4, 6));
      day = Integer.parseInt(spec.substring(6, 8));
      hour = 0;
      min = 0;
      sec = 0;
      z = TimeZone.getDefault(); // we really need to know the timezone of the user for
      // this.
      date = true;
      break;
    case 10: // 1997-07-14
      year = Integer.parseInt(spec.substring(0, 4));
      month = Integer.parseInt(spec.substring(5, 7));
      day = Integer.parseInt(spec.substring(8, 10));
      hour = 0;
      min = 0;
      sec = 0;
      z = TimeZone.getDefault(); // we really need to know the timezone of the user for
      // this.
      date = true;
      break;
    default:
      throw new IllegalArgumentException("Illeagal ISO8601 Date Time " + spec);
    }
    if (z == null) {
      throw new IllegalArgumentException(
          "Time Zone incorrectly formatted, must be one of Z or +00:00 or +0000. Time was "
              + spec);
    }
    setTimeZone(z);
    set(MILLISECOND,0);
    set(year, month-1, day, hour, min, sec);
  }

  public boolean before(long when) {
    if (date) {// end of the day must be before.
      return (getTimeInMillis() + (24000L * 3600L) < when);
    }
    return (getTimeInMillis() < when);
  }

  public boolean after(long when) {
    return (getTimeInMillis() > when);
  }

  @Override
  public String toString() {
    Formatter formatter = new Formatter();
    int year = get(YEAR);
    int month = get(MONTH)+1;
    int day = get(DAY_OF_MONTH);
    int hour = get(HOUR_OF_DAY);
    int min = get(MINUTE);
    int second = get(SECOND);
    if (date) {
      formatter.format("%04d-%02d-%02d", year, month, day);
    } else {
      long offset = getTimeZone().getOffset(getTimeInMillis()) / (60000L);
      int hoffset = (int) (offset / 60L);
      int minoffset = (int) (offset % 60L);
      if (offset == 0) {
        formatter.format("%04d-%02d-%02dT%02d:%02d:%02dZ", year, month, day, hour, min,
            second);
      } else if (offset < 0) {
        formatter.format("%04d-%02d-%02dT%02d:%02d:%02d-%02d:%02d", year, month, day,
            hour, min, second, -hoffset, -minoffset);
      } else {
        formatter.format("%04d-%02d-%02dT%02d:%02d:%02d+%02d:%02d", year, month, day,
            hour, min, second, hoffset, minoffset);
      }
    }
    return formatter.toString();
  }

  /**
   * @param b
   */
  public void setDate(boolean b) {
    date = b;
  }
}
