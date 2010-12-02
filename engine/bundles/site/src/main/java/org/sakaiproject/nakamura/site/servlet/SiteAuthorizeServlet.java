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
package org.sakaiproject.nakamura.site.servlet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>SiteServiceGroupServlet</code> supports add and remove groups from the site.
 */
@Component(immediate = true, label = "%site.authorizeServlet.label", description = "site.authorizeServlet.desc")
@SlingServlet(resourceTypes = "sakai/site", methods = "POST", selectors = "authorize", generateComponent = false)
@ServiceDocumentation(name="Site Authorize Servlet",
    description=" The <code>SiteServiceGroupServlet</code> supports add and remove groups from the site.",
    shortDescription="Adds or removes groups from a site.",
    bindings=@ServiceBinding(type=BindingType.TYPE,bindings={"sakai/site"},
        selectors=@ServiceSelector(name="authorize", description="Authorize a group with a site"),
        extensions=@ServiceExtension(name="html", description="A standard HTML response for creating a node.")),
    methods=@ServiceMethod(name="POST",
        description={"Adds or removes the groups listed in the addauth, and removeauth request parameters to the site." +
        		" All of the groups must already exist. The service will also store the sites where the group is used on the " +
        		"group object.",
            "Example<br>" +
            "<pre>Example needed</pre>"
        },
        parameters={
          @ServiceParameter(name="addauth", description="The Path to the site being created (required)"),
          @ServiceParameter(name="removeauth", description="Path to a template node in JCR to use when creating the site (optional)")

        },
        response={
          @ServiceResponse(code=200,description="The body will be empty on sucess."),
          @ServiceResponse(code=400,description={
              "If the location does not represent a site.",
              "If the addauth and removeauth parameters dont make sense either becuase there are " +
              "the wrong numer of items or the groups dont exists"
          }),
          @ServiceResponse(code=403,description="Current user is not allowed to create a site in the current location."),
          @ServiceResponse(code=404,description="Resource was not found."),
          @ServiceResponse(code=500,description="Failure with HTML explanation.")}
    ))
public class SiteAuthorizeServlet extends AbstractSiteServlet {

  /**
   *
   */
  private static final long serialVersionUID = 6025706807478371356L;
  /**
   *
   */
  private static final Logger LOGGER = LoggerFactory
      .getLogger(SiteAuthorizeServlet.class);;

  @org.apache.felix.scr.annotations.Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  @org.apache.felix.scr.annotations.Property(value = "Supports Group association with the site.")
  static final String SERVICE_DESCRIPTION = "service.description";

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      Node site = request.getResource().adaptTo(Node.class);
      if (site == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Couldn't find site node");
        return;
      }
      Session session = site.getSession();
      UserManager userManager = AccessControlUtil.getUserManager(session);
      if (!getSiteService().isSite(site)) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "Location does not represent site ");
        return;
      }

      String[] addGroups = request.getParameterValues(SiteService.PARAM_ADD_GROUP);
      String[] removeGroups = request.getParameterValues(SiteService.PARAM_REMOVE_GROUP);
      if ((addGroups == null || addGroups.length == 0)
          && (removeGroups == null || removeGroups.length == 0)) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Either at least one "
            + SiteService.PARAM_ADD_GROUP + " or at least one "
            + SiteService.PARAM_REMOVE_GROUP + " must be specified");
        return;
      }
      Value[] groupValues = new Value[0];
      if (site.hasProperty(SiteService.AUTHORIZABLE)) {
        Property groupProperty = site.getProperty(SiteService.AUTHORIZABLE);
        groupValues = groupProperty.getValues();
      }
      Set<String> groups = new HashSet<String>();
      Map<String, Authorizable> removed = new HashMap<String, Authorizable>();
      Map<String, Authorizable> added = new HashMap<String, Authorizable>();
      for (Value v : groupValues) {
        groups.add(v.getString());
      }
      int changes = 0;
      if (removeGroups != null) {
        for (String remove : removeGroups) {
          groups.remove(remove);
          Authorizable auth = userManager.getAuthorizable(remove);
          if (auth != null) {
            removed.put(remove, auth);
          }
          changes++;
        }
      }
      if (addGroups != null) {
        for (String add : addGroups) {
          Authorizable auth = userManager.getAuthorizable(add);
          if (auth == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The authorizable "
                + add + " does not exist, nothing added ");
            return;
          }
          added.put(add, auth);
          groups.add(add);
          changes++;
        }
      }
      String path = site.getPath();
      String siteUuid = site.getIdentifier();

      // set the authorizables on the site
      site.setProperty(SiteService.AUTHORIZABLE, groups.toArray(new String[0]));

      // adjust the sites on each group added or removed.
      ValueFactory vf = session.getValueFactory();
      LOGGER.debug("Removing {} Site references to Site {} ", removed.size(),
          site.getPath());

      // remove old sites.
      for (Authorizable auth : removed.values()) {
        if (auth.hasProperty(SiteService.SITES)) {
          Value[] v = auth.getProperty(SiteService.SITES);
          if (v != null) {
            List<Value> vnew = new ArrayList<Value>();
            boolean r = false;
            for (int i = 0; i < v.length; i++) {
              if (!siteUuid.equals(v[i].getString())) {
                vnew.add(v[i]);
              } else {
                LOGGER.debug("Removing {}", siteUuid);
                r = true;
              }
            }
            if (r) {
              Value[] vnewa = vnew.toArray(new Value[0]);
              auth.setProperty(SiteService.SITES, vnewa);
            }
          }
        }
      }

      LOGGER.debug("Adding Site {} references to Site {} ", added.size(), site.getPath());
      // add new sites
      for (Authorizable auth : added.values()) {
        Value[] v = null;
        if (auth.hasProperty(SiteService.SITES)) {
          v = auth.getProperty(SiteService.SITES);
        }
        Value[] vnew = null;
        if (v == null) {
          vnew = new Value[1];
          vnew[0] = vf.createValue(siteUuid);
          LOGGER.debug("Adding Site {} to Group {} ", path, auth.getID());
        } else {
          boolean a = true;
          for (int i = 0; i < v.length; i++) {
            if (siteUuid.equals(v[i].getString())) {
              a = false;
              LOGGER.debug("Site {} already is present in Group {} ", path, auth.getID());
              break;
            }
          }
          if (a) {
            LOGGER.debug("Appending Site {} to Group {} ", path, auth.getID());
            vnew = new Value[v.length + 1];
            System.arraycopy(v, 0, vnew, 0, v.length);
            vnew[v.length] = vf.createValue(siteUuid);
          }

        }
        if (vnew != null) {
          auth.setProperty(SiteService.SITES, vnew);
        }
      }
      LOGGER.debug("Done processing  Site {} ", path);

      if (session.hasPendingChanges()) {
        session.save();
      }
      return;
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Failed to service request: " + e.getMessage());
      return;
    }

  }

}
