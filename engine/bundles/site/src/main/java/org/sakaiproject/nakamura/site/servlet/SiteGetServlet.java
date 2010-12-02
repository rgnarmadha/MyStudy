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
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.site.SiteException;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService;
import org.sakaiproject.nakamura.site.SiteAuthz;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>SiteServiceGetServlet</code>
 */
@Component(immediate = true, label = "%site.getServlet.label", description = "%site.getServlet.desc")
@SlingServlet(resourceTypes = {"sakai/site"}, methods = {"GET"}, generateComponent = false)
@ServiceDocumentation(name="Get Site Servlet",
    description=" Gets the site container or the site definition depending on serialization ",
    shortDescription="Get site.",
    bindings=@ServiceBinding(type=BindingType.TYPE,bindings={"sakai/site"},
        extensions={
        @ServiceExtension(name="html", description="Get the html template for the site, ready for population by the client"),
        @ServiceExtension(name="json", description="Get a json tree of the site and metadata")
    }),

    methods=@ServiceMethod(name="GET",
        description={"This method gets one of two forms of the site. If json is requested, the site properties are seialized into" +
        		"a json structure. If html is requested, the site object is inspected to determine the html template and then " +
        		"the template is processed and sent back to the client.",
            "Example<br>" +
            "<pre>curl http://localhost:8080/sites/physics101/year3.html</pre>"
        },
        response={
          @ServiceResponse(code=200,description="A HTML template for the site, or json tree of the site depending on the request."),
          @ServiceResponse(code=204,description="When A site is not found"),
          @ServiceResponse(code=400,description={
              "If the location does not represent a site."
          }),
          @ServiceResponse(code=403,description="Current user is not allowed to create a site in the current location."),
          @ServiceResponse(code=404,description="Resource was not found."),
          @ServiceResponse(code=500,description="Failure with HTML explanation.")}
    ))
public class SiteGetServlet extends AbstractSiteServlet {

  private static final Logger LOG = LoggerFactory.getLogger(SiteGetServlet.class);
  private static final long serialVersionUID = 4874392318687088747L;

  @Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  @Property(value = "Renders sites")
  static final String SERVICE_DESCRIPTION = "service.description";
  
  @Reference
  private AuthorizablePostProcessService postProcessService;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    Node site = request.getResource().adaptTo(Node.class);
    if (site == null) {
      response.sendError(HttpServletResponse.SC_NO_CONTENT, "Couldn't find site node");
      return;
    }

    if ("json".equals(request.getRequestPathInfo().getExtension())) {
      renderAsJson(site, response);
      return;
    }

    if (!getSiteService().isSite(site)) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Location does not represent site ");
      return;
    }
    try {
      String templatePath = getSiteService().getSiteSkin(site);
      Resource siteTemplate = request.getResourceResolver().getResource(templatePath);
      if (siteTemplate == null) {
        LOG.warn("No site template found at location {} for site {}, will use default template (templates must be specified as absolute paths) ", new Object[] {
            templatePath, site, request.getResource().getPath()});
        templatePath = getSiteService().getDefaultSiteTemplate(site);
        siteTemplate = request.getResourceResolver().getResource(templatePath);
      }
      response.setContentType("text/html");
      response.setStatus(HttpServletResponse.SC_OK);
      IOUtils.stream(siteTemplate.adaptTo(InputStream.class), response.getOutputStream());
      return;
    } catch (SiteException e) {
      response.sendError(e.getStatusCode(), e.getMessage());
      return;
    }
  }

  private void renderAsJson(Node node, SlingHttpServletResponse response) throws IOException {
    // KERN-788 Output as json.
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    ExtendedJSONWriter writer = new ExtendedJSONWriter(response.getWriter());
    try {
      writer.object();
      ExtendedJSONWriter.writeNodeContentsToWriter(writer, node);
      // Add a non-persisted property to convey the current session's rights.
      try {
        SiteAuthz authzHelper = new SiteAuthz(node, postProcessService);
        boolean isMaintainer = authzHelper.isUserSiteMaintainer();
        boolean isPendingApproval = authzHelper.isUserPendingApproval();
        writer.key(SiteAuthz.SITE_IS_USER_MAINTAINER_PROPERTY);
        writer.value(isMaintainer);
        writer.key(SiteAuthz.SITE_IS_USER_PENDING_APPROVAL);
        writer.value(isPendingApproval);
      } catch (RepositoryException e) {
        LOG.warn("Problem with authorization setup for site", e);
        // Continue without additional properties.
      }
      writer.endObject();
      response.setStatus(HttpServletResponse.SC_OK);
    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

}
