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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;
import org.sakaiproject.nakamura.api.auth.trusted.TrustedTokenService;
import org.sakaiproject.nakamura.auth.trusted.TrustedTokenServiceImpl.TrustedUser;
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
 *
 */
@Component(immediate = true)
@Service
public final class TrustedLoginModulePlugin implements LoginModulePlugin {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(TrustedLoginModulePlugin.class);

  /**
   * This holds onto the principal manager connected to the thread after login. Assumes
   * that there is only one session per thread. This is not the case where an administrative login has been 
   * performed, but *hopefully* it wont matter. (thats awful, but until the plugin is fixed there is nothing we can do)
   */
  private ThreadLocal<WeakReference<PrincipalManager>> principalManagerHolder = new ThreadLocal<WeakReference<PrincipalManager>>();

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#doInit(javax.security.auth.callback.CallbackHandler,
   *      javax.jcr.Session, java.util.Map)
   */
  @SuppressWarnings("rawtypes")
  public void doInit(CallbackHandler callbackHandler, Session session, Map options)
      throws LoginException {
    try {
      // at the moment, Sling Login Modules are singletons so we have to assume that
      // doInit will always
      // be called for the current thread and capture anythign we need in a thread local.
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
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#addPrincipals(java.util.Set)
   */
  @SuppressWarnings("rawtypes")
  public void addPrincipals(Set principals) {
    // Since this plugin is a service, how can principals be added. Login modules are not
    // normally services, perhapse this shoud not be one.
    // TODO Auto-generated method stub

  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#canHandle(javax.jcr.Credentials)
   */
  public boolean canHandle(Credentials cred) {
    return TrustedAuthenticationPlugin.canHandle(cred);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#getAuthentication(java.security.Principal,
   *      javax.jcr.Credentials)
   */
  public AuthenticationPlugin getAuthentication(Principal principal, Credentials creds)
      throws RepositoryException {
    try {
      return new TrustedAuthenticationPlugin(principal, creds);
    } catch ( IllegalArgumentException e ) {
      LOGGER.debug("Didnt get authentication {}",e.getMessage(),e);
      return null;      
    }
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
      Object user = sc.getAttribute(TrustedTokenService.CA_AUTHENTICATION_USER);
      if (user instanceof TrustedUser) {
        WeakReference<PrincipalManager> ref = principalManagerHolder.get();
        if (ref != null) {
          PrincipalManager pm = ref.get();
          if (pm != null) {
            principal = pm.getPrincipal(((TrustedUser) user).getUser());
            LOGGER.debug("Got Principal {} which is a ItemBasedPrincipal ? {}  ",principal,(principal instanceof ItemBasedPrincipal));
          } else {
            LOGGER
                .warn("no principal manager available due to Garbage Collection, TrustedLoginModulePlugin, should never happen, please Jira");
          }
        } else {
          LOGGER
              .warn("no session in TrustedLoginModulePlugin, should never happen, please Jira");
        }
        if (principal == null) {
          LOGGER
              .warn("No Principal found via configured managers, defaulting to TrustedPrincipal ");
          principal = new TrustedPrincipal((TrustedUser) user);
        }
      }
    }
    return principal;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#impersonate(java.security.Principal,
   *      javax.jcr.Credentials)
   */
  public int impersonate(Principal principal, Credentials credentials)
      throws RepositoryException, FailedLoginException {
    return LoginModulePlugin.IMPERSONATION_DEFAULT;
  }

}
