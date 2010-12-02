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

import org.apache.commons.lang.RandomStringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public abstract class AbstractAuthenticationHandler implements AuthenticationHandler {

  public static final Logger LOGGER = LoggerFactory.getLogger(AbstractAuthenticationHandler.class);
  public static final String AUTHENTICATION_OBJECT = "authentication-object";


  /**
   * {@inheritDoc}
   * @see org.apache.sling.auth.corei.AuthenticationHandler#dropCredentials(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public void dropCredentials(HttpServletRequest arg0, HttpServletResponse arg1)
      throws IOException {
    // Since this component's credentials are stored via the TrustedTokenService,
    // they should be cleared out by the TrustedAuthenticationHandler's dropCredentials.
    // No other action required here.
  }

  /**
   * @param request
   * @throws RepositoryException
   */
  protected final void doCreateUser(String userName) throws RepositoryException {
    Session session = null;
    try {
      session = getRespository().loginAdministrative(null); // usage checked and ok KERN-577

      UserManager userManager = AccessControlUtil.getUserManager(session);
      Authorizable authorizable = userManager.getAuthorizable(userName);

      if (authorizable == null) {
        // create user
        LOGGER.debug("Createing user {}", userName);
        userManager.createUser(userName, RandomStringUtils.random(32));

      }
    } catch (RepositoryException e) {
      LOGGER.error(e.getMessage(), e);
      throw (e);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
  }


  /**
   * {@inheritDoc}
   * @see orgorg.apache.sling.auth.coreuthenticationHandler#extractCredentials(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public AuthenticationInfo extractCredentials(HttpServletRequest request,
      HttpServletResponse response) {
    // no session authentication info, try the request
    System.err.println(" Extracting Credentials ");

    AbstractAuthentication authentication = createAuthenticationObject(request, response);
    if (authentication.isValid()) {
      // authenticate
      AuthenticationInfo authenticatioInfo = new AuthenticationInfo(getAuthType());
      authenticatioInfo.put(JcrResourceConstants.AUTHENTICATION_INFO_CREDENTIALS, authentication.getCredentials());
      // put the form authentication into the request so that it can be checked by the servlet and saved to session if valid.
      request.setAttribute(AUTHENTICATION_OBJECT, authentication);
      return authenticatioInfo;
    }

    return null;
  }

  /**
   * @param request
   * @return
   */
  protected  abstract AbstractAuthentication createAuthenticationObject(HttpServletRequest request, HttpServletResponse response);

  /**
   * @return the sling repository
   */
  protected abstract SlingRepository getRespository();

  /**
   * @return the authentication type for the authentication handler
   */
  protected abstract String getAuthType();

  /**
   * {@inheritDoc}
   * @see org.aporg.apache.sling.auth.coreenticationHandler#requestCredentials(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public boolean requestCredentials(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    // we should send a response that causes the user to login, probably a 401
    // but we might want to send to a redirect.
    response.setStatus(401);
    return true;
  }



}
