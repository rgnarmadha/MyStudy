/*
 * Licensed to the Sakai Foundation (SF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The SF licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.sakaiproject.nakamura.api.persondirectory;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class PersonProviderExceptionTest {
  @Test
  public void testDefaultConstructor() {
    PersonProviderException e = new PersonProviderException();
    assertNull(e.getMessage());
    assertNull(e.getCause());
  }

  @Test
  public void testConstructorMessage() {
    PersonProviderException e = new PersonProviderException("Exception happened");
    assertEquals("Exception happened", e.getMessage());
    assertNull(e.getCause());
  }

  @Test
  public void testConstructorThrowable() {
    Exception ex = new Exception();
    PersonProviderException e = new PersonProviderException(ex);
    assertEquals(ex.getClass().getName(), e.getMessage());
    assertEquals(ex, e.getCause());
  }

  @Test
  public void testConstructorMessageThrowable() {
    Exception ex = new Exception();
    PersonProviderException e = new PersonProviderException("Exception happened", ex);
    assertEquals("Exception happened", e.getMessage());
    assertEquals(ex, e.getCause());
  }
}
