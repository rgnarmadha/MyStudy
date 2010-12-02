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

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchResults;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.auth.core.spi.AuthenticationFeedbackHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.ModificationType;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManager;
import org.sakaiproject.nakamura.api.ldap.LdapUtil;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Authentication feedback handler for provisioning a JCR system user after successful
 * authentication processing. Attempt to create user is only tried if no
 * {@link Authorizable} is found for the provided username.
 */
// @Component(metatype = true)
// @Service
public class LdapAuthenticationFeedbackHandler implements AuthenticationFeedbackHandler {
  private static final Logger logger = LoggerFactory
      .getLogger(LdapAuthenticationFeedbackHandler.class);

  @Reference
  private AuthorizablePostProcessService authorizablePostProcessService;;

  @Reference
  private SlingRepository slingRepository;

  @Reference
  private LdapConnectionManager connMgr;

  @Property(value = "o=sakai")
  static final String LDAP_BASE_DN = "sakai.auth.ldap.fb.baseDn";
  private String baseDn;

  @Property(value = "uid={}")
  static final String USER_FILTER = "sakai.auth.ldap.fb.filter.user";
  private String userFilter;

  public static final boolean DECORATE_USER_DEFAULT = true;
  @Property(boolValue = DECORATE_USER_DEFAULT)
  static final String DECORATE_USER = "sakai.auth.ldap.fb.user.decorate";
  private boolean decorateUser;

  public static final String FIRST_NAME_PROP_DEFAULT = "firstName";
  @Property(value = FIRST_NAME_PROP_DEFAULT)
  static final String FIRST_NAME_PROP = "sakai.auth.ldap.fb.prop.firstName";
  private String firstNameProp;

  public static final String LAST_NAME_PROP_DEFAULT = "lastName";
  @Property(value = LAST_NAME_PROP_DEFAULT)
  static final String LAST_NAME_PROP = "sakai.auth.ldap.fb.prop.lastName";
  private String lastNameProp;

  public static final String EMAIL_PROP_DEFAULT = "email";
  @Property(value = EMAIL_PROP_DEFAULT)
  static final String EMAIL_PROP = "sakai.auth.ldap.fb.prop.email";
  private String emailProp;

  @Activate
  protected void activate(Map<?, ?> props) {
    init(props);
  }

  @Modified
  protected void modified(Map<?, ?> props) {
    init(props);
  }

  private void init(Map<?, ?> props) {
    baseDn = OsgiUtil.toString(props.get(LDAP_BASE_DN), "");
    userFilter = OsgiUtil.toString(props.get(USER_FILTER), "");
    decorateUser = OsgiUtil.toBoolean(props.get(DECORATE_USER), DECORATE_USER_DEFAULT);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.auth.core.spi.AuthenticationFeedbackHandler#authenticationFailed(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse,
   *      org.apache.sling.auth.core.spi.AuthenticationInfo)
   */
  public void authenticationFailed(HttpServletRequest arg0, HttpServletResponse arg1,
      AuthenticationInfo arg2) {
    // Nothing for us to do.
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.auth.core.spi.AuthenticationFeedbackHandler#authenticationSucceeded(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse,
   *      org.apache.sling.auth.core.spi.AuthenticationInfo)
   */
  public boolean authenticationSucceeded(HttpServletRequest req,
      HttpServletResponse resp, AuthenticationInfo authInfo) {
    try {
      Session session = slingRepository.loginAdministrative(null);

      UserManager um = AccessControlUtil.getUserManager(session);
      Authorizable auth = um.getAuthorizable(authInfo.getUser());

      if (auth == null) {
        String password = RandomStringUtils.random(8);
        User user = um.createUser(authInfo.getUser(), password);

        if (decorateUser) {
          decorateUser(session, user);
        }

        // TODO To properly set up personal profiles, non-persisted data from
        // LDAP may need to be forwarded to post-processing.
        authorizablePostProcessService.process(user, session, ModificationType.CREATE);
      }
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
    }
    return false;
  }

  /**
   * Decorate the user with extra information.
   *
   * @param session
   * @param user
   */
  private void decorateUser(Session session, User user) {
    try {
      // fix up the user dn to search
      String userDn = LdapUtil.escapeLDAPSearchFilter(userFilter.replace("{}",
          user.getID()));

      // get a connection to LDAP
      LDAPConnection conn = connMgr.getBoundConnection(null, null);
      LDAPSearchResults results = conn.search(baseDn, LDAPConnection.SCOPE_SUB, userDn,
          new String[] { firstNameProp, lastNameProp, emailProp }, true);
      if (results.hasMore()) {
        LDAPEntry entry = results.next();
        ValueFactory vf = session.getValueFactory();
        user.setProperty("firstName",
            vf.createValue(entry.getAttribute(firstNameProp).toString()));
        user.setProperty("lastName",
            vf.createValue(entry.getAttribute(lastNameProp).toString()));
        user.setProperty("email",
            vf.createValue(entry.getAttribute(emailProp).toString()));
      } else {
        logger.warn("Can't find user [" + userDn + "]");
      }
    } catch (LDAPException e) {
      logger.warn(e.getMessage(), e);
    } catch (RepositoryException e) {
      logger.warn(e.getMessage(), e);
    }
  }
}
