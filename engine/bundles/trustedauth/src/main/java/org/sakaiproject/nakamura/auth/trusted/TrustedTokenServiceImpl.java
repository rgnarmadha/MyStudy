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
package org.sakaiproject.nakamura.auth.trusted;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.auth.trusted.TrustedTokenService;
import org.sakaiproject.nakamura.api.cluster.ClusterTrackingService;
import org.sakaiproject.nakamura.api.memory.CacheManagerService;
import org.sakaiproject.nakamura.api.servlet.HttpOnlyCookie;
import org.sakaiproject.nakamura.auth.trusted.TokenStore.SecureCookie;
import org.sakaiproject.nakamura.auth.trusted.TokenStore.SecureCookieException;
import org.sakaiproject.nakamura.util.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 */
@Component(immediate = true, metatype = true)
@Service
public final class TrustedTokenServiceImpl implements TrustedTokenService {




  private static final Logger LOG = LoggerFactory.getLogger(TrustedTokenServiceImpl.class);

  /** Property to invalidate if the session should be used. */
  @Property(boolValue = false)
  public static final String USE_SESSION = "sakai.auth.trusted.token.usesession";

  /** Property to indicate if only cookies should be secure */
  @Property(boolValue = false)
  public static final String SECURE_COOKIE = "sakai.auth.trusted.token.securecookie";

  /** Property to indicate the TTL on cookies */
  @Property(longValue = 1200000)
  public static final String TTL = "sakai.auth.trusted.token.ttl";

  /** Property to indicate the name of the cookie. */
  @Property(value = "sakai-trusted-authn")
  public static final String COOKIE_NAME = "sakai.auth.trusted.token.name";

  /** Property to point to keystore file */
  @Property(value = "sling/cookie-keystore.bin")
  public static final String TOKEN_FILE_NAME = "sakai.auth.trusted.token.storefile";

  /** Property to contain the shared secret used by all trusted servers */
  @Property(value = "default-setting-change-before-use")
  public static final String SERVER_TOKEN_SHARED_SECRET = "sakai.auth.trusted.server.secret";

  /** True if server tokens are enabled. */
  @Property(boolValue=true)
  public static final String SERVER_TOKEN_ENABLED = "sakai.auth.trusted.server.enabled";

  /** A list of all the known safe hosts to trust as servers */
  @Property(value ="localhost;127.0.0.1;0:0:0:0:0:0:0:1%0")
  public static final String SERVER_TOKEN_SAFE_HOSTS_ADDR = "sakai.auth.trusted.server.safe-hostsaddress";

  private static final String DEFAULT_WRAPPERS = "org.sakaiproject.nakamura.formauth.FormAuthenticationTokenServiceWrapper;org.sakaiproject.nakamura.opensso.OpenSsoAuthenticationTokenServiceWrapper;org.sakaiproject.nakamura.auth.opensso.OpenSsoAuthenticationTokenServiceWrapper;org.sakaiproject.nakamura.auth.cas.CasAuthenticationTokenServiceWrapper";
  @Property(value = DEFAULT_WRAPPERS)
  public static final String SERVER_TOKEN_SAFE_WRAPPERS = "sakai.auth.trusted.wrapper.class.names";

  @Property(value="")
  public static final String TRUSTED_HEADER_NAME = "sakai.auth.trusted.header";

  @Property(value="")
  public static final String TRUSTED_PARAMETER_NAME = "sakai.auth.trusted.request-parameter";

  /** A list of all the known safe hosts to trust for authentication purposes, ie front end proxies */
  @Property(value ="")
  public static final String TRUSTED_PROXY_SERVER_ADDR = "sakai.auth.trusted.server.safe-authentication-addresses";

  @Property(boolValue=false )
  public static final String DEBUG_COOKIES = "sakai.auth.trusted.token.debugcookies";

  /**
   * the name of the header to be trusted, if null or "" then don't trust headers.
   */
  private String trustedHeaderName;

  /**
   * the name of the parameter to be trusted, if null or "" then don't trust request parameters.
   */
  private String trustedParameterName;

  /**
   * set of trusted IP address to use as proxies.
   */
  private Set<String> trustedProxyServerAddrSet = new HashSet<String>(5);


  /**
   * If True, sessions will be used, if false cookies.
   */
  private boolean usingSession = false;

  /**
   * Should the cookies go over ssl.
   */
  private boolean secureCookie = false;

  /**
   * The name of the authN token.
   */
  private String trustedAuthCookieName;


  /**
   * An optional cookie server can be used in a cluster to centralize the management of
   * authN tokens. This is for situations where session storage and replication is not
   * desired, and session affinity can't be tolerated. Without this clients must come back
   * to the same host where they were authenticated as the cookie encode decode has
   * entropy associated with the instance of the server they are operating on.
   */
  @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY)
  private ClusterCookieServer clusterCookieServer;

  private TokenStore tokenStore;

  private Long ttl;


  @Reference
  protected ClusterTrackingService clusterTrackingService;

  @Reference
  protected CacheManagerService cacheManager;

  @Reference
  protected EventAdmin eventAdmin;

  /**
   * If this is true the implementation is in test mode to enable external components to
   * test, without compromising the protection of the class.
   */
  private boolean testing = false;

  /**
   * Contains the calls made during testing.
   */
  private ArrayList<Object[]> calls;

  private String sharedSecret;

  private boolean trustedTokenEnabled;

  private Set<String> safeHostAddrSet = new HashSet<String>(16); // 16 way cluster is about as big as we will get.

  private String[] safeWrappers;

  private boolean debugCookies;

  /**
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeyException
   * @throws UnsupportedEncodingException
   * @throws IllegalStateException
   *
   */
  public TrustedTokenServiceImpl() throws NoSuchAlgorithmException, InvalidKeyException,
      IllegalStateException, UnsupportedEncodingException {
      tokenStore = new TokenStore();
    
  }


  @SuppressWarnings("rawtypes")
  protected void activate(ComponentContext context) {
    Dictionary props = context.getProperties();
    usingSession = (Boolean) props.get(USE_SESSION);
    secureCookie = (Boolean) props.get(SECURE_COOKIE);
    ttl = (Long) props.get(TTL);
    trustedAuthCookieName = (String) props.get(COOKIE_NAME);
    sharedSecret = (String) props.get(SERVER_TOKEN_SHARED_SECRET);
    trustedTokenEnabled = (Boolean) props.get(SERVER_TOKEN_ENABLED);
    debugCookies = (Boolean) props.get(DEBUG_COOKIES);
    tokenStore.setDebugCookies(debugCookies);
    String safeHostsAddr = OsgiUtil.toString(props.get(SERVER_TOKEN_SAFE_HOSTS_ADDR), "");
    safeHostAddrSet.clear();
    if ( safeHostsAddr != null) {
      for ( String address : StringUtils.split(safeHostsAddr,';')) {
        safeHostAddrSet.add(address);
      }
    }
    String trustedProxyServerAddr = OsgiUtil.toString(props.get(TRUSTED_PROXY_SERVER_ADDR), "");
    trustedProxyServerAddrSet.clear();
    if ( trustedProxyServerAddr != null) {
      for ( String address : StringUtils.split(trustedProxyServerAddr,';')) {
        trustedProxyServerAddrSet.add(address);
      }
    }
    String wrappers = (String)props.get(SERVER_TOKEN_SAFE_WRAPPERS);
    if ( wrappers == null || wrappers.length() == 0 ) {
      wrappers = DEFAULT_WRAPPERS;
    }
    safeWrappers = StringUtils.split(wrappers, ";");

    String tokenFile = (String) props.get(TOKEN_FILE_NAME);
    String serverId = clusterTrackingService.getCurrentServerId();
    tokenStore.doInit(cacheManager, tokenFile, serverId, ttl);

    trustedHeaderName = OsgiUtil.toString(props.get(TRUSTED_HEADER_NAME), "");
    trustedParameterName = OsgiUtil.toString(props.get(TRUSTED_PARAMETER_NAME), "");
  }

  public void activateForTesting() {
    testing = true;
    calls = new ArrayList<Object[]>();
    safeWrappers = StringUtils.split(DEFAULT_WRAPPERS,";");
  }

  /**
   * @return the calls used in testing.
   */
  public ArrayList<Object[]> getCalls() {
    return calls;
  }

  /**
   * Extract credentials from the request.
   *
   * @param req
   * @return credentials associated with the request.
   */
  public Credentials getCredentials(HttpServletRequest req, HttpServletResponse response) {
    if (testing) {
      calls.add(new Object[] { "getCredentials", req, response });
      return new SimpleCredentials("testing", "testing".toCharArray());
    }
    Credentials cred = null;
    String userId = null;
    String sakaiTrustedHeader = req.getHeader("x-sakai-token");
    if (trustedTokenEnabled && sakaiTrustedHeader != null
        && sakaiTrustedHeader.trim().length() > 0) {
      String host = req.getRemoteAddr();
      if (!safeHostAddrSet.contains(host)) {
        LOG.warn("Ignoring Trusted Token request from {} ", host);
      } else {
        // we have a HMAC based token, we should see if it is valid against the key we
        // have
        // and if so create some credentials.
        String[] parts = sakaiTrustedHeader.split(";");
        if (parts.length == 3) {
          try {
            String hash = parts[0];
            String user = parts[1];
            String timestamp = parts[2];
            String hmac = Signature.calculateRFC2104HMAC(
                user + ";" + timestamp, sharedSecret);
            if (hmac.equals(hash)) {
              // the user is Ok, we will trust it.
              userId = user;
              cred = createCredentials(userId);
            } else {
              LOG.debug("HMAC Match Failed {} != {} ", hmac, hash );
            }
          } catch (SignatureException e) {
            LOG.warn("Failed to validate server token : {} {} ", sakaiTrustedHeader, e
                .getMessage());
          }
        } else {
          LOG.warn("Illegal number of elements in trusted server token:{} {}  ",
              sakaiTrustedHeader, parts.length);
        }
      }
    }
    if (userId == null) {
      if (usingSession) {
        HttpSession session = req.getSession(false);
        if (session != null) {
          Credentials testCredentials = (Credentials) session
              .getAttribute(SA_AUTHENTICATION_CREDENTIALS);
          if (testCredentials instanceof SimpleCredentials) {
            SimpleCredentials sc = (SimpleCredentials) testCredentials;
            Object o = sc.getAttribute(CA_AUTHENTICATION_USER);
            if (o instanceof TrustedUser) {
              TrustedUser tu = (TrustedUser) o;
              if (tu.getUser() != null) {
                userId = tu.getUser();
                cred = testCredentials;
              }
            }
          }
        } else {
          cred = null;
        }
      } else {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
          for (Cookie c : cookies) {
            if (trustedAuthCookieName.equals(c.getName())) {
              if (secureCookie && !c.getSecure()) {
                continue;
              }
              String cookieValue = c.getValue();
              userId = decodeCookie(c.getValue());
              if (userId != null) {
                LOG.debug("Token is valid and decoded to {} ",userId);
                cred = createCredentials(userId);
                refreshToken(response, c.getValue(), userId);
                break;
              } else {
                LOG.debug("Invalid Cookie {} ", cookieValue);
                clearCookie(response);
              }
            }
          }
        }
      }
    }
    if (userId != null) {
      LOG.debug("Trusted Authentication for {} with credentials {}  ", userId, cred);
    }

    return cred;
  }

  /**
   * Remove credentials so that subsequent request don't contain credentials.
   *
   * @param request
   * @param response
   */
  public void dropCredentials(HttpServletRequest request, HttpServletResponse response) {
    if ( testing ) {
      calls.add(new Object[]{"dropCredentials",request,response});
      return;
    }
    if (usingSession) {
      HttpSession session = request.getSession(false);
      if (session != null) {
        session.setAttribute(SA_AUTHENTICATION_CREDENTIALS, null);
      }
    } else {
      clearCookie(response);
    }
  }

  /**
   * Inject a token into the request/response, this assumes htat the getUserPrincipal() of the request
   * or the request.getRemoteUser() contain valid user ID's from which to generate the request.
   *
   *
   * @param req
   * @param resp
   */
  public void injectToken(HttpServletRequest request, HttpServletResponse response) {
    if ( testing ) {
      calls.add(new Object[]{"injectToken",request,response});
      return;
    }
    String userId = null;
    String remoteAddress = request.getRemoteAddr();
    if (trustedProxyServerAddrSet.contains(remoteAddress)) {
      if (trustedHeaderName.length() > 0) {
        userId = request.getHeader(trustedHeaderName);
        if (userId != null) {
          LOG.debug(
              "Injecting Trusted Token from request: Header [{}] indicated user was [{}] ",
              trustedHeaderName, userId);
        }
      }
      if (userId == null && trustedParameterName.length() > 0) {
        userId = request.getParameter(trustedParameterName);
        if (userId != null) {
          LOG.debug(
              "Injecting Trusted Token from request: Parameter [{}] indicated user was [{}] ",
              trustedParameterName, userId);
        }
      }
    }
    if (userId == null) {
      Principal p = request.getUserPrincipal();
      if (p != null) {
        userId = p.getName();
        if (userId != null) {
          LOG.debug(
              "Injecting Trusted Token from request: User Principal indicated user was [{}] ",
              userId);
        }
      }
    }
    if (userId == null) {
      userId = request.getRemoteUser();
      if ( userId != null ) {
        LOG.debug("Injecting Trusted Token from request: Remote User indicated user was [{}] ", userId);
      }
    }


    if (userId != null) {
      if (usingSession) {
        HttpSession session = request.getSession(true);
        if (session != null) {
          LOG.debug("Injecting Credentials into Session for " + userId);
          session.setAttribute(SA_AUTHENTICATION_CREDENTIALS, createCredentials(userId));
        }
      } else {
        addCookie(response, userId);
      }
      Dictionary<String, Object> eventDictionary = new Hashtable<String, Object>();
      eventDictionary.put(TrustedTokenService.EVENT_USER_ID, userId);

      // send an async event to indicate that the user has been trusted, things that want to create users can hook into this.
      eventAdmin.sendEvent(new Event(TrustedTokenService.TRUST_USER_TOPIC,eventDictionary));
    } else {
      LOG.warn("Unable to inject token; unable to determine user from request.");
    }
  }

  /**
   * @param userId
   * @param response
   */
  void addCookie(HttpServletResponse response, String userId) {
    Cookie c = new HttpOnlyCookie(trustedAuthCookieName, encodeCookie(userId));
    c.setMaxAge(-1);
    c.setPath("/");
    c.setSecure(secureCookie);
    response.addCookie(c);
    // rfc 2109 section 4.5. stop http 1.1 caches caching the response
    response.addHeader("Cache-Control", "no-cache=\"set-cookie\" ");
    // and stop http 1.0 caches caching the response
    response.addDateHeader("Expires", 0);
  }

  /**
   * @param response
   */
  void clearCookie(HttpServletResponse response) {
    Cookie c = new HttpOnlyCookie(trustedAuthCookieName, "");
    c.setMaxAge(0);
    c.setPath("/");
    c.setSecure(secureCookie);
    response.addCookie(c);
  }

  /**
   * Refresh the token, assumes that the cookie is valid.
   *
   * @param req
   * @param value
   * @param userId
   */
  void refreshToken(HttpServletResponse response, String value, String userId) {
    String[] parts = StringUtils.split(value, "@");
    if (parts != null && parts.length == 4) {
      long cookieTime = Long.parseLong(parts[1].substring(1));
      if (System.currentTimeMillis() + (ttl / 2) > cookieTime) {
        if ( debugCookies) {
          LOG.info("Refreshing Token for {} cookieTime {} ttl {} CurrentTime {} ",new Object[]{userId, cookieTime, ttl, System.currentTimeMillis()});
        }
        addCookie(response, userId);
      }
    }

  }

  /**
   * Encode the user ID in a secure cookie.
   *
   * @param userId
   * @return
   */
  String encodeCookie(String userId) {
    if (userId == null) {
      return null;
    }
    if (clusterCookieServer != null) {
      return clusterCookieServer.encodeCookie(userId);
    } else {
      long expires = System.currentTimeMillis() + ttl;
      SecureCookie secretKeyHolder = tokenStore.getActiveToken();

      try {
        return secretKeyHolder.encode(expires, userId);
      } catch (NoSuchAlgorithmException e) {
        LOG.error(e.getMessage(), e);
      } catch (InvalidKeyException e) {
        LOG.error(e.getMessage(), e);
      } catch (IllegalStateException e) {
        LOG.error(e.getMessage(), e);
      } catch (UnsupportedEncodingException e) {
        LOG.error(e.getMessage(), e);
      } catch (SecureCookieException e) {
        LOG.error(e.getMessage(), e);
      }
      return null;
    }
  }

  /**
   * Decode the user ID.
   *
   * @param value
   * @return
   */
  String decodeCookie(String value) {
    if (value == null) {
      return null;
    }
    if (clusterCookieServer != null) {
      return clusterCookieServer.decodeCookie(value);
    } else {
      try {
        SecureCookie secureCookie = tokenStore.getSecureCookie();
        return secureCookie.decode(value);
      } catch (SecureCookieException e) {
        LOG.error(e.getMessage());
      }
    }
    return null;

  }


  /**
   * Create credentials from a validated userId.
   *
   * @param req
   *          The request to sniff for a user.
   * @return
   */
  private Credentials createCredentials(String userId) {
    SimpleCredentials sc = new SimpleCredentials(userId, new char[0]);
    TrustedUser user = new TrustedUser(userId);
    sc.setAttribute(CA_AUTHENTICATION_USER, user);
    return sc;
  }

  /**
   * "Trusted" inner class for passing the user on to the authentication handler.<br/>
   * <br/>
   * By being a static, inner class with a private constructor, it is harder for an
   * external source to inject into the authentication chain.
   */
  static final class TrustedUser {
    private final String user;

    /**
     * Constructor.
     *
     * @param user
     *          The user to represent.
     */
    private TrustedUser(String user) {
      this.user = user;
    }

    /**
     * Get the user that is being represented.
     *
     * @return The represented user.
     */
    String getUser() {
      return user;
    }
  }

  /**
   * @return
   */
  public String[] getAuthorizedWrappers() {
    return safeWrappers;
  }

}
