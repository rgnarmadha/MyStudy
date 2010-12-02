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

import static org.sakaiproject.nakamura.api.user.UserConstants.PROP_GROUP_MANAGERS;
import static org.sakaiproject.nakamura.api.user.UserConstants.PROP_GROUP_VIEWERS;
import static org.sakaiproject.nakamura.api.user.UserConstants.SYSTEM_USER_MANAGER_GROUP_PREFIX;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jackrabbit.usermanager.impl.resource.AuthorizableResourceProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.impl.helper.RequestProperty;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
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

import java.security.Principal;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * Sling Post Servlet implementation for creating a group in the jackrabbit UserManager.
 * </p>
 * <h2>Rest Service Description</h2>
 * <p>
 * Creates a new group. Maps on to nodes of resourceType <code>sling/groups</code> like
 * <code>/rep:system/rep:userManager/rep:groups</code> mapped to a resource url
 * <code>/system/userManager/group</code>. This servlet responds at
 * <code>/system/userManager/group.create.html</code>
 * </p>
 * <h4>Methods</h4>
 * <ul>
 * <li>POST</li>
 * </ul>
 * <h4>Post Parameters</h4>
 * <dl>
 * <dt>:name</dt>
 * <dd>The name of the new group (required)</dd>
 * <dt>*</dt>
 * <dd>Any additional parameters become properties of the group node (optional)</dd>
 * </dl>
 * <h4>Response</h4>
 * <dl>
 * <dt>200</dt>
 * <dd>Success, a redirect is sent to the group resource locator. The redirect comes with
 * HTML describing the status.</dd>
 * <dt>500</dt>
 * <dd>Failure, including group already exists. HTML explains the failure.</dd>
 * </dl>
 * <h4>Example</h4>
 *
 * <code>
 * curl -F:name=newGroupA  -Fproperty1=value1 http://localhost:8080/system/userManager/group.create.html
 * </code>
 *
 * <h4>Notes</h4>
 *
 * @scr.component immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sling/groups"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.selectors" value="create"
 *
 * @scr.property name="servlet.post.dateFormats"
 *               values.0="EEE MMM dd yyyy HH:mm:ss 'GMT'Z"
 *               values.1="yyyy-MM-dd'T'HH:mm:ss.SSSZ" values.2="yyyy-MM-dd'T'HH:mm:ss"
 *               values.3="yyyy-MM-dd" values.4="dd.MM.yyyy HH:mm:ss"
 *               values.5="dd.MM.yyyy"
 *
 *
 */
@ServiceDocumentation(name="Create Group Servlet",
    description="Creates a new group. Maps on to nodes of resourceType sling/groups like " +
    		"/rep:system/rep:userManager/rep:groups mapped to a resource url " +
    		"/system/userManager/group. This servlet responds at /system/userManager/group.create.html",
    shortDescription="Creates a new group",
    bindings=@ServiceBinding(type=BindingType.PATH,bindings="/system/userManager/group.create.html",
        selectors=@ServiceSelector(name="create", description="Creates a new group"),
        extensions=@ServiceExtension(name="html", description="Posts produce html containing the update status")),
    methods=@ServiceMethod(name="POST",
        description={"Creates a new group with a name :name, " +
            "storing additional parameters as properties of the new group.",
            "Example<br>" +
            "<pre>curl -F:name=g-groupname -Fproperty1=value1 http://localhost:8080/system/userManager/group.create.html</pre>"},
        parameters={
        @ServiceParameter(name=":name", description="The name of the new group (required)"),
        @ServiceParameter(name="",description="Additional parameters become group node properties, " +
            "except for parameters starting with ':', which are only forwarded to post-processors (optional)")
        },
        response={
        @ServiceResponse(code=200,description="Success, a redirect is sent to the groups resource locator with HTML describing status."),
        @ServiceResponse(code=500,description="Failure, including group already exists. HTML explains failure.")
        }))

public class CreateSakaiGroupServlet extends AbstractSakaiGroupPostServlet implements
    ManagedService {


  /**
   *
   */
  private static final long serialVersionUID = 6587376522316825454L;

  private static final Logger LOGGER = LoggerFactory
      .getLogger(CreateSakaiGroupServlet.class);

  /**
   * The JCR Repository we access to resolve resources
   *
   * @scr.reference
   */
  protected transient SlingRepository repository;

  /**
   * Used to launch OSGi events.
   *
   * @scr.reference
   */
  protected transient EventAdmin eventAdmin;

  /**
   * Used to create the group.
   *
   * @scr.reference
   */
  protected transient AuthorizablePostProcessService postProcessorService;

  /**
   *
   * @scr.property value="authenticated,everyone" type="String"
   *               name="Groups who are allowed to create other groups" description=
   *               "A comma separated list of groups who area allowed to create other groups"
   */
  public static final String GROUP_AUTHORISED_TOCREATE = "groups.authorized.tocreate";

  private String[] authorizedGroups = {"authenticated"};


  /*
   * (non-Javadoc)
   *
   * @seeorg.apache.sling.jackrabbit.usermanager.post.AbstractAuthorizablePostServlet#
   * handleOperation(org.apache.sling.api.SlingHttpServletRequest,
   * org.apache.sling.api.servlets.HtmlResponse, java.util.List)
   */
  @Override
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(justification="If there is an exception, the user is certainly not admin", value={"REC_CATCH_EXCEPTION"})

  protected void handleOperation(SlingHttpServletRequest request,
      HtmlResponse response, List<Modification> changes)
      throws RepositoryException {

    // KERN-432 dont allow anon users to access create group.
    if ( SecurityConstants.ANONYMOUS_ID.equals(request.getRemoteUser()) ) {
      response.setStatus(403, "AccessDenied");
      return;
    }

        // check that the submitted parameter values have valid values.
        final String principalName = request.getParameter(SlingPostConstants.RP_NODE_NAME);
        if (principalName == null) {
            throw new RepositoryException("Group name was not submitted");
        }

    NameSanitizer san = new NameSanitizer(principalName, false);
    san.validate();

    // check for allow create Group
    boolean allowCreateGroup = false;
    User currentUser = null;

    try {
      Session currentSession = request.getResourceResolver().adaptTo(Session.class);
      UserManager um = AccessControlUtil.getUserManager(currentSession);
      currentUser = (User) um.getAuthorizable(currentSession.getUserID());
      if (currentUser.isAdmin()) {
        LOGGER.debug("User is an admin ");
        allowCreateGroup = true;
      } else {
        LOGGER.debug("Checking for membership of one of {} ", Arrays
            .toString(authorizedGroups));
        PrincipalManager principalManager = AccessControlUtil
            .getPrincipalManager(currentSession);
        PrincipalIterator pi = principalManager.getGroupMembership(principalManager
            .getPrincipal(currentSession.getUserID()));
        Set<String> groups = new HashSet<String>();
        for (; pi.hasNext();) {
          groups.add(pi.nextPrincipal().getName());
        }

        for (String groupName : authorizedGroups) {
          if (groups.contains(groupName)) {
            allowCreateGroup = true;
            break;
          }

          // TODO: move this nasty hack into the PrincipalManager dynamic groups need to
          // be in the principal manager for this to work.
          if ("authenticated".equals(groupName)
              && !SecurityConstants.ADMIN_ID.equals(currentUser.getID())) {
            allowCreateGroup = true;
            break;
          }

          // just check via the user manager for dynamic resolution.
          Group group = (Group) um.getAuthorizable(groupName);
          LOGGER.debug("Checking for group  {} {} ", groupName, group);
          if (group != null && group.isMember(currentUser)) {
            allowCreateGroup = true;
            LOGGER.debug("User is a member  of {} {} ", groupName, group);
            break;
          }
        }
      }
    } catch (Exception ex) {
      LOGGER.warn("Failed to determin if the user is an admin, assuming not. Cause: "
          + ex.getMessage());
      allowCreateGroup = false;
    }

    if (!allowCreateGroup) {
      LOGGER.debug("User is not allowed to create groups ");
      response.setStatus(HttpServletResponse.SC_FORBIDDEN,
          "User is not allowed to create groups");
      return;
    }

    Session session = getSession();

        try {
            UserManager userManager = AccessControlUtil.getUserManager(session);
            Authorizable authorizable = userManager.getAuthorizable(principalName);

            if (authorizable != null) {
                // principal already exists!
              response.setStatus(400,
                  "A principal already exists with the requested name: " + principalName);
              return;
            } else {
                Group group = userManager.createGroup(new Principal() {
                  public String getName() {
                      return principalName;
                  }
                });
                String groupPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX
                   + group.getID();
                Map<String, RequestProperty> reqProperties = collectContent(
                    request, response, groupPath);

                response.setPath(groupPath);
                response.setLocation(externalizePath(request, groupPath));
                response.setParentLocation(externalizePath(request,
                    AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PATH));
                changes.add(Modification.onCreated(groupPath));

                // It is not allowed to touch the rep:group-managers property directly.
                String key = SYSTEM_USER_MANAGER_GROUP_PREFIX + principalName + "/";
                reqProperties.remove(key + PROP_GROUP_MANAGERS);
                reqProperties.remove(key + PROP_GROUP_VIEWERS);

                // write content from form
                writeContent(session, group, reqProperties, changes);

                // update the group memberships, although this uses session from the request, it
                // only
                // does so for finding authorizables, so its ok that we are using an admin session
                // here.
                updateGroupMembership(request, group, changes);
                // TODO We should probably let the client decide whether the
                // current user belongs in the managers list or not.
                updateOwnership(request, group, new String[] {currentUser.getID()}, changes);

                try {
                  postProcessorService.process(group, session, ModificationType.CREATE, request);
                } catch (RepositoryException e) {
                  LOGGER.info("Failed to create Group  {}",e.getMessage());
                  response.setStatus(HttpServletResponse.SC_CONFLICT, e.getMessage());
                  return;
                } catch (Exception e) {
                  LOGGER.warn(e.getMessage(), e);
                  response
                     .setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                  return;
                }

                // Launch an OSGi event for creating a group.
                try {
                  Dictionary<String, String> properties = new Hashtable<String, String>();
                  properties.put(UserConstants.EVENT_PROP_USERID, principalName);
                  EventUtils
                      .sendOsgiEvent(properties, UserConstants.TOPIC_GROUP_CREATED, eventAdmin);
                } catch (Exception e) {
                  // Trap all exception so we don't disrupt the normal behaviour.
                  LOGGER.error("Failed to launch an OSGi event for creating a user.", e);
                }
            }
        } catch (RepositoryException re) {
          LOGGER.info("Failed to create Group  {}",re.getMessage());
          LOGGER.debug("Failed to create Group Cause {}",re,re.getMessage());
          response.setStatus(HttpServletResponse.SC_CONFLICT, re.getMessage());
          return;
        } finally {
            ungetSession(session);
        }
  }


  /** Returns the JCR repository used by this service. */
  @Override
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
        LOGGER.error("Unable to log out of session: " + t.getMessage(), t);
      }
    }
  }

  // ---------- SCR integration ---------------------------------------------

  /**
   * Activates this component.
   *
   * @param componentContext
   *          The OSGi <code>ComponentContext</code> of this component.
   */
  @Override
  protected void activate(ComponentContext componentContext) {
    super.activate(componentContext);
    String groupList = (String) componentContext.getProperties().get(
        GROUP_AUTHORISED_TOCREATE);
    if (groupList != null) {
      authorizedGroups = StringUtils.split(groupList, ',');
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @SuppressWarnings("rawtypes")
  public void updated(Dictionary dictionary) throws ConfigurationException {
    String groupList = (String) dictionary.get(GROUP_AUTHORISED_TOCREATE);
    if (groupList != null) {
      authorizedGroups = StringUtils.split(groupList, ',');
    }
  }

}
