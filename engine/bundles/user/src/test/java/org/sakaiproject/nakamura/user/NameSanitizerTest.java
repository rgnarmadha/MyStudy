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
package org.sakaiproject.nakamura.user;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import javax.jcr.RepositoryException;

public class NameSanitizerTest {

  @Test
  public void testValidGroup() {
    String name = "g-mygroup-foo";
    boolean result = testName(name, false);
    assertEquals(name + " is a correct name. This should pass.", true, result);
  }

  @Test
  public void testAnotherValidGroup() {
    String name = "mygroup-foo";
    boolean result = testName(name, false);
    assertEquals(name + " is an correct name. This should pass.", true, result);
  }
  
  @Test
  public void testShortUserName() {
    String name = "CD";
    boolean result = testName(name, true);
    assertEquals(name + " is an incorrect name. This should fail.", false, result);
  }
  
  @Test
  public void testEmailAsUsername() {
    String name = "g-man@gmail.com";
    boolean result = testName(name, true);
    assertEquals(name + " is a correct name. This should pass.", true, result);
  }
  
  @Test
  public void testInvalidCharactersGroup() {
    String name = "g-foo%$*bar";
    boolean result = testName(name, true);
    assertEquals(name + " is a correct name. This should pass.", true, result);
  }
  
  private boolean testName(String name, boolean isUser) {
    NameSanitizer san = new NameSanitizer(name, isUser);
    try {
      san.validate();
      return true;
    } catch (RepositoryException e) {
      return false;
    }
  }
}
