/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.nakamura.user.servlet;


import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jackrabbit.usermanager.impl.post.AbstractUserPostServlet;
import org.apache.sling.jackrabbit.usermanager.impl.resource.AuthorizableResourceProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.impl.helper.RequestProperty;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.auth.trusted.RequestTrustValidator;
import org.sakaiproject.nakamura.api.auth.trusted.RequestTrustValidatorService;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.user.NameSanitizer;
import org.sakaiproject.nakamura.util.osgi.EventUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * Sling Post Servlet implementation for creating a user in the jackrabbit UserManager.
 * </p>
 * <h2>Rest Service Description</h2>
 * <p>
 * Creates a new user. Maps on to nodes of resourceType <code>sling/users</code> like
 * <code>/rep:system/rep:userManager/rep:users</code> mapped to a resource url
 * <code>/system/userManager/user</code>. This servlet responds at <code>/system/userManager/user.create.html</code>
 * </p>
 * <h4>Methods</h4>
 * <ul>
 * <li>POST</li>
 * </ul>
 * <h4>Post Parameters</h4>
 * <dl>
 * <dt>:name</dt>
 * <dd>The name of the new user (required)</dd>
 * <dt>:pwd</dt>
 * <dd>The password of the new user (required)</dd>
 * <dt>:pwdConfirm</dt>
 * <dd>The password of the new user (required)</dd>
 * <dt>*</dt>
 * <dd>Any additional parameters become properties of the user node (optional)</dd>
 * </dl>
 * <h4>Response</h4>
 * <dl>
 * <dt>200</dt>
 * <dd>Success, a redirect is sent to the users resource locator. The redirect comes with
 * HTML describing the status.</dd>
 * <dt>500</dt>
 * <dd>Failure, including user already exists. HTML explains the failure.</dd>
 * </dl>
 * <h4>Example</h4>
 *
 * <code>
 * curl -F:name=ieb -Fpwd=password -FpwdConfirm=password -Fproperty1=value1 http://localhost:8080/system/userManager/user.create.html
 * </code>
 *
 *
 * @scr.component immediate="true" label="%createUser.post.operation.name"
 *                description="%createUser.post.operation.description"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sling/users"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.selectors" value="create"
 *
 * @scr.property name="password.digest.algorithm" value="sha1"
 *
 *
 * @scr.property name="servlet.post.dateFormats"
 *               values.0="EEE MMM dd yyyy HH:mm:ss 'GMT'Z"
 *               values.1="yyyy-MM-dd'T'HH:mm:ss.SSSZ" values.2="yyyy-MM-dd'T'HH:mm:ss"
 *               values.3="yyyy-MM-dd" values.4="dd.MM.yyyy HH:mm:ss"
 *               values.5="dd.MM.yyyy"
 *
 *
 * @scr.property name="self.registration.enabled" label="%self.registration.enabled.name"
 *               description="%self.registration.enabled.description"
 *               valueRef="DEFAULT_SELF_REGISTRATION_ENABLED"
 *
 */

@ServiceDocumentation(name="Create User Servlet",
    description="Creates a new user. Maps on to nodes of resourceType sling/users like " +
    		"/rep:system/rep:userManager/rep:users mapped to a resource url /system/userManager/user. " +
    		"This servlet responds at /system/userManager/user.create.html",
    shortDescription="Creates a new user",
    bindings=@ServiceBinding(type=BindingType.PATH,bindings="/system/userManager",
        selectors={
        @ServiceSelector(name="create",description="binds to this servlet for user creation")
    },
    extensions={
        @ServiceExtension(name="html", description="All post operations produce HTML")
    }),

    methods=@ServiceMethod(name="POST",
        description={"Creates a new user with a name :name, and password pwd, " +
        		"storing additional parameters as properties of the new user.",
        		"Example<br><pre>curl -F:name=username -Fpwd=password -FpwdConfirm=password " +
        		"-Fproperty1=value1 http://localhost:8080/system/userManager/user.create.html</pre>"},
        parameters={
        @ServiceParameter(name=":name", description="The name of the new user (required)"),
        @ServiceParameter(name="pwd", description="The password of the new user (required)"),
        @ServiceParameter(name="pwdConfirm", description="The password of the new user (required)"),
        @ServiceParameter(name="",description="Additional parameters become user node properties, " +
        		"except for parameters starting with ':', which are only forwarded to post-processors (optional)"),
        @ServiceParameter(name=":create-auth", description="The name of a per request authentication " +
        		"mechanism eg capatcha, callers will also need to add parameters to satisfy the " +
        		"authentication method,  (optional)")
    },
    response={
    @ServiceResponse(code=200,description="Success, a redirect is sent to the users resource locator with HTML describing status."),
    @ServiceResponse(code=400,description="Failure, when you try to create a user with a username that already exists."),
    @ServiceResponse(code=500,description="Failure, HTML explains failure.")
        }))

public class CreateSakaiUserServlet extends AbstractUserPostServlet  {

    /**
     *
     */
    private static final long serialVersionUID = -5060795742204221361L;

    /**
     * default log
     */
    private static final Logger log = LoggerFactory.getLogger(CreateSakaiUserServlet.class);

    private static final String PROP_SELF_REGISTRATION_ENABLED = "self.registration.enabled";

    private static final Boolean DEFAULT_SELF_REGISTRATION_ENABLED = Boolean.TRUE;

    private Boolean selfRegistrationEnabled = DEFAULT_SELF_REGISTRATION_ENABLED;

    /**
     * The JCR Repository we access to resolve resources
     *
     * @scr.reference
     */
    private transient SlingRepository repository;

    /**
     * Used to launch OSGi events.
     *
     * @scr.reference
     */
    protected transient EventAdmin eventAdmin;

    /**
     * Used to post process authorizable creation request.
     *
     * @scr.reference
     */
    private transient AuthorizablePostProcessService postProcessorService;

    private String adminUserId = null;

    private Object lock = new Object();

    /**
     *
     * @scr.reference
     */
    protected transient RequestTrustValidatorService requestTrustValidatorService;

    /** Returns the JCR repository used by this service. */
    @SuppressWarnings(justification="OSGi Managed", value={"UWF_UNWRITTEN_FIELD"})
    protected SlingRepository getRepository() {
        return repository;
    }

    /**
     * Returns an administrative session to the default workspace.
     */
    private Session getSession() throws RepositoryException {
        return getRepository().loginAdministrative(null);
    }

    /**
     * Return the administrative session and close it.
     */
    private void ungetSession(final Session session) {
        if (session != null) {
            try {
                session.logout();
            } catch (Throwable t) {
                log.error("Unable to log out of session: " + t.getMessage(), t);
            }
        }
    }

    // ---------- SCR integration ---------------------------------------------

    /**
     * Activates this component.
     *
     * @param componentContext The OSGi <code>ComponentContext</code> of this
     *            component.
     */
    @Override
    protected void activate(ComponentContext componentContext) {
        super.activate(componentContext);
        Dictionary<?, ?> props = componentContext.getProperties();
        Object propValue = props.get(PROP_SELF_REGISTRATION_ENABLED);
        if (propValue instanceof String) {
            selfRegistrationEnabled = Boolean.parseBoolean((String) propValue);
        } else {
            selfRegistrationEnabled = DEFAULT_SELF_REGISTRATION_ENABLED;
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.sling.jackrabbit.usermanager.post.AbstractAuthorizablePostServlet
     * #handleOperation(org.apache.sling.api.SlingHttpServletRequest,
     * org.apache.sling.api.servlets.HtmlResponse, java.util.List)
     */
    @Override
    protected void handleOperation(SlingHttpServletRequest request,
            HtmlResponse response, List<Modification> changes)
            throws RepositoryException {

        // check for an administrator
        boolean administrator = false;
        try {
            Session currentSession = request.getResourceResolver().adaptTo(Session.class);
            if ( adminUserId == null ) {
              synchronized (lock) {
                UserManager um = AccessControlUtil.getUserManager(currentSession);
                User currentUser = (User) um.getAuthorizable(currentSession.getUserID());
                administrator = currentUser.isAdmin();
                if ( administrator ) {
                  adminUserId = currentUser.getID();
                }
              }
            } else {
              administrator = adminUserId.equals(currentSession.getUserID());
            }
          } catch ( Exception ex ) {
            log.warn("Failed to determin if the user is an admin, assuming not. Cause: "+ex.getMessage());
            administrator = false;
          }
          if (!administrator) {
            if (!selfRegistrationEnabled) {
              throw new RepositoryException(
                  "Sorry, registration of new users is not currently enabled. Please try again later.");
            }

            boolean trustedRequest = false;
            String trustMechanism = request.getParameter(":create-auth");
            if (trustMechanism != null) {
              RequestTrustValidator validator = requestTrustValidatorService
                  .getValidator(trustMechanism);
              if (validator != null
                  && validator.getLevel() >= RequestTrustValidator.CREATE_USER
                  && validator.isTrusted(request)) {
                trustedRequest = true;
              }
            }

            if (selfRegistrationEnabled && !trustedRequest) {
              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED, "Untrusted request.");
              log.error("Untrusted request.");
              return;
            }
          }

        Session session = request.getResourceResolver().adaptTo(Session.class);
        if (session == null) {
            throw new RepositoryException("JCR Session not found");
        }

        // check that the submitted parameter values have valid values.
        String principalName = request.getParameter(SlingPostConstants.RP_NODE_NAME);
        if (principalName == null) {
            throw new RepositoryException("User name was not submitted");
        }

        NameSanitizer san = new NameSanitizer(principalName, true);
        san.validate();

        String pwd = request.getParameter("pwd");
        if (pwd == null) {
            throw new RepositoryException("Password was not submitted");
        }
        String pwdConfirm = request.getParameter("pwdConfirm");
        if (!pwd.equals(pwdConfirm)) {
            throw new RepositoryException(
                "Password value does not match the confirmation password");
        }

        Session selfRegSession = null;
        try {
          selfRegSession = getSession();
          UserManager userManager = AccessControlUtil.getUserManager(selfRegSession);
          User user = userManager.createUser(principalName,
              digestPassword(pwd));
          log.info("User {} created", user.getID());

          String userPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_USER_PREFIX + user.getID();
          Map<String, RequestProperty> reqProperties = collectContent(
              request, response, userPath);
          response.setPath(userPath);
          response.setLocation(userPath);
          response.setParentLocation(AuthorizableResourceProvider.SYSTEM_USER_MANAGER_USER_PATH);
          changes.add(Modification.onCreated(userPath));

          // write content from form
          writeContent(selfRegSession, user, reqProperties, changes);
          try {
            if (selfRegSession.hasPendingChanges()) {
              selfRegSession.save();
            }
          } catch (RepositoryException e ) {
            log.warn("Failed to create user {} ",e.getMessage());
            log.debug("Failed to create user {} ",e.getMessage(), e);
            response
            .setStatus(HttpServletResponse.SC_CONFLICT, e.getMessage());
            return;
          }
          try {
            postProcessorService.process(user, selfRegSession, ModificationType.CREATE,
                request);
          } catch (RepositoryException e ) {
            log.warn("Failed to create user {} ",e.getMessage());
            log.debug("Failed to create user {} ",e.getMessage(), e);
            response
            .setStatus(HttpServletResponse.SC_CONFLICT, e.getMessage());
            return;
          } catch (Exception e) {
            log.warn(e.getMessage(), e);
            response
            .setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return;
          }

          // Launch an OSGi event for creating a user.
          try {
            Dictionary<String, String> properties = new Hashtable<String, String>();
            properties.put(UserConstants.EVENT_PROP_USERID, principalName);
            EventUtils
            .sendOsgiEvent(properties, UserConstants.TOPIC_USER_CREATED, eventAdmin);
          } catch (Exception e) {
            // Trap all exception so we don't disrupt the normal behaviour.
            log.error("Failed to launch an OSGi event for creating a user.", e);
          }
        } catch ( AuthorizableExistsException e) {
          log.warn(e.getMessage());
          response.setStatus(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch ( RepositoryException e ) {
          log.error(e.getMessage(),e);
          throw e;
        } finally {
            ungetSession(selfRegSession);
        }
    }

}
