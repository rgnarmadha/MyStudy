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
package org.sakaiproject.nakamura.message;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sakaiproject.nakamura.api.resource.SubPathProducer;
import org.sakaiproject.nakamura.message.resource.MessageSubPathProducer;

/**
 *
 */
public class MessageSubPathProducerTest {

  @Test
  public void testGetPath() {
    SubPathProducer producer = new MessageSubPathProducer(
        "7eb256fd000d8fb33668138998251f605696b112");
    String expected = "/7e/b2/56/fd/7eb256fd000d8fb33668138998251f605696b112";
    assertEquals(expected, producer.getSubPath());
  }

}
