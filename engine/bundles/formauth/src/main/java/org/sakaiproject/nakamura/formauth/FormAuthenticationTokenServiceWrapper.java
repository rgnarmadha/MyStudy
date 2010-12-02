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
package org.sakaiproject.nakamura.formauth;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.sakaiproject.nakamura.api.auth.trusted.TrustedTokenService;
import org.sakaiproject.nakamura.api.auth.trusted.TrustedTokenServiceWrapper;

/**
 *
 */
class FormAuthenticationTokenServiceWrapper extends TrustedTokenServiceWrapper {
  /**
   * @param delegate
   */
  FormAuthenticationTokenServiceWrapper(FormLoginServlet servlet, TrustedTokenService delegate) {
    super(validate(servlet,delegate));
  }

  /**
   * @param servlet
   * @return
   */
  private static TrustedTokenService validate(FormLoginServlet servlet, TrustedTokenService delegate) {
    if ( !FormLoginServlet.class.equals(servlet.getClass()) ) {
      throw new IllegalArgumentException("Invalid use of FormAuthenticationTonkenService");
    }
    return delegate;
  }

  /**
   * @param request
   * @param response
   */
  public final void addToken(SlingHttpServletRequest request,
      SlingHttpServletResponse response) {
    injectToken(request, response);
  }
  

}
