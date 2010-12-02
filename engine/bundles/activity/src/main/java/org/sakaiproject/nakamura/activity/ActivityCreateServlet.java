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
package org.sakaiproject.nakamura.activity;

import static org.apache.jackrabbit.JcrConstants.NT_UNSTRUCTURED;
import static org.sakaiproject.nakamura.api.activity.ActivityConstants.ACTIVITY_STORE_NAME;
import static org.sakaiproject.nakamura.api.activity.ActivityConstants.EVENT_TOPIC;
import static org.sakaiproject.nakamura.api.activity.ActivityConstants.PARAM_ACTOR_ID;
import static org.sakaiproject.nakamura.api.activity.ActivityConstants.PARAM_APPLICATION_ID;
import static org.sakaiproject.nakamura.api.activity.ActivityConstants.PARAM_TEMPLATE_ID;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.activity.ActivityConstants;
import org.sakaiproject.nakamura.api.activity.ActivityUtils;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.sakaiproject.nakamura.util.StringUtils;
import org.sakaiproject.nakamura.util.osgi.EventUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * In addition to the required parameters, properties should be included that will be used
 * to fill the bundle (i.e. macro expansions).
 * <p>
 * Required parameters:<br/>
 * applicationId: (i.e. used to locate the bundles)<br/>
 * templateId: (i.e. you know - the templateId. Locale will be appended to the templateId
 * for resolution.)
 */
@SlingServlet(selectors = { "activity" }, methods = { "POST" }, resourceTypes = { "sling/servlet/default" }, generateService = true, generateComponent = true)
@Properties(value = {
    @Property(name = "service.description", value = "Records the activity related to a particular node"),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
@ServiceDocumentation(name = "ActivityCreateServlet", shortDescription="Record activity related to a specific node.", description = "Record activity related to a specific node.", bindings = @ServiceBinding(type = BindingType.PATH, bindings = "*", selectors = @ServiceSelector(name = "activity")), methods = { @ServiceMethod(name = "POST", description = "Perform a post to a particular resource to record activity related to it.", parameters = {
    @ServiceParameter(name = "sakai:activity-appid", description = "i.e. used to locate the bundles"),
    @ServiceParameter(name = "sakai:activity-templateid", description = "The id of the template that will be used for text and macro expansion."
        + "Locale will be appended to the templateId for resolution"),
    @ServiceParameter(name = "*", description = "You should also include any parameters necessary to fill the template specified in sakai:activity-templateid.") }, response = {
    @ServiceResponse(code = 400, description = "if(applicationId == null || templateId == null || request.getRemoteUser() == null)"),
    @ServiceResponse(code = 404, description = "The node was not found.") }) })
public class ActivityCreateServlet extends SlingAllMethodsServlet {
  private static final long serialVersionUID = 1375206766455341437L;
  private static final Logger LOG = LoggerFactory
      .getLogger(ActivityCreateServlet.class);

  @Reference
  protected transient EventAdmin eventAdmin;

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    // Let's perform some validation on the request parameters.
    // Do we have the minimum required?
    RequestParameter applicationId = request
        .getRequestParameter(PARAM_APPLICATION_ID);
    if (applicationId == null || "".equals(applicationId.toString())) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "The applicationId parameter must not be null");
      return;
    }
    RequestParameter templateId = request
        .getRequestParameter(PARAM_TEMPLATE_ID);
    if (templateId == null || "".equals(templateId.toString())) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "The templateId parameter must not be null");
      return;
    }
    final String currentUser = request.getRemoteUser();
    if (currentUser == null || "".equals(currentUser)) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
          "CurrentUser could not be determined, user must be identifiable");
      return;
    }

    // Create or verify that an ActivityStore exists for content node
    // An activity store will be created for each node where a .activity gets executed.
    // TODO Maybe we shouldn't allow it on sakai/activity and sakai/activityFeed nodes?
    Node location = request.getResource().adaptTo(Node.class);
    if (location == null) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    Session session = null;
    String path = null;
    try {
      session = location.getSession();
      path = location.getPath() + "/" + ACTIVITY_STORE_NAME;
      if (!location.hasNode(ACTIVITY_STORE_NAME)) {
        // need to create an activityStore
        Node activityStoreNode = JcrUtils.deepGetOrCreateNode(session, path);
        activityStoreNode.setProperty(
            JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
            ActivityConstants.ACTIVITY_STORE_RESOURCE_TYPE);
      }
      String id = ActivityUtils.createId();
      path = ActivityUtils.getPathFromId(id, path);
      // for some odd reason I must manually create the Node before dispatching to
      // Sling...
      Node activity = JcrUtils.deepGetOrCreateNode(session, path, NT_UNSTRUCTURED);
      activity.addMixin("mix:created");
    } catch (RepositoryException e) {
      LOG.error(e.getMessage(), e);
      throw new Error(e);
    }

    final String activityItemPath = path;
    final RequestPathInfo requestPathInfo = request.getRequestPathInfo();
    // Wrapper which needs to remove the .activity selector from RequestPathInfo to avoid
    // an infinite loop.
    final RequestPathInfo wrappedPathInfo = createRequestPathInfo(
        requestPathInfo, activityItemPath);

    // Next insert the new RequestPathInfo into a wrapped Request
    SlingHttpServletRequest wrappedRequest = new SlingHttpServletRequestWrapper(
        request) {
      @Override
      public RequestPathInfo getRequestPathInfo() {
        return wrappedPathInfo;
      }
    };

    // We redispatch the request to the created sakai/activity.
    // This allows for custom properties to be set by the UI.
    Resource target = request.getResourceResolver().resolve(activityItemPath);
    LOG.debug("dispatch to {}  ", target);
    request.getRequestDispatcher(target).forward(wrappedRequest, response);

    // next add the current user to the actor property
    try {

      if (session.hasPendingChanges()) {
        session.save();
      }
      Node activity = (Node) session.getItem(activityItemPath);
      activity.setProperty(PARAM_ACTOR_ID, currentUser);
      activity.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
          ActivityConstants.ACTIVITY_ITEM_RESOURCE_TYPE);
      if (session.hasPendingChanges()) {
        session.save();
      }
    } catch (RepositoryException e) {
      LOG.error(e.getMessage(), e);
      throw new Error(e);
    }
    // post the asynchronous OSGi event
    final Dictionary<String, String> properties = new Hashtable<String, String>();
    properties.put(UserConstants.EVENT_PROP_USERID, request.getRemoteUser());
    properties.put(ActivityConstants.EVENT_PROP_PATH, activityItemPath);
    EventUtils.sendOsgiEvent(target, properties, EVENT_TOPIC, eventAdmin);
  }

  /**
   * @param requestPathInfo
   * @param activityItemPath
   * @return
   */
  protected RequestPathInfo createRequestPathInfo(
      final RequestPathInfo requestPathInfo, final String activityItemPath) {
    return new RequestPathInfo() {
      public String getSuffix() {
        return requestPathInfo.getSuffix();
      }

      public String[] getSelectors() {
        return StringUtils.removeString(requestPathInfo.getSelectors(),
            "activity");
      }

      public String getSelectorString() {
        return requestPathInfo.getSelectorString()
            .replaceAll("\\.activity", "");
      }

      public String getResourcePath() {
        return activityItemPath;
      }

      public String getExtension() {
        return requestPathInfo.getExtension();
      }
    };
  }

  /**
   * @param eventAdmin
   *          the new EventAdmin service to bind to this service.
   */
  protected void bindEventAdmin(EventAdmin eventAdmin) {
    this.eventAdmin = eventAdmin;
  }

  /**
   * @param eventAdmin
   *          the EventAdminService to be unbound from this service.
   */
  protected void unbindEventAdmin(EventAdmin eventAdmin) {
    this.eventAdmin = null;
  }
}
