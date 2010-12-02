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

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.auth.Authenticator;
import org.sakaiproject.nakamura.api.auth.trusted.TrustedTokenService;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.formauth.FormAuthenticationHandler.FormAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Session;
import javax.servlet.ServletException;

/**
 * The <code>FormLoginServlet</code> provides an end point to login against. On GET it
 * will response with the remote username of the logged in user or "anonymous" if there is
 * no logged in user. On POST, the FormAutenticationHandler will be invoked. see
 * {@link FormAuthenticationHandler} to see the parameters.
 *
 */

@ServiceDocumentation(name = "Form Login Servlet", shortDescription = "", description = {
    " In combination with the FormAuthenticationHandler that is bound to the Sling/OSGi handleSecurity Mechanism a POST to "
        + "this servlet results in the FormAuthentciation object that will have been placed in the request as an attribute, being added to the HTTPSession"
        + "for later use. So that the FormAuthenticationHandler can use the FormAuthentication object bound into the session on subsiquent requests, "
        + "to authenticate the request",
    "Security in the OSGi Http service is achieved through a method in that service handleSecurity that all requests pass through. Classes that are "
        + "bound into that method see all requests and can perform modifications to the request based on the details of the request and any state that they "
        + "can access at that time. Since we only want to modify session state when a user explicitly askes to login using a form, we dont store anything in  "
        + "outside the request scope until the user POSTs to this servlet. For anyone reading this documentation in the code, they should look at FormAuthenticatonHandler "
        + "for more informtion on the protocol, but for those reading this documentation online that information is reproduced here." }, bindings = @ServiceBinding(type = BindingType.PATH, bindings = "/system/sling/formlogin"), methods = {
    @ServiceMethod(name = "GET", description = "Simply respond with the ID of the current user."),
    @ServiceMethod(name = "POST", description = "Performs the login operation using form data If sakai:login is set a username and password are required, which, "
        + "for a sucessfull login must be valid JCR session credentials", parameters = {
        @ServiceParameter(name = "sakaiauth:login", description = "Perform a login operartion based on the username and password supplied."),
        @ServiceParameter(name = "sakaiauth:un", description = "The Username for the login attempt"),
        @ServiceParameter(name = "sakaiauth:pw", description = "The Password for the login attempt") }, response = {
        @ServiceResponse(code = 403, description = "If the login is not sucessful a 403 will be returned at the point at which the credentials supplied are "
            + "used to establish a session in the JCR."),
        @ServiceResponse(code = 200, description = "On a sucessfull login the userid will be returned.") }) })
@SlingServlet(paths = { "/system/sling/formlogin" }, methods = { "GET", "POST" })
@Properties(value = {
    @Property(name = "service.description", value = "The Sakai Foundation"),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
public class FormLoginServlet extends SlingAllMethodsServlet {

  @Reference
  protected transient TrustedTokenService trustedTokenService;

  @Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY, policy=ReferencePolicy.DYNAMIC)
  protected Authenticator authenticator;

  /**
   *
   */
  private static final long serialVersionUID = -6303432993222973296L;
  private static final String PARAM_DESTINATION = "d";
  public static final String TRY_LOGIN = "sakaiauth:login";
  public static final String USERNAME = "sakaiauth:un";
  public static final String PASSWORD = "sakaiauth:pw";

  private static final Logger LOGGER = LoggerFactory.getLogger(FormLoginServlet.class);

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    response.setContentType("text/plain");
    response.setCharacterEncoding("UTF-8");

  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      // was the request just authenticated ?
      // If the Formauthentication object got to this point, a session was created and
      // logged in, therefore the
      // username and password have been checked by logging into the JCR. We can safely
      // capture the FormAuthentication
      // object in session. (or we could use the secure token at this point to avoid
      // session usage.)
      FormAuthentication authenticaton = (FormAuthentication) request
      .getAttribute(FormAuthenticationHandler.FORM_AUTHENTICATION);
      if (authenticaton != null) {
        if (authenticaton.isValid()) {

          // the request has now been authenticated, hence its valid.
          // just check that session userID is the same as the login user ID.
          Session session = request.getResourceResolver().adaptTo(Session.class);
          String userId = session.getUserID();
          String authUser = authenticaton.getUserId();
          if (userId.equals(authUser)) {
            FormAuthenticationTokenServiceWrapper formAuthenticationTokenService = new FormAuthenticationTokenServiceWrapper(
                this, trustedTokenService);
            formAuthenticationTokenService.addToken(request, response);
          } else {
            LOGGER.warn("Authentication failed for {} session user was {}", authUser, userId);
            response.setStatus(401);
            return;
          }
        }

        String destination = request.getParameter(PARAM_DESTINATION);

        if (destination != null) {
          // ensure that the redirect is safe and not susceptible to hacking
          response.sendRedirect(destination.replace('\n', ' ').replace('\r', ' '));
          return;
        }

      } else {
        LOGGER.debug("No Authentication Provided ");
      }
      doGet(request, response);
      return;
    } catch (IllegalStateException ise) {
      LOGGER.error("doPOST: Response already committed, cannot login");
      return;
    }
  }

}
