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
package org.sakaiproject.nakamura.auth.ldap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.FailedLoginException;

/**
 * The <code>LdapLoginModulePlugin</code> is a LoginModulePlugin which handles
 * <code>SimpleCredentials</code> attributed with the special authentication
 * data provided by the {@link org.sakaiproject.nakamura.auth.ldap.LdapAuthenticationHandler}.
 */
@Component(metatype = true)
@Service
public class LdapLoginModulePlugin implements LoginModulePlugin {
  public static final String USER_FILTER_DEFAULT = "^admin$";
  @Property(value = USER_FILTER_DEFAULT)
  static final String USER_FILTER = "sakai.login.ldap.user.filter";
  private String userFilter;

  @Reference
  private LdapAuthenticationPlugin authPlugin;

  public LdapLoginModulePlugin() {
  }

  LdapLoginModulePlugin(LdapAuthenticationPlugin authPlugin) {
    this.authPlugin = authPlugin;
  }

  @Activate
  protected void activate(Map<?, ?> props) {
    init(props);
  }

  @Modified
  protected void modified(Map<?, ?> props) {
    init(props);
  }

  protected void init(Map<?, ?> props) {
    userFilter = OsgiUtil.toString(props.get(USER_FILTER), USER_FILTER_DEFAULT);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#addPrincipals(java.util.Set)
   */
  @SuppressWarnings("rawtypes")
  public void addPrincipals(Set arg0) {
    // Nothing to do
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#canHandle(javax.jcr.Credentials)
   */
  public boolean canHandle(Credentials cred) {
    boolean canHandle = false;
    if (cred instanceof SimpleCredentials) {
      SimpleCredentials sc = (SimpleCredentials) cred;

      boolean hasUserId = sc.getUserID() != null && sc.getUserID().length() > 0;
      boolean hasPassword = sc.getPassword() != null && sc.getPassword().length > 0;

      if (hasUserId && hasPassword) {
        boolean filtered = sc.getUserID().matches(userFilter);
        if (!filtered) {
          canHandle = true;
        }
      }
    }
    return canHandle;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#doInit(javax.security.auth.callback.CallbackHandler,
   *      javax.jcr.Session, java.util.Map)
   */
  @SuppressWarnings("rawtypes")
  public void doInit(CallbackHandler callbackHandler, Session session, Map options) {
    // nothing to do
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#getAuthentication(java.security.Principal,
   *      javax.jcr.Credentials)
   */
  public AuthenticationPlugin getAuthentication(Principal principal, Credentials creds)
      throws RepositoryException {
    return authPlugin;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#getPrincipal(javax.jcr.Credentials)
   */
  public Principal getPrincipal(Credentials credentials) {
    Principal principal = null;
    if (credentials != null && credentials instanceof SimpleCredentials) {
      final SimpleCredentials sc = (SimpleCredentials) credentials;

      principal = new Principal() {
        public String getName() {
          return sc.getUserID();
        }
      };

    }
    return principal;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#impersonate(java.security.Principal,
   *      javax.jcr.Credentials)
   */
  public int impersonate(Principal principal, Credentials credentials) throws RepositoryException,
      FailedLoginException {
    return LoginModulePlugin.IMPERSONATION_DEFAULT;
  }
}
