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

import static org.apache.sling.jcr.resource.JcrResourceConstants.AUTHENTICATION_INFO_CREDENTIALS;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * This is the Form Based Login Authenticator, its mounted at / and is invoked via the
 * OSGi HttpService handleSecurity call on the context. It is also invoked explicitly via
 * the FormLoginServlet via that authenticator service.
 * </p>
 * <p>
 * When the request is invoked, if the method is a POST, the authentication mechanism will
 * be invoked. If the method is not a post an attempt to get credentials out of the
 * session will be made, if they are found, then the session stored credentials will be
 * used to authenticate the request against the JCR.
 * </p>
 * <p>
 * On POST, if sakaiauth:login = 1, authentication will take place.
 * The request parameter "sakaiauth:un" will be used for the username and
 * "sakaiauth:pw" for the password.
 * </p>
 * <p>
 * Login should be attempted at /system/sling/formlogin where there is a servlet mounted
 * to handle the request, performing this operation anywhere else will result in a
 * resource creation, as POST creates a resource in the default SLing POST mechanism.
 * </p>
 * <p>
 * See {@link FormLoginServlet} to see a description of POST and GET to that servlet.
 * </p>
 *
 */
@Component(immediate=true, metatype=true, label="%auth.http.name", description="%auth.http.description")
@Service(value=AuthenticationHandler.class)
@Properties( value={
    @Property(name="service.description",value="Form Authentication Handler"),
    @Property(name="service.vendor",value="The Sakai Foundation"),
    @Property(name=AuthenticationHandler.PATH_PROPERTY, value="/" ),
    @Property(name=AuthenticationHandler.TYPE_PROPERTY, value="FormAuthenticationHandler" )
})
public final class FormAuthenticationHandler implements AuthenticationHandler {



  /**
   *
   */
  final class FormAuthentication {

    /**
     *
     */
    private static final long serialVersionUID = 8019850707623762839L;
    private boolean valid;
    private SimpleCredentials credentials;

    /**
     * @param request
     */
    FormAuthentication(HttpServletRequest request) {
      valid = false;
      if ("POST".equals(request.getMethod())) {
        if ("1".equals(request.getParameter(FormLoginServlet.TRY_LOGIN))) {
          LOGGER.debug(" login as {} ",request.getParameter(FormLoginServlet.USERNAME));
          String password = request.getParameter(FormLoginServlet.PASSWORD);
          if (password == null) {
            credentials = new SimpleCredentials(request.getParameter(FormLoginServlet.USERNAME),
                new char[0]);
          } else {
            credentials = new SimpleCredentials(request.getParameter(FormLoginServlet.USERNAME), password
                .toCharArray());
          }
          valid = true;
        } else {
          LOGGER.debug("Login was not requested ");
        }
      }
    }

    /**
     * @return
     */
    boolean isValid() {
      return valid;
    }

    /**
     * @return
     */
    Credentials getCredentials() {
      return credentials;
    }

    /**
     * @return
     */
    public String getUserId() {
      return credentials.getUserID();
    }

  }

  private static final Logger LOGGER = LoggerFactory
      .getLogger(FormAuthenticationHandler.class);
  static final String FORM_AUTHENTICATION = FormAuthentication.class.getName();
  public static final String SESSION_AUTH = FormAuthenticationHandler.class.getName();

  /**
   * {@inheritDoc}
   * @see org.apache.sling.auth.corei.AuthenticationHandler#extractCredentials(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public AuthenticationInfo extractCredentials(HttpServletRequest request,
      HttpServletResponse response) {

    // no session authentication info, try the request

    FormAuthentication authentication = new FormAuthentication(request);
    if (authentication.isValid()) {
      // authenticate
      AuthenticationInfo authenticatioInfo = new AuthenticationInfo(SESSION_AUTH);
      authenticatioInfo.put(AUTHENTICATION_INFO_CREDENTIALS, authentication.getCredentials());
      // put the form authentication into the request so that it can be checked by the servlet and saved to session if valid.
      request.setAttribute(FORM_AUTHENTICATION, authentication);
      return authenticatioInfo;
    }

    return null;
  }


  /**
   * {@inheritDoc}
   * @see orgorg.apache.sling.auth.coreuthenticationHandler#dropCredentials(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public void dropCredentials(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    // Since this component's credentials are stored via the TrustedTokenService,
    // they should be cleared out by the TrustedAuthenticationHandler's dropCredentials.
    // No other action required here.
  }

  /**
   * {@inheritDoc}
   * @see org.aporg.apache.sling.auth.coreenticationHandler#requestCredentials(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public boolean requestCredentials(HttpServletRequest request,
      HttpServletResponse response) throws IOException {
    // we should send a response that causes the user to login, probably a 401
    // but we might want to send to a HTML form.
    response.setStatus(401);
    return true;
  }

}
