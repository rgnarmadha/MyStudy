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
package org.sakaiproject.nakamura.testutils.mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.commons.testing.jcr.MockValue;

import java.security.Principal;

import javax.jcr.Value;

/**
 *
 */
public class MockitoTestUtils {

  /**
   * Mocks an authorizable and the principal
   * 
   * @param id
   *          The ID that needs to be assigned to an authorizable
   * @param isGroup
   *          If this authorizable should represent a group or not.
   * @return The mocked authorizable
   * @throws Exception
   */
  public static Authorizable createAuthorizable(String id, boolean isGroup)
      throws Exception {
    Authorizable au = mock(Authorizable.class);
    Principal p = mock(Principal.class);
    String hashedPath = "/" + id.substring(0, 1) + "/" + id.substring(0, 2) + "/" + id;
    MockValue pathValue = new MockValue(hashedPath);

    // Principal mocking
    when(p.getName()).thenReturn(id);

    // Authorizable mocking
    when(au.getID()).thenReturn(id);
    when(au.hasProperty("path")).thenReturn(true);
    when(au.getProperty("path")).thenReturn(new Value[] { pathValue });
    when(au.getPrincipal()).thenReturn(p);
    return au;
  }
}
