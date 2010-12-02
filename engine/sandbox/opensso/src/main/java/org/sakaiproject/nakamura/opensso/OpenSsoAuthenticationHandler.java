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
package org.sakaiproject.nakamura.opensso;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.jcr.api.SlingRepository;
import org.sakaiproject.nakamura.opensso.trusted.AbstractAuthentication;
import org.sakaiproject.nakamura.opensso.trusted.AbstractAuthenticationHandler;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This is an authentication handler that implements OpenSSO, it uses a OpenSsoHandler to
 * deal with the protocol implementation.
 */
@Component(immediate = true)
@Service
public final class OpenSsoAuthenticationHandler extends AbstractAuthenticationHandler {

  /**
   * Path on which this authentication should be activated. Its active on all paths, as
   * the authentication itself is performed by the TrustedAuthenticationServlet that
   * places credentials in the session. Those credentials are then used to authenticate
   * all subsequent requests.
   */
  @Property(value = "/")
  static final String PATH_PROPERTY = AuthenticationHandler.PATH_PROPERTY;

  @Property(value = "Open SSO Authentication Handler")
  static final String DESCRIPTION_PROPERTY = "service.description";

  @Property(value = "The Sakai Foundation")
  static final String VENDOR_PROPERTY = "service.vendor";

  /**
   * Internal protected final Authentication object, that is injected into the
   * creadentials (for use by the LoginModule) and the request for use by the
   * OpenSsoServlet as attributes. The class will be created with valid == false if the
   * authentication is invalid.
   */
  final class OpenSsoAuthentication extends AbstractAuthentication {

    /**
     * Construct a new OpenSsoAuthencitation object from the request and response.
     *
     * @param request
     * @param response
     */
    OpenSsoAuthentication(HttpServletRequest request, HttpServletResponse response) {
      super(request, response);
      System.err.println(" Creating authentication valid:"+isValid());
    }

    /**
     * {@inheritDoc} Creates a user in the repository by invoking the handler.
     *
     * @throws RepositoryException
     * @see org.sakaiproject.nakamura.opensso.AbstractAuthentication#createUser(java.lang.String)
     */
    @Override
    protected final void createUser(String userName) throws RepositoryException {
      OpenSsoAuthenticationHandler.this.createUser(userName);
    }

    /**
     * {@inheritDoc} Get the Username from the request vial the AuthenciationHandler.
     *
     * @see org.sakaiproject.nakamura.opensso.AbstractAuthentication#getUserName(javax.servlet.http.HttpServletRequest)
     */
    @Override
    protected final String getUserName(HttpServletRequest request,
        HttpServletResponse response) {
      return OpenSsoAuthenticationHandler.this.getUserName(request, response);
    }

  }

  /**
   * The type of this handler.
   */
  private static final String OPEN_SSO_AUTHTYPE = OpenSsoAuthenticationHandler.class
      .getName();

  /**
   * The SLign repository used when creating a new user.
   */
  @Reference
  private SlingRepository repository;

  /**
   * Create a new used, using the parent implementation.
   *
   * @param userName
   * @throws RepositoryException
   */
  private void createUser(String userName) throws RepositoryException {
    super.doCreateUser(userName);
  }

  /**
   * Extract the user name from the request if there are sufficient credentials encoded in
   * the request.
   *
   * @param request
   * @param response
   * @return
   */
  private String getUserName(HttpServletRequest request, HttpServletResponse response) {
    OpenSsoHandler openSsoHandler = new OpenSsoHandler(request, response);
    return openSsoHandler.getUserName();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.opensso.AbstractAuthenticationHandler#getAuthType()
   */
  @Override
  protected String getAuthType() {
    return OPEN_SSO_AUTHTYPE;
  }

  /**
   * {@inheritDoc}
   * Creates a new abstract authentication which my not be valid.
   * @see org.sakaiproject.nakamura.opensso.AbstractAuthenticationHandler#getAuthenticationObject(javax.servlet.http.HttpServletRequest)
   */
  @Override
  protected final AbstractAuthentication createAuthenticationObject(
      HttpServletRequest request, HttpServletResponse response) {
    System.err.println(" Creating authentication object ");
    return new OpenSsoAuthentication(request, response);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.opensso.AbstractAuthenticationHandler#getRespository()
   */
  @Override
  protected SlingRepository getRespository() {
    return repository;
  }




}
