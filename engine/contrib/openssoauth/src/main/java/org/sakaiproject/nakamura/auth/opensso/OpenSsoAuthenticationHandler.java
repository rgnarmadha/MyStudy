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

import static org.apache.sling.jcr.resource.JcrResourceConstants.AUTHENTICATION_INFO_CREDENTIALS;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Services;
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.auth.core.spi.AuthenticationFeedbackHandler;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.auth.core.spi.DefaultAuthenticationFeedbackHandler;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.ModificationType;
import org.osgi.framework.Constants;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.ValueFactory;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class integrates SSO with the Sling authentication framework.
 * The integration is needed only due to limitations on servlet filter
 * support in the OSGi / Sling environment.
 */
@Component(metatype = true)
@Services({
    @Service(value = OpenSsoAuthenticationHandler.class),
    @Service(value = AuthenticationHandler.class),
    @Service(value = AuthenticationFeedbackHandler.class)
})
@Properties(value = {
    @Property(name = Constants.SERVICE_RANKING, intValue = -5),
    @Property(name = AuthenticationHandler.PATH_PROPERTY, value = "/"),
    @Property(name = AuthenticationHandler.TYPE_PROPERTY, value = OpenSsoAuthenticationHandler.AUTH_TYPE, propertyPrivate = true),
    @Property(name = OpenSsoAuthenticationHandler.SSO_AUTOCREATE_USER, boolValue = OpenSsoAuthenticationHandler.DEFAULT_SSO_AUTOCREATE_USER),
    @Property(name = OpenSsoAuthenticationHandler.LOGIN_URL, value = OpenSsoAuthenticationHandler.DEFAULT_LOGIN_URL),
    @Property(name = OpenSsoAuthenticationHandler.LOGOUT_URL, value = OpenSsoAuthenticationHandler.DEFAULT_LOGOUT_URL),
    @Property(name = OpenSsoAuthenticationHandler.SERVER_URL, value = OpenSsoAuthenticationHandler.DEFAULT_SERVER_URL),
    @Property(name = OpenSsoAuthenticationHandler.ATTRIBUTES_NAMES, value = OpenSsoAuthenticationHandler.DEFAULT_ATTRIBUTE_NAME),
    @Property(name = OpenSsoAuthenticationHandler.MISSING_LOCAL_USER_URL, value = OpenSsoAuthenticationHandler.DEFAULT_MISSING_LOCAL_USER_URL)
})
public class OpenSsoAuthenticationHandler implements AuthenticationHandler,
    AuthenticationFeedbackHandler {

  public static final String AUTH_TYPE = "SSO";

  private static final Logger LOGGER = LoggerFactory
      .getLogger(OpenSsoAuthenticationHandler.class);

  static final String DEFAULT_ARTIFACT_NAME = "iPlanetDirectoryPro";
  static final String DEFAULT_SUCCESSFUL_BODY = "boolean=true\n";

  static final String DEFAULT_LOGIN_URL = "http://localhost/sso/UI/Login";
  static final String DEFAULT_LOGOUT_URL = "http://localhost/sso/UI/Logout";
  static final String DEFAULT_SERVER_URL = "http://localhost/sso";
  static final String DEFAULT_ATTRIBUTE_NAME = "uid";
  static final String DEFAULT_MISSING_LOCAL_USER_URL = "/dev/500.html";

  static final String USRDTLS_ATTR_NAME_STUB = "userdetails.attribute.name=";
  static final String USRDTLS_ATTR_VAL_STUB = "userdetails.attribute.value=";

  /** Represents the constant for where the assertion will be located in memory. */
  static final String SSO_AUTOCREATE_USER = "sakai.auth.sso.user.autocreate";

  static final String AUTHN_INFO = "org.sakaiproject.nakamura.auth.sso.SsoAuthnInfo";
  public static final boolean DEFAULT_SSO_AUTOCREATE_USER = false;
  private boolean autoCreateUser;

  // needed for the automatic user creation.
  @Reference
  protected SlingRepository repository;

  @Reference
  protected AuthorizablePostProcessService authzPostProcessService;

  static final String LOGIN_URL = "sakai.auth.sso.url.login";
  private String loginUrl;

  static final String LOGOUT_URL = "sakai.auth.sso.url.logout";
  private String logoutUrl;

  static final String SERVER_URL = "sakai.auth.sso.url.server";
  private String serverUrl;

  static final String ATTRIBUTES_NAMES = "sakai.auth.sso.opensso.user.attribute";
  private String attributeName;

  static final String MISSING_LOCAL_USER_URL = "sakai.auth.sso.user.missing";
  private String missingLocalUserUrl;

  /**
   * Define the set of authentication-related query parameters which should
   * be removed from the "service" URL sent to the SSO server.
   */
  Set<String> filteredQueryStrings = new HashSet<String>(
      Arrays.asList(REQUEST_LOGIN_PARAMETER));

  public OpenSsoAuthenticationHandler() {
  }

  OpenSsoAuthenticationHandler(SlingRepository repository,
      AuthorizablePostProcessService authzPostProcessService) {
    this.repository = repository;
    this.authzPostProcessService = authzPostProcessService;
  }

  //----------- OSGi integration ----------------------------
  @Activate
  protected void activate(Map<?, ?> props) {
    modified(props);
  }

  @Modified
  protected void modified(Map<?, ?> props) {
    loginUrl = OsgiUtil.toString(props.get(LOGIN_URL), DEFAULT_LOGIN_URL);
    logoutUrl = OsgiUtil.toString(props.get(LOGOUT_URL), DEFAULT_LOGOUT_URL);
    serverUrl = OsgiUtil.toString(props.get(SERVER_URL), DEFAULT_SERVER_URL);
    missingLocalUserUrl = OsgiUtil.toString(props.get(MISSING_LOCAL_USER_URL),
        DEFAULT_MISSING_LOCAL_USER_URL);

    attributeName = OsgiUtil.toString(props.get(ATTRIBUTES_NAMES), DEFAULT_ATTRIBUTE_NAME);
    autoCreateUser = OsgiUtil.toBoolean(props.get(SSO_AUTOCREATE_USER), false);
  }

  //----------- AuthenticationHandler interface ----------------------------

  public void dropCredentials(HttpServletRequest request, HttpServletResponse response)
      throws IOException {

    String target = (String) request.getAttribute(Authenticator.LOGIN_RESOURCE);
    if (StringUtils.isBlank(target)) {
      target = request.getParameter(Authenticator.LOGIN_RESOURCE);
    }

    if (target != null && target.length() > 0 && !("/".equals(target))) {
      LOGGER.info(
          "SSO logout about to override requested redirect to {} and instead redirect to {}",
          target, logoutUrl);
    } else {
      LOGGER.debug("SSO logout will request redirect to {}", logoutUrl);
    }
    request.setAttribute(Authenticator.LOGIN_RESOURCE, logoutUrl);
  }

  public AuthenticationInfo extractCredentials(HttpServletRequest request,
      HttpServletResponse response) {
    LOGGER.debug("extractCredentials called");

    AuthenticationInfo authnInfo = null;

    String artifact = extractArtifact(request);

    if (artifact != null) {
      try {
        // make REST call to validate artifact
        String validateUrl = serverUrl + "/identity/isTokenValid?tokenid=" + artifact;
        GetMethod get = new GetMethod(validateUrl);
        HttpClient httpClient = new HttpClient();
        int returnCode = httpClient.executeMethod(get);

        if (returnCode >= 200 && returnCode < 300) {
          // successful call; test for valid response
          String body = get.getResponseBodyAsString();
          String credentials = retrieveCredentials(artifact, body, request);
          if (credentials != null) {
            // found some credentials; proceed
            authnInfo = createAuthnInfo(credentials);

            request.setAttribute(AUTHN_INFO, authnInfo);
          } else {
            LOGGER.warn("Unable to extract credentials from validation server.");
            authnInfo = AuthenticationInfo.FAIL_AUTH;
          }
        } else {
          LOGGER.error("Failed response from validation server: [" + returnCode + "]");
          authnInfo = AuthenticationInfo.FAIL_AUTH;
        }
      } catch (Exception e) {
        LOGGER.error(e.getMessage(), e);
      }
    }

    return authnInfo;
  }

  /**
   * Called after extractCredentials has returne non-null but logging into the repository
   * with the provided AuthenticationInfo failed.<br/>
   *
   * {@inheritDoc}
   *
   * @see org.apache.sling.auth.corei.AuthenticationHandler#requestCredentials(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  public boolean requestCredentials(HttpServletRequest request,
      HttpServletResponse response) throws IOException {
    LOGGER.debug("requestCredentials called");

    final String service = constructServiceParameter(request);
    LOGGER.debug("Service URL = \"{}\"", service);
    final String urlToRedirectTo = loginUrl + "?goto=" + service;
    LOGGER.debug("Redirecting to: \"{}\"", urlToRedirectTo);
    response.sendRedirect(urlToRedirectTo);
    return true;
  }

  //----------- AuthenticationFeedbackHandler interface ----------------------------

  /**
   * {@inheritDoc}
   *
   * @see orgorg.apache.sling.auth.coreuthenticationFeedbackHandler#authenticationFailed(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse,
   *      org.aporg.apache.sling.auth.coreenticationInfo)
   */
  public void authenticationFailed(HttpServletRequest request,
      HttpServletResponse response, AuthenticationInfo authInfo) {
//    LOGGER.debug("authenticationFailed called");
//    final HttpSession session = request.getSession(false);
//    if (session != null) {
//      final SsoPrincipal principal = (SsoPrincipal) session
//          .getAttribute(CONST_SSO_ASSERTION);
//      if (principal != null) {
//        LOGGER.warn("SSO assertion is set", new Exception());
//      }
//    }
  }

  /**
   * If a redirect is configured, this method will take care of the redirect.
   * <p>
   * If user auto-creation is configured, this method will check for an existing
   * Authorizable that matches the principal. If not found, it creates a new Jackrabbit
   * user with all properties blank except for the ID and a randomly generated password.
   * WARNING: Currently this will not perform the extra work done by the Nakamura
   * CreateUserServlet, and the resulting user will not be associated with a valid
   * profile.
   * <p>
   * Note: do not try to inject the token here.  The request has not had the authenticated
   * user added to it so request.getUserPrincipal() and request.getRemoteUser() both
   * return null.
   * <p>
   * TODO This really needs to be dropped to allow for user pull, person directory
   * integrations, etc. See SLING-1563 for the related issue of user population via
   * OpenID.
   *
   * @see org.apache.sling.auth.core.spi.AuthenticationFeedbackHandler#authenticationSucceeded(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse,
   *      org.apache.sorg.apache.sling.auth.coretionInfo)
   */
  public boolean authenticationSucceeded(HttpServletRequest request,
      HttpServletResponse response, AuthenticationInfo authInfo) {
    LOGGER.debug("authenticationSucceeded called");

    // If the plug-in is intended to verify the existence of a matching Authorizable,
    // check that now.
    boolean isUserValid = isUserValid(authInfo);
    if (!isUserValid) {
      LOGGER.warn("SSO authentication succeeded but corresponding user not found or created");
      try {
        dropCredentials(request, response);
      } catch (IOException e) {
        LOGGER.error(
            "Failed to drop credentials after SSO authentication by invalid user", e);
      }
      try {
        // redirect over to SSO to logout to invalidate the session, then return to our
        // server to tell the user about the missing local user.
        String localUrl = request.getScheme() + "://" + request.getServerName() + ":"
            + request.getServerPort() + missingLocalUserUrl;
        String redirectUrl = response.encodeRedirectURL(logoutUrl + "?goto=" + localUrl);
        response.sendRedirect(redirectUrl);
      } catch (IOException e) {
        LOGGER.error(e.getMessage(), e);
      }
      return true;
    }

    // Check for the default post-authentication redirect.
    return DefaultAuthenticationFeedbackHandler.handleRedirect(request, response);
  }

  //----------- Internal ----------------------------
  private AuthenticationInfo createAuthnInfo(final String username) {
    final SsoPrincipal principal = new SsoPrincipal(username);
    AuthenticationInfo authnInfo = new AuthenticationInfo(AUTH_TYPE, username);
    SimpleCredentials credentials = new SimpleCredentials(principal.getName(),
        new char[] {});
    credentials.setAttribute(SsoPrincipal.class.getName(), principal);
    authnInfo.put(AUTHENTICATION_INFO_CREDENTIALS, credentials);
    return authnInfo;
  }

  /**
   * @param request
   * @return the URL to which the SSO server should redirect after successful
   * authentication. By default, this is the same URL from which authentication
   * was initiated (minus authentication-related query strings like "ticket").
   * A request attribute or parameter can be used to specify a different
   * return path.
   */
  protected String constructServiceParameter(HttpServletRequest request)
      throws UnsupportedEncodingException {
    StringBuffer url = request.getRequestURL().append("?");

    String queryString = request.getQueryString();
    String tryLogin = OpenSsoLoginServlet.TRY_LOGIN + "=2";
    if (queryString == null || queryString.indexOf(tryLogin) == -1) {
      url.append(tryLogin).append("&");
    }

    if (queryString != null) {
      String[] parameters = StringUtils.split(queryString, '&');
      for (String parameter : parameters) {
        String[] keyAndValue = StringUtils.split(parameter, "=", 2);
        String key = keyAndValue[0];
        if (!filteredQueryStrings.contains(key)) {
          url.append(parameter).append("&");
        }
      }
    }

    String encodedUrl = URLEncoder.encode(url.toString(), "UTF-8");
    return encodedUrl;
  }

  private boolean isUserValid(AuthenticationInfo authInfo) {
    boolean isUserValid = false;
    final String username = authInfo.getUser();
    // Check for a matching Authorizable. If one isn't found, create
    // a new user.
    Session session = null;
    try {
      session = repository.loginAdministrative(null); // usage checked and ok KERN-577
      UserManager userManager = AccessControlUtil.getUserManager(session);
      Authorizable authorizable = userManager.getAuthorizable(username);
      if (authorizable == null) {
        if (autoCreateUser) {
          createUser(username, session);
          isUserValid = true;
        }
      } else {
        isUserValid = true;
      }
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
    return isUserValid;
  }

  /**
   * TODO This logic should probably be supplied by a shared service rather
   * than copied and pasted across components.
   */
  private User createUser(String principalName, Session session) throws Exception {
    LOGGER.info("Creating user {}", principalName);
    UserManager userManager = AccessControlUtil.getUserManager(session);
    User user = userManager.createUser(principalName, RandomStringUtils.random(32));
    ItemBasedPrincipal principal = (ItemBasedPrincipal) user.getPrincipal();
    String path = principal.getPath();
    path = path.substring(UserConstants.USER_REPO_LOCATION.length());
    ValueFactory valueFactory = session.getValueFactory();
    user.setProperty("path", valueFactory.createValue(path));

    if (authzPostProcessService != null) {
      authzPostProcessService.process(user, session, ModificationType.CREATE);
    }
    return user;
  }

  private String extractArtifact(HttpServletRequest request) {
    String artifact = null;
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (DEFAULT_ARTIFACT_NAME.equals(cookie.getName())) {
          artifact = cookie.getValue();
          break;
        }
      }
    }
    return artifact;
  }

  private String retrieveCredentials(String artifact, String responseBody,
      HttpServletRequest request) {
    String username = null;

    try {
      if (DEFAULT_SUCCESSFUL_BODY.equals(responseBody)) {
        String url = serverUrl + "/identity/attributes?attributes_names=" + attributeName
            + "&subjectid=" + artifact;
        GetMethod get = new GetMethod(url);
        HttpClient httpClient = new HttpClient();
        int returnCode = httpClient.executeMethod(get);
        String body = get.getResponseBodyAsString();

        if (returnCode >= 200 && returnCode < 300) {
          BufferedReader br = new BufferedReader(new StringReader(body));
          String attrLine = USRDTLS_ATTR_NAME_STUB + attributeName;
          String line = null;
          boolean getNextValue = false;
          while ((line = br.readLine()) != null) {
            if (getNextValue && line.startsWith(USRDTLS_ATTR_VAL_STUB)) {
              username = line.substring(USRDTLS_ATTR_VAL_STUB.length());
              break;
            } else if (attrLine.equals(line)) {
              getNextValue = true;
            }
          }
        }
      }
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
    }
    return username;
  }

  static final class SsoPrincipal implements Principal {
    private static final long serialVersionUID = -6232157660434175773L;
    private String principalName;

    public SsoPrincipal(String principalName) {
      this.principalName = principalName;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.security.Principal#getName()
     */
    public String getName() {
      return principalName;
    }
  }
}
