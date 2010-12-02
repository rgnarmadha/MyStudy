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

import static org.junit.Assert.*;

import java.util.regex.Pattern;

import org.junit.Test;

public class DateUtilsTest {

  @Test
  public void testRfc3339() throws Exception {
    Pattern dateFormat = Pattern
        .compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}[-+]\\d{4}$");
    assertTrue(dateFormat.matcher(DateUtils.rfc3339()).matches());
  }

  @Test
  public void testRfc2822() throws Exception {
    Pattern dateFormat = Pattern
        .compile("^\\p{Alpha}{3}, \\d{2} \\p{Alpha}{3} \\d{4} \\d{2}:\\d{2}:\\d{2} [-+]\\d{4}$");
    assertTrue(dateFormat.matcher(DateUtils.rfc2822()).matches());
  }

  @Test
  public void testIso8601() throws Exception {
    Pattern dateFormat = Pattern
        .compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[-+]\\d{2}:\\d{2}$");
    assertTrue(dateFormat.matcher(DateUtils.iso8601()).matches());
  }
}
