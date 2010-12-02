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
package org.sakaiproject.nakamura.api.auth.trusted;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Marker interface for the private service.
 */
public interface TrustedTokenService {

  /**
   * Attribute name for the Credentials stored in the session, put there by the
   * TrustedAuthenticationServlet
   */
  static final String SA_AUTHENTICATION_CREDENTIALS = "sakai-trusted-authentication-credentials";

  /**
   * The attribute name in the Credentials for the TrustedUser, put there by the
   * TrustedAuthenticationServlet on login.
   */
  static final String CA_AUTHENTICATION_USER = "sakai-trusted-authentication-user";

  static final String EVENT_USER_ID = "user";

  static final String TRUST_USER_TOPIC = TrustedTokenService.class.getName().replace('.', '/')+"/trustuser";

  /**
   * Invalidated any trusted token in the response.
   * @param request
   * @param response
   */
  void dropCredentials(HttpServletRequest request, HttpServletResponse response);


}
