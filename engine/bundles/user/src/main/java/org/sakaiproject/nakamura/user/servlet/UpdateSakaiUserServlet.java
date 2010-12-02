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

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jackrabbit.usermanager.impl.post.UpdateUserServlet;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * Sling Post Operation implementation for updating a user in the jackrabbit UserManager.
 * </p>
 * <h2>Rest Service Description</h2>
 * <p>
 * Updates a users properties. Maps on to nodes of resourceType <code>sling/users</code> like
 * <code>/rep:system/rep:userManager/rep:users</code> mapped to a resource url
 * <code>/system/userManager/user/ieb</code>. This servlet responds at
 * <code>/system/userManager/user/ieb.update.html</code>
 * </p>
 * <h4>Methods</h4>
 * <ul>
 * <li>POST</li>
 * </ul>
 * <h4>Post Parameters</h4>
 * <dl>
 * <dt>*</dt>
 * <dd>Any additional parameters become properties of the user node (optional)</dd>
 * <dt>*@Delete</dt>
 * <dd>Delete the property eg prop3@Delete means prop3 will be deleted (optional)</dd>
 * </dl>
 * <h4>Response</h4>
 * <dl>
 * <dt>200</dt>
 * <dd>Success, a redirect is sent to the users resource locator. The redirect comes with
 * HTML describing the status.</dd>
 * <dt>404</dt>
 * <dd>The resource was not found</dd>
 * <dt>500</dt>
 * <dd>Failure</dd>
 * </dl>
 * <h4>Example</h4>
 *
 * <code>
 * curl -Fprop1=value2 -Fproperty1=value1 http://localhost:8080/system/userManager/user/ieb.update.html
 * </code>
 *
 *
 *
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sling/user"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.selectors" value="update"
 *
 */
@ServiceDocumentation(name="Update User Servlet",
    description="Updates a user's properties. Maps on to nodes of resourceType sling/user " +
    		"like /rep:system/rep:userManager/rep:users mapped to a resource url " +
    		"/system/userManager/user/username . This servlet responds at " +
    		"/system/userManager/user/username.update.html",
    shortDescription="Update a user properties",
    bindings=@ServiceBinding(type=BindingType.TYPE,bindings={"sling/user"},
        selectors=@ServiceSelector(name="update", description="Updates the properties of a user"),
        extensions=@ServiceExtension(name="html", description="Posts produce html containing the update status")),
    methods=@ServiceMethod(name="POST",
        description={"Updates a user setting or deleting properties, " +
            "storing additional parameters as properties of the new user.",
            "Example<br>" +
            "<pre>curl -Fproperty1@Delete -Fproperty2=value2 http://localhost:8080/system/userManager/user/username.update.html</pre>"},
        parameters={
        @ServiceParameter(name="propertyName@Delete", description="Delete property, eg property1@Delete means delete property1 (optional)"),
        @ServiceParameter(name="",description="Additional parameters become user node properties, " +
            "except for parameters starting with ':', which are only forwarded to post-processors (optional)")
        },
        response={
          @ServiceResponse(code=200,description="Success, a redirect is sent to the user's resource locator with HTML describing status."),
          @ServiceResponse(code=404,description="User was not found."),
          @ServiceResponse(code=500,description="Failure with HTML explanation.")}
        ))

public class UpdateSakaiUserServlet extends UpdateUserServlet {

  /**
   *
   */
  private static final long serialVersionUID = 8697964295729373458L;

  private static final Logger LOGGER = LoggerFactory
      .getLogger(UpdateSakaiUserServlet.class);

  /**
   * @scr.reference
   */
  private transient AuthorizablePostProcessService postProcessorService;

  /**
   * Used to launch OSGi events.
   *
   * @scr.reference
   */
  protected transient EventAdmin eventAdmin;

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.jackrabbit.usermanager.post.CreateUserServlet#handleOperation(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.servlets.HtmlResponse, java.util.List)
   */
  @Override
  protected void handleOperation(SlingHttpServletRequest request, HtmlResponse response,
      List<Modification> changes) throws RepositoryException {
    super.handleOperation(request, response, changes);
    Resource resource = request.getResource();
    Authorizable authorizable = resource.adaptTo(Authorizable.class);
    try {
      Session session = request.getResourceResolver().adaptTo(Session.class);
      postProcessorService.process(authorizable, session, ModificationType.MODIFY,
          request);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    }

    // Launch an OSGi event for updating a user.
    try {
      Dictionary<String, String> properties = new Hashtable<String, String>();
      properties.put(UserConstants.EVENT_PROP_USERID, authorizable.getID());
      EventUtils
          .sendOsgiEvent(properties, UserConstants.TOPIC_USER_UPDATE, eventAdmin);
    } catch (Exception e) {
      // Trap all exception so we don't disrupt the normal behaviour.
      LOGGER.error("Failed to launch an OSGi event for creating a user.", e);
    }
  }

}
