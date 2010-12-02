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

import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.security.Principal;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

/**
 * An abstract login module containing support for custom login module plugins.
 */
public abstract class AbstractLoginModule implements LoginModulePlugin {

  /**
   * A protected internal Authentication Plugin class that binds to a supplied
   * Authentication object.
   */
  final class InternalAuthenticationPlugin implements AuthenticationPlugin {
    private final Principal principal;

    /**
     * Create the plugin, provided the credentials are suitable for this plugin, ie they
     * contain a authentication object attribute.
     * 
     * @param principal
     * @param creds
     */
    public InternalAuthenticationPlugin(Principal principal, Credentials creds) {
      if (canHandle(creds)) {
        this.principal = principal;
        return;
      }
      throw new IllegalArgumentException("Creadentials are not trusted ");
    }

    /**
     * 
     * @param cred
     * @return true if the credentials contain an authentication object of the correct
     *         type.
     */
    public boolean canHandle(Credentials cred) {
      boolean hasAttribute = false;
      if (cred != null && cred instanceof SimpleCredentials) {
        Object attr = ((SimpleCredentials) cred)
            .getAttribute(AbstractAuthenticationHandler.AUTHENTICATION_OBJECT);
        hasAttribute = isAuthenticationValid(attr);
      }
      return hasAttribute;
    }

    /**
     * {@inheritDoc}
     * Authenticate by checking that the principals match the credentials. 
     * @see org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin#authenticate(javax.jcr.Credentials)
     */
    public boolean authenticate(Credentials credentials) throws RepositoryException {
      boolean auth = false;
      if (canHandle(credentials)) {
        SimpleCredentials sc = (SimpleCredentials) credentials;
        if (principal.getName().equals(sc.getUserID())) {
          auth = true;
        }
      }
      return auth;
    }
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractLoginModule.class);

  /**
   * This holds onto the principal manager connected to the thread after login. Assumes
   * that there is only one session per thread. This is not the case where an
   * administrative login has been performed, but *hopefully* it wont matter. (thats
   * awful, but until the plugin is fixed there is nothing we can do)
   */
  private ThreadLocal<WeakReference<PrincipalManager>> principalManagerHolder = new ThreadLocal<WeakReference<PrincipalManager>>();

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#getAuthentication(java.security.Principal,
   *      javax.jcr.Credentials)
   */
  public AuthenticationPlugin getAuthentication(Principal principal, Credentials creds)
      throws RepositoryException {
    try {
      return new InternalAuthenticationPlugin(principal, creds);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#addPrincipals(java.util.Set)
   */
  @SuppressWarnings("unchecked")
  public void addPrincipals(Set arg0) {
    // Since this plugin is a service, how can principals be added. Login modules are not
    // normally services, perhaps this should not be one.
    // TODO Auto-generated method stub
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#canHandle(javax.jcr.Credentials)
   */
  public boolean canHandle(Credentials credentials) {
    if (credentials != null && credentials instanceof SimpleCredentials) {
      SimpleCredentials sc = (SimpleCredentials) credentials;
      Object authObj = sc
          .getAttribute(AbstractAuthenticationHandler.AUTHENTICATION_OBJECT);
      if (isAuthenticationValid(authObj)) {
        return true;
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#doInit(javax.security.auth.callback.CallbackHandler,
   *      javax.jcr.Session, java.util.Map)
   */
  @SuppressWarnings("unchecked")
  public void doInit(CallbackHandler cbHandler, Session session, Map props)
      throws LoginException {
    try {
      // at the moment, Sling Login Modules are singletons so we have to assume that
      // doInit will always
      // be called for the current thread and capture anything we need in a thread local.
      // Unfortunately there is no doDestroy, so we have to use a WeakReference to ensure
      // that
      // the thread local cache does not prevent the session from being closed.
      WeakReference<PrincipalManager> userManagerRef = new WeakReference<PrincipalManager>(
          AccessControlUtil.getPrincipalManager(session));
      principalManagerHolder.set(userManagerRef);
    } catch (Exception e) {
      throw new LoginException("Login Failed due to seconday exception " + e.getMessage());
    }
    return;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#getPrincipal(javax.jcr.Credentials)
   */
  public Principal getPrincipal(Credentials credentials) {
    Principal principal = null;
    if (credentials != null && credentials instanceof SimpleCredentials) {
      SimpleCredentials sc = (SimpleCredentials) credentials;
      Object authObj = sc
          .getAttribute(AbstractAuthenticationHandler.AUTHENTICATION_OBJECT);
      if (isAuthenticationValid(authObj)) {
        WeakReference<PrincipalManager> ref = principalManagerHolder.get();
        if (ref != null) {
          PrincipalManager pm = ref.get();
          if (pm != null) {
            principal = pm.getPrincipal(((AbstractAuthentication) authObj).getUserId());
            LOGGER.info("Got Principal {} which is a ItemBasedPrincipal ? {}  ",
                principal, (principal instanceof ItemBasedPrincipal));
          } else {
            LOGGER
                .warn("no principal manager available due to Garbage Collection, AbstractLoginModulePlugin, should never happen, please Jira");
          }
        } else {
          LOGGER
              .warn("no session in AbstractLoginModulePlugin, should never happen, please Jira");
        }
        if (principal == null) {
          LOGGER
              .warn("No Principal found via configured managers, defaulting to OpenSsoPrincipal ");
          principal = new UnknownAuthenticatedPrincipal((AbstractAuthentication) authObj);
        }
      }
    }
    return principal;
  }

  /**
   * @param authObj
   * @return true if the authObj is a valid authentication object.
   */
  protected abstract boolean isAuthenticationValid(Object authObj);

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#impersonate(java.security.Principal,
   *      javax.jcr.Credentials)
   */
  public int impersonate(Principal arg0, Credentials arg1) throws RepositoryException,
      FailedLoginException {
    return LoginModulePlugin.IMPERSONATION_DEFAULT;
  }

}
