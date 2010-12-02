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
package org.sakaiproject.nakamura.opensso.trusted;

import java.security.Principal;

/**
 * Represents a user that was validated successfully but could not be found within the
 * system.
 */
public class UnknownAuthenticatedPrincipal implements Principal {
  private final AbstractAuthentication auth;

  public UnknownAuthenticatedPrincipal(AbstractAuthentication auth) {
    this.auth = auth;
  }

  public String getName() {
    return auth.getUserId();
  }
}
