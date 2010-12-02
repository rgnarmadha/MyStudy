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

import static org.apache.sling.jcr.resource.JcrResourceConstants.AUTHENTICATION_INFO_CREDENTIALS;

import com.ctc.wstx.stax.WstxInputFactory;

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

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.ValueFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * This class integrates SSO with the Sling authentication framework.
 * The integration is needed only due to limitations on servlet filter
 * support in the OSGi / Sling environment.
 */
@Component(metatype = true)
@Services({
    @Service(value = CasAuthenticationHandler.class),
    @Service(value = AuthenticationHandler.class),
    @Service(value = AuthenticationFeedbackHandler.class)
})
@Properties(value = {
    @Property(name = Constants.SERVICE_RANKING, intValue = -5),
    @Property(name = AuthenticationHandler.PATH_PROPERTY, value = "/"),
    @Property(name = AuthenticationHandler.TYPE_PROPERTY, value = CasAuthenticationHandler.AUTH_TYPE, propertyPrivate = true),
    @Property(name = CasAuthenticationHandler.AUTOCREATE_USER, boolValue = CasAuthenticationHandler.DEFAULT_SSO_AUTOCREATE_USER),
    @Property(name = CasAuthenticationHandler.LOGIN_URL, value = CasAuthenticationHandler.DEFAULT_LOGIN_URL),
    @Property(name = CasAuthenticationHandler.LOGOUT_URL, value = CasAuthenticationHandler.DEFAULT_LOGOUT_URL),
    @Property(name = CasAuthenticationHandler.SERVER_URL, value = CasAuthenticationHandler.DEFAULT_SERVER_URL),
    @Property(name = CasAuthenticationHandler.RENEW, boolValue = CasAuthenticationHandler.DEFAULT_RENEW),
    @Property(name = CasAuthenticationHandler.GATEWAY, boolValue = CasAuthenticationHandler.DEFAULT_GATEWAY)
})
public class CasAuthenticationHandler implements AuthenticationHandler,
    AuthenticationFeedbackHandler {

  public static final String AUTH_TYPE = "CAS";

  private static final Logger LOGGER = LoggerFactory
      .getLogger(CasAuthenticationHandler.class);

  static final String DEFAULT_ARTIFACT_NAME = "ticket";
  static final String DEFAULT_LOGIN_URL = "http://localhost/cas/login";
  static final String DEFAULT_LOGOUT_URL = "http://localhost/cas/logout";
  static final String DEFAULT_SERVER_URL = "http://localhost/cas";
  static final boolean DEFAULT_RENEW = false;
  static final boolean DEFAULT_GATEWAY = false;
  static final boolean DEFAULT_SSO_AUTOCREATE_USER = false;

  /** Represents the constant for where the assertion will be located in memory. */
  static final String AUTHN_INFO = "org.sakaiproject.nakamura.auth.cas.SsoAuthnInfo";

  // needed for the automatic user creation.
  @Reference
  protected SlingRepository repository;

  @Reference
  protected AuthorizablePostProcessService authzPostProcessService;

  static final String LOGIN_URL = "sakai.auth.cas.url.login";
  private String loginUrl;

  static final String LOGOUT_URL = "sakai.auth.cas.url.logout";
  private String logoutUrl;

  static final String SERVER_URL = "sakai.auth.cas.url.server";
  private String serverUrl;

  static final String RENEW = "sakai.auth.cas.prop.renew";
  private boolean renew;

  static final String GATEWAY = "sakai.auth.cas.prop.gateway";
  private boolean gateway;

  static final String AUTOCREATE_USER = "sakai.auth.cas.user.autocreate";
  private boolean autoCreateUser;

  /**
   * Define the set of authentication-related query parameters which should
   * be removed from the "service" URL sent to the SSO server.
   */
  Set<String> filteredQueryStrings = new HashSet<String>(Arrays.asList(
      REQUEST_LOGIN_PARAMETER, DEFAULT_ARTIFACT_NAME));

  public CasAuthenticationHandler() {
  }

  CasAuthenticationHandler(SlingRepository repository,
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

    autoCreateUser = OsgiUtil.toBoolean(props.get(AUTOCREATE_USER), false);
    renew = OsgiUtil.toBoolean(props.get(RENEW), DEFAULT_RENEW);
    gateway = OsgiUtil.toBoolean(props.get(GATEWAY), DEFAULT_GATEWAY);
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
        String service = constructServiceParameter(request);
        String validateUrl = serverUrl + "/serviceValidate?service=" + service + "&ticket=" + artifact;
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
    final String urlToRedirectTo = constructLoginUrl(request, service);
    LOGGER.debug("Redirecting to: \"{}\"", urlToRedirectTo);
    response.sendRedirect(urlToRedirectTo);
    return true;
  }

  private String constructLoginUrl(HttpServletRequest request, String service) {
    ArrayList<String> params = new ArrayList<String>();

    String renewParam = request.getParameter("renew");
    boolean renew = this.renew;
    if (renewParam != null) {
      renew = Boolean.parseBoolean(renewParam);
    }
    if (renew) {
      params.add("renew=true");
    }

    String gatewayParam = request.getParameter("gateway");
    boolean gateway = this.gateway;
    if (gatewayParam != null) {
      gateway = Boolean.parseBoolean(gatewayParam);
    }
    if (gateway) {
      params.add("gateway=true");
    }

    params.add("service=" + service);
    String urlToRedirectTo = loginUrl + "?" + StringUtils.join(params, '&');
    return urlToRedirectTo;
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
   * @see org.apachorg.apache.sling.auth.coreicationFeedbackHandler#authenticationSucceeded(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse,
   *      org.apache.sorg.apache.sling.auth.coretionInfo)
   */
  public boolean authenticationSucceeded(HttpServletRequest request,
      HttpServletResponse response, AuthenticationInfo authInfo) {
    LOGGER.debug("authenticationSucceeded called");

    // If the plug-in is intended to verify the existence of a matching Authorizable,
    // check that now.
    if (this.autoCreateUser) {
      boolean isUserValid = findOrCreateUser(authInfo);
      if (!isUserValid) {
        LOGGER.warn("SSO authentication succeeded but corresponding user not found or created");
        try {
          dropCredentials(request, response);
        } catch (IOException e) {
          LOGGER.error("Failed to drop credentials after SSO authentication by invalid user", e);
        }
        return true;
      }
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
    String tryLogin = CasLoginServlet.TRY_LOGIN + "=2";
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

  private boolean findOrCreateUser(AuthenticationInfo authInfo) {
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
        createUser(username, session);
      }
      isUserValid = true;
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
    return request.getParameter(DEFAULT_ARTIFACT_NAME);
  }

  private String retrieveCredentials(String artifact, String responseBody,
      HttpServletRequest request) {
    String username = null;
    String failureCode = null;
    String failureMessage = null;

    try {
      XMLInputFactory xmlInputFactory = new WstxInputFactory();
      xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
      xmlInputFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
      xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
      XMLEventReader eventReader = xmlInputFactory.createXMLEventReader(new StringReader(
          responseBody));

      while (eventReader.hasNext()) {
        XMLEvent event = eventReader.nextEvent();

        // process the event if we're starting an element
        if (event.isStartElement()) {
          StartElement startEl = event.asStartElement();
          QName startElName = startEl.getName();
          String startElLocalName = startElName.getLocalPart();

          /*
           * Example of failure XML
          <cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
            <cas:authenticationFailure code='INVALID_REQUEST'>
              &#039;service&#039; and &#039;ticket&#039; parameters are both required
            </cas:authenticationFailure>
          </cas:serviceResponse>
          */
          if ("authenticationFailure".equalsIgnoreCase(startElLocalName)) {
            // get code of the failure
            Attribute code = startEl.getAttributeByName(QName.valueOf("code"));
            failureCode = code.getValue();

            // get the message of the failure
            event = eventReader.nextEvent();
            assert event.isCharacters();
            Characters chars = event.asCharacters();
            failureMessage = chars.getData();
            break;
          }

          /*
           * Example of success XML
          <cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
            <cas:authenticationSuccess>
              <cas:user>NetID</cas:user>
            </cas:authenticationSuccess>
          </cas:serviceResponse>
          */
          if ("authenticationSuccess".equalsIgnoreCase(startElLocalName)) {
            // skip to the user tag start
            event = eventReader.nextTag();
            assert event.isStartElement();
            startEl = event.asStartElement();
            startElName = startEl.getName();
            startElLocalName = startElName.getLocalPart();
            if (!"user".equals(startElLocalName)) {
              LOGGER.error("Found unexpected element [" + startElName
                  + "] while inside 'authenticationSuccess'");
              break;
            }

            // move on to the body of the user tag
            event = eventReader.nextEvent();
            assert event.isCharacters();
            Characters chars = event.asCharacters();
            username = chars.getData();
            break;
          }
        }
      }
    } catch (XMLStreamException e) {
      LOGGER.error(e.getMessage(), e);
    }

    if (failureCode != null || failureMessage != null) {
      LOGGER.error("Error response from server [code=" + failureCode
          + ", message=" + failureMessage);
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
