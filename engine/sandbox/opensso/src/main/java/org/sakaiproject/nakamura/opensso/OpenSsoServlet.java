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

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.auth.Authenticator;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.auth.trusted.TrustedTokenService;
import org.sakaiproject.nakamura.api.auth.trusted.TrustedTokenServiceWrapper;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.opensso.trusted.AbstractAuthServlet;

import java.io.IOException;
import java.util.Dictionary;

/**
 * The <code>OpenSsoServlet</code> provides an end point to login against. On GET it
 * will response with the remote username of the logged in user or "anonymous" if there is
 * no logged in user. On POST it will check that the POST was authenticated by the OpenSsoAuthenticationaHandler and the
 * requested user is the same user as associated with the session. If that is valid it will inject a trusted token into the response for future requests.
 */

@ServiceDocumentation(name = "OpenOss Login Servlet", shortDescription = "", description = {
    " Requests that are posted to this servlet will result in a TrustedToken being injected into the " +
    "response provided that the request was previously authenticated by the OpenSsoAuthenticationHandler " +
    "and the session user id matches the request login userid." },
    bindings = @ServiceBinding(type = BindingType.PATH, bindings = "/system/sling/opensso"), methods = {
    @ServiceMethod(name = "GET", description = "Simply respond with the ID of the current user."),
    @ServiceMethod(name = "POST", description = "Performs the login or logout operations using opensso redirects ", parameters = {
        @ServiceParameter(name = "sakaiauth:logout", description = "Perform a logout operation removing all state related to the user"),
        @ServiceParameter(name = "sakaiauth:login", description = "Perform a login operartion based on the username and password supplied."),
        @ServiceParameter(name = "d", description = "The destination URL on a sucessfull login")},
        response = {
       @ServiceResponse(code = 200, description = "On a sucessfull login the userid will be returned.") }) })
@SlingServlet(paths = { "/system/sling/formlogin" }, methods = { "GET", "POST" })
@Properties(value = {
    @Property(name = "service.description", value = "The Sakai Foundation"),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
public final class OpenSsoServlet extends AbstractAuthServlet {

  /**
   * The trusted token service used to inject a token into the response.
   */
  @Reference
  protected transient TrustedTokenService trustedTokenService;

  /**
   * The authenticator that may be used to perform a logout.
   */
  @Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY, policy=ReferencePolicy.DYNAMIC)
  protected Authenticator authenticator;

  private String serverUrl;

  /**
   *
   */
  private static final long serialVersionUID = -6303432993222973296L;

  @Property(value="https://login.nyu.edu/sso/UI/Login?goto=", description="The stub url of the Open SSO server")
  private static final String SSO_SERVER_STUB_URL = "opensso.serverurl";

  @SuppressWarnings("unchecked")
  public void activate(ComponentContext context) {
    Dictionary<String, Object> properties = context.getProperties();
    serverUrl = (String) properties.get(SSO_SERVER_STUB_URL);
  }

  /**
   * modify the response object to generate an authentication failed response. This will be a redirect to the OpenSso login location.
   * @param request
   * @param response
   * @throws IOException
   */
  @Override
  protected void sendAuthenticationFailed(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws IOException {
    OpenSsoHandler openSsoHandler = new OpenSsoHandler(request, response);
    openSsoHandler.sendAuthenticationFailed(serverUrl, request.getParameter(PARAM_DESTINATION));
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.opensso.trusted.AbstractAuthServlet#getAuthenticator()
   */
  @Override
  protected Authenticator getAuthenticator() {
    return this.authenticator;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.opensso.trusted.AbstractAuthServlet#getTokenWrapper(org.sakaiproject.nakamura.opensso.trusted.AbstractAuthServlet)
   */
  @Override
  protected TrustedTokenServiceWrapper getTokenWrapper() {
    return new OpenSsoAuthenticationTokenServiceWrapper(this, trustedTokenService);
  }

}
