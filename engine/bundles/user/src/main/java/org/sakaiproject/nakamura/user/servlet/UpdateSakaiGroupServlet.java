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
package org.sakaiproject.nakamura.user.servlet;

import static org.sakaiproject.nakamura.api.user.UserConstants.PROP_GROUP_MANAGERS;
import static org.sakaiproject.nakamura.api.user.UserConstants.PROP_GROUP_VIEWERS;
import static org.sakaiproject.nakamura.api.user.UserConstants.PROP_MANAGED_GROUP;
import static org.sakaiproject.nakamura.api.user.UserConstants.PROP_MANAGERS_GROUP;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jackrabbit.usermanager.impl.resource.AuthorizableResourceProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.apache.sling.servlets.post.impl.helper.RequestProperty;
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
 * Sling Post Operation implementation for updating a group in the jackrabbit UserManager.
 * </p>
 * <h2>Rest Service Description</h2>
 * <p>
 * Updates a group's properties. Maps on to nodes of resourceType
 * <code>sling/groups</code> like
 * <code>/rep:system/rep:userManager/rep:groups/ae/3f/ed/testGroup</code> mapped to a
 * resource url <code>/system/userManager/group/testGroup</code>. This servlet responds at
 * <code>/system/userManager/group/testGroup.update.html</code>
 * </p>
 * <h4>Methods</h4>
 * <ul>
 * <li>POST</li>
 * </ul>
 * <h4>Post Parameters</h4>
 * <dl>
 * <dt>*</dt>
 * <dd>Any additional parameters become properties of the group node (optional)</dd>
 * <dt>*@Delete</dt>
 * <dd>The property is deleted, eg prop1@Delete</dd>
 * </dl>
 * <h4>Response</h4>
 * <dl>
 * <dt>200</dt>
 * <dd>Success, a redirect is sent to the group's resource locator. The redirect comes
 * with HTML describing the status.</dd>
 * <dt>404</dt>
 * <dd>The resource was not found</dd>
 * <dt>500</dt>
 * <dd>Failure</dd>
 * </dl>
 * <h4>Example</h4>
 *
 * <code>
 * curl -Fprop1=value2 -Fproperty1=value1 http://localhost:8080/system/userManager/group/testGroup.update.html
 * </code>
 *
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" values="sling/group"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.selectors" value="update"
 * @scr.property name="servlet.post.dateFormats"
 *               values.0="EEE MMM dd yyyy HH:mm:ss 'GMT'Z"
 *               values.1="yyyy-MM-dd'T'HH:mm:ss.SSSZ" values.2="yyyy-MM-dd'T'HH:mm:ss"
 *               values.3="yyyy-MM-dd" values.4="dd.MM.yyyy HH:mm:ss"
 *               values.5="dd.MM.yyyy"
 *
 */
@ServiceDocumentation(name = "Update Group Servlet", description = "Updates a group's properties. Maps on to nodes of resourceType sling/group "
    + "like /rep:system/rep:userManager/rep:groups/ae/3f/ed/groupname mapped to a resource "
    + "url /system/userManager/group/g-groupname. This servlet responds at "
    + "/system/userManager/group/groupname.update.html", shortDescription = "Update a group properties", bindings = @ServiceBinding(type = BindingType.TYPE, bindings = { "sling/group" }, selectors = @ServiceSelector(name = "update", description = "Updates the properties of a group"), extensions = @ServiceExtension(name = "html", description = "Posts produce html containing the update status")), methods = @ServiceMethod(name = "POST", description = {
    "Updates a group setting or deleting properties, "
        + "storing additional parameters as properties of the group.",
    "Example<br>"
        + "<pre>curl -Fproperty1@Delete -Fproperty2=value2 http://localhost:8080/system/userManager/group/g-groupname.update.html</pre>" }, parameters = {
    @ServiceParameter(name = "propertyName@Delete", description = "Delete property, eg property1@Delete means delete property1 (optional)"),
    @ServiceParameter(name = ":member", description = "Add a member to this group (optional)"),
    @ServiceParameter(name = ":member@Delete", description = "Remove a member from this group (optional)"),
    @ServiceParameter(name = ":manager", description = "Add a manager to this group, note: this does not add the manager as a member! (optional)"),
    @ServiceParameter(name = ":manager@Delete", description = "Remove a manager from this group, note: this does not remove the manager as a member! (optional)"),
    @ServiceParameter(name = ":viewer", description = "Add a viewer to this group, note: this does not add the viewer as a member! (optional)"),
    @ServiceParameter(name = ":viewer@Delete", description = "Remove a viewer from this group, note: this does not remove the viewer as a member! (optional)"),
    @ServiceParameter(name = "propertyName@Delete", description = "Delete property, eg property1@Delete means delete property1 (optional)"),
    @ServiceParameter(name="",description="Additional parameters become group node properties, " +
        "except for parameters starting with ':', which are only forwarded to post-processors (optional)")
    }, response={
    @ServiceResponse(code = 200, description = "Success, a redirect is sent to the group's resource locator with HTML describing status."),
    @ServiceResponse(code = 404, description = "Group was not found."),
    @ServiceResponse(code = 500, description = "Failure with HTML explanation.") }))
public class UpdateSakaiGroupServlet extends AbstractSakaiGroupPostServlet {

  /**
   *
   */
  private static final long serialVersionUID = -2378929115784007976L;

  private static final Logger LOGGER = LoggerFactory
      .getLogger(UpdateSakaiGroupServlet.class);

  /**
   * The post processor service.
   *
   * @scr.reference
   */
  protected transient AuthorizablePostProcessService postProcessorService;

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

  /** Returns the JCR repository used by this service. */
  protected SlingRepository getRepository() {
    return repository;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.jackrabbit.usermanager.post.CreateUserServlet#handleOperation(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.servlets.HtmlResponse, java.util.List)
   */
  @Override
  protected void handleOperation(SlingHttpServletRequest request,
      HtmlResponse htmlResponse, List<Modification> changes) throws RepositoryException {

    Authorizable authorizable = null;
    Resource resource = request.getResource();

    if (resource != null) {
      authorizable = resource.adaptTo(Authorizable.class);
    }

    // check that the group was located.
    if (authorizable == null) {
      throw new ResourceNotFoundException("Group to update could not be determined");
    }
    Session session = request.getResourceResolver().adaptTo(Session.class);
    if (session == null) {
      throw new RepositoryException("JCR Session not found");
    }
    try {
      String groupPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX
      + authorizable.getID();

      Map<String, RequestProperty> reqProperties = collectContent(request, htmlResponse, groupPath);
      try {
        // cleanup any old content (@Delete parameters)
        // This is the only way to make a private group (one with a "rep:group-viewers"
        // property) no longer private.
        processDeletes(authorizable, reqProperties, changes);

        // It is not allowed to touch the rep:group-managers and rep:group-viewers
        // properties directly except to delete them.
        reqProperties.remove(groupPath + "/" + PROP_GROUP_MANAGERS);
        reqProperties.remove(groupPath + "/" + PROP_GROUP_VIEWERS);

        // Block direct manipulation of the properties that implement
        // a Sakai Group entity's Managers group.
        reqProperties.remove(PROP_MANAGERS_GROUP);
        reqProperties.remove(PROP_MANAGED_GROUP);

        // write content from form
        writeContent(session, authorizable, reqProperties, changes);

        // update the group memberships
        if (authorizable.isGroup()) {
          updateGroupMembership(request, authorizable, changes);
          updateOwnership(request, (Group)authorizable, new String[0], changes);
        }
      } catch (RepositoryException re) {
        throw new RepositoryException("Failed to update group.", re);
      }

      try {
        postProcessorService.process(authorizable, session, ModificationType.MODIFY, request);
      } catch (Exception e) {
        LOGGER.warn(e.getMessage(), e);

        htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e
            .getMessage());
        return;
      }

      if (session.hasPendingChanges()) {
        session.save();
      }

      // Launch an OSGi event for updating a group.
      try {
        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(UserConstants.EVENT_PROP_USERID, authorizable.getID());
        EventUtils
            .sendOsgiEvent(properties, UserConstants.TOPIC_GROUP_CREATED, eventAdmin);
      } catch (Exception e) {
        // Trap all exception so we don't disrupt the normal behaviour.
        LOGGER.error("Failed to launch an OSGi event for creating a user.", e);
      }

    } catch (Throwable t) {
      LOGGER.debug("Failed " + t.getMessage(), t);
      throw new RepositoryException(t.getMessage(), t);
    }
  }

  /**
   * @param slingRepository
   */
  protected void bindRepository(SlingRepository slingRepository) {
    this.repository = slingRepository;

  }

  /**
   * @param slingRepository
   */
  protected void unbindRepository(SlingRepository slingRepository) {
    this.repository = null;

  }

}
