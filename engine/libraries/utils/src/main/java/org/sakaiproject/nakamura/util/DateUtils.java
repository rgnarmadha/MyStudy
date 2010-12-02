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
package org.sakaiproject.nakamura.util;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.FastDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Utility methods for working with dates.
 */
public class DateUtils {
  private final static FastDateFormat rfc3339;
  private final static FastDateFormat rfc2822;
  private final static FastDateFormat rfc2445;
  private final static FastDateFormat iso8601jcr;

  static {
    rfc3339 = FastDateFormat.getInstance("yyyy-MM-dd hh:mm:ssZ");
    rfc2822 = FastDateFormat.getInstance("EEE, dd MMM yyyy HH:mm:ss Z", new Locale("en"));
    //20100401T140000Z
    rfc2445 = FastDateFormat.getInstance("yyyyMMdd'T'HHmmssZ");
    iso8601jcr = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
  }

  /**
   * Returns an <a href="http://www.ietf.org/rfc/rfc3339.txt">RFC 3339</a> compliant time
   * stamp.
   * 
   * @return yyyy-MM-dd hh:mm:ssZ
   * @see java.text.SimpleDateFormat
   */
  public static String rfc3339() {
    Date d = new Date();
    return rfc3399(d);
  }

  /**
   * Returns an <a href="http://www.ietf.org/rfc/rfc3339.txt">RFC 3339</a> compliant time
   * stamp.
   * 
   *@param
   * @return yyyy-MM-dd hh:mm:ssZ
   * @see java.text.SimpleDateFormat
   */
  public static String rfc3399(Date d) {
    String s = rfc3339.format(d);
    return s;
  }

  /**
   * Returns an <a href="http://www.ietf.org/rfc/rfc2822.txt">RFC 2822</a> compliant time
   * stamp for messages.
   * 
   * @return EEE, dd MMM yyyy HH:mm:ss Z
   * @see java.text.SimpleDateFormat
   */
  public static String rfc2822() {
    Date d = new Date();
    return rfc2822(d);
  }

  /**
   * Returns an <a href="http://www.ietf.org/rfc/rfc2822.txt">RFC 2822</a> compliant time
   * stamp for messages.
   * 
   * @return EEE, dd MMM yyyy HH:mm:ss Z
   * @see java.text.SimpleDateFormat
   */
  public static String rfc2822(Date d) {
    String s = rfc2822.format(d);
    return s;
  }

  /**
   * Returns an <a href="http://en.wikipedia.org/wiki/ISO_8601">ISO 8601> compliant
   * datetime stamp.
   * 
   * @return yyyy-MM-dd'T'HH:mm:ssZZ
   */
  public static String iso8601() {
    return iso8601(new Date());
  }

  /**
   * Returns an <a href="http://en.wikipedia.org/wiki/ISO_8601">ISO 8601> compliant
   * datetime stamp.
   * 
   * @param d
   *          The date to format.
   * @return yyyy-MM-dd'T'HH:mm:ssZZ
   */
  public static String iso8601(Date d) {
    return DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(d);
  }

  /**
   * Returns an <a href="http://en.wikipedia.org/wiki/ISO_8601">ISO 8601> compliant
   * datetime stamp.
   * 
   * @param c
   *          The date to format.
   * @return yyyy-MM-dd'T'HH:mm:ssZZ
   */
  public static String iso8601(Calendar c) {
    return DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(c);
  }
  
  /**
   * Jackrabbit uses a subset of 8601 (8601:2000) for their datetimes. This method returns
   * the datetime in this subset format.
   * 
   * @return yyyy-MM-dd'T'HH:mm:ss.SSSZZ
   */
  public static String iso8601jcr() {
    return iso8601(new Date());
  }

  /**
   * Jackrabbit uses a subset of 8601 (8601:2000) for their datetimes. This method returns
   * the specified {@link Date date} in this subset format.
   * 
   * @param d
   *          The date to format.
   * @return yyyy-MM-dd'T'HH:mm:ss.SSSZZ
   */
  public static String iso8601jcr(Date d) {
    return iso8601jcr.format(d);
  }

  /**
   * Jackrabbit uses a subset of 8601 (8601:2000) for their datetimes. This method returns
   * the specified {@link Calendar calendar} in this subset format.
   * 
   * @param c
   *          The date to format.
   * @return yyyy-MM-dd'T'HH:mm:ss.SSSZZ
   */
  public static String iso8601jcr(Calendar c) {
    return iso8601jcr.format(c);
  }
  
  /**
   * Returns an <a href="http://www.ietf.org/rfc/rfc2445.txt">RFC 2445</a> compliant time
   * stamp.
   * 
   * @return yyyy-MM-dd hh:mm:ssZ
   * @see java.text.SimpleDateFormat
   */
  public static String rfc2445() {
    Date d = new Date();
    return rfc2445(d);
  }

  /**
   * Returns an <a href="http://www.ietf.org/rfc/rfc2445.txt">RFC 2445</a> compliant time
   * stamp.
   * 
   *@param
   * @return yyyy-MM-dd hh:mm:ssZ
   * @see java.text.SimpleDateFormat
   */
  public static String rfc2445(Date d) {
    String s = rfc2445.format(d);
    return s;
  }

}
