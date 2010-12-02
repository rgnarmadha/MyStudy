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
package org.sakaiproject.nakamura.auth.opensso;

import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;

/**
 * Authentication plugin used during the JCR authentication cycle. Is returned by
 * {@link OpenSsoLoginModulePlugin#getAuthentication(java.security.Principal, Credentials) to
 * be injected into the authentication cycle.
 */
public class OpenSsoAuthenticationPlugin implements AuthenticationPlugin {
  private OpenSsoLoginModulePlugin loginModulePlugin;

  public OpenSsoAuthenticationPlugin(OpenSsoLoginModulePlugin loginModulePlugin) {
    this.loginModulePlugin = loginModulePlugin;
  }

  /**
   * No actual authentication takes place here. By now, the CAS supplied credentials
   * will have been extracted and verified, and a getPrincipal() implementation will
   * have handled any necessary translation from the CAS principal name to a
   * Sling-appropriate principal. This only validates that the credentials
   * actually did come from the CAS handler.
   *
   * @see org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin#authenticate(javax.jcr.Credentials)
   */
  public boolean authenticate(Credentials credentials) throws RepositoryException {
    if (loginModulePlugin.canHandle(credentials)) {
      return true;
    } else {
      return false;
    }
  }
}
