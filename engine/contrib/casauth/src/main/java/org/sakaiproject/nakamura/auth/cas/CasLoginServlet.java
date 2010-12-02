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
package org.sakaiproject.nakamura.auth.cas;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.sakaiproject.nakamura.api.auth.trusted.TrustedTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Helper servlet for SSO authentication. The servlet simply redirects to
 * the configured SSO server via AuthenticationHandler requestCredentials.
 * To avoid a loop, if the request is already authenticated, the servlet redirects to
 * the path specified by the request parameter "resource", or to the root
 * path.
 * <p>
 * Once all authentication modules use Sling's authtype approach to trigger
 * requestCredentials, it should also be possible to reach SSO through any servlet
 * (including sling.commons.auth's LoginServlet) by setting the
 * sling:authRequestLogin request parameter to "SSO".
 */
@SlingServlet(paths = { "/system/sling/cas/login" }, methods = { "GET", "POST" })
public class CasLoginServlet extends SlingAllMethodsServlet {
  private static final long serialVersionUID = -1894135945816269913L;
  private static final Logger LOGGER = LoggerFactory.getLogger(CasLoginServlet.class);

  public static final String TRY_LOGIN = "sakaiauth:login";

  @Reference
  private CasAuthenticationHandler ssoAuthnHandler;

  @Reference
  private TrustedTokenService trustedTokenService;

  public CasLoginServlet() {
  }

  CasLoginServlet(CasAuthenticationHandler ssoAuthHandler,
      TrustedTokenService trustedTokenService) {
    this.ssoAuthnHandler = ssoAuthHandler;
    this.trustedTokenService = trustedTokenService;
  }

  @Override
  protected void service(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    // Check for possible loop after authentication.
    // #1 just went through auth
    // #2 already passed auth
    if (request.getAuthType() != null) {
      AuthenticationInfo authnInfo = (AuthenticationInfo) request
          .getAttribute(CasAuthenticationHandler.AUTHN_INFO);
      if (authnInfo != null) {
        CasAuthenticationTokenServiceWrapper tokenServiceWrapper = new CasAuthenticationTokenServiceWrapper(
            this, trustedTokenService);
        tokenServiceWrapper.addToken(request, response);
      }

      String redirectTarget = getReturnPath(request);
      if ((redirectTarget == null) || request.getRequestURI().equals(redirectTarget)) {
        redirectTarget = request.getContextPath() + "/";
      }
      LOGGER.info("Request already authenticated, redirecting to {}", redirectTarget);
      response.sendRedirect(redirectTarget);
      return;
    }

    // Pass control to the handler.
    if ("2".equals(request.getParameter(TRY_LOGIN))) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication Failed");
    } else if (!ssoAuthnHandler.requestCredentials(request, response)) {
      LOGGER.error("Unable to request credentials from handler");
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Cannot login");
    }
  }

  /**
   * In imitation of sling.formauth, use the "resource" parameter to determine
   * where the browser should go after successful authentication.
   * <p>
   * TODO The "sling.auth.redirect" parameter seems to make more sense, but it
   * currently causes a redirect to happen in SlingAuthenticator's
   * getAnonymousResolver method before handlers get a chance to requestCredentials.
   *
   * @param request
   * @return the path to which the browser should be directed after successful
   * authentication, or null if no destination was specified
   */
  String getReturnPath(HttpServletRequest request) {
    final String returnPath;
    Object resObj = request.getAttribute(Authenticator.LOGIN_RESOURCE);
    if ((resObj instanceof String) && ((String) resObj).length() > 0) {
      returnPath = (String) resObj;
    } else {
      String resource = request.getParameter(Authenticator.LOGIN_RESOURCE);
      if ((resource != null) && (resource.length() > 0)) {
        returnPath = resource;
      } else {
        returnPath = null;
      }
    }
    return returnPath;
  }

}
