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
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jcr.api.SlingRepository;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService;
import org.sakaiproject.nakamura.site.SiteAuthz;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Any special node-deletion handling based on properties cannot be implemented via
 * a SlingPostProcessor or an event observer because the node will no longer be
 * available when the code executes.
 */
@Component(immediate = true, label = "%site.deleteServlet.label", description = "%site.deleteServlet.desc")
@SlingServlet(resourceTypes = "sakai/site", methods = "POST", selectors = "delete", generateComponent = false)
@ServiceDocumentation(name="Site Delete Servlet",
    description=" Deletes the specified site and any dependent resources",
    shortDescription="Delete site",
    bindings=@ServiceBinding(type=BindingType.TYPE,bindings={"sakai/site"},
        selectors=@ServiceSelector(name="delete", description="Delete Site"),
        extensions=@ServiceExtension(name="html", description="Get the standard HTML response for node deletion")),
    methods=@ServiceMethod(name="POST",
        description={"Delete the site at the specified location. Example<br>" +
            "<pre>curl -X POST http://localhost:8080/sites/physics101/year3.delete.html</pre>"
        },
        response={
          @ServiceResponse(code=200,description="A HTML description of the operation."),
          @ServiceResponse(code=500,description="Failure with HTML explanation.")}
    ))
public class SiteDeleteServlet extends AbstractSiteServlet {
  private static final long serialVersionUID = -7070401970431158659L;
  private static final Logger LOGGER = LoggerFactory.getLogger(SiteDeleteServlet.class);

  @Reference
  protected transient SlingRepository slingRepository;
  
  @Reference
  private AuthorizablePostProcessService postProcessService;

  @Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  @Property(value = "Supports special handling of site deletion")
  static final String SERVICE_DESCRIPTION = "service.description";

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    HtmlResponse htmlResponse = new HtmlResponse();
    htmlResponse.setReferer(request.getHeader("referer"));
    Node site = request.getResource().adaptTo(Node.class);
    if (site == null) {
      htmlResponse.setStatus(HttpServletResponse.SC_NOT_FOUND, "Couldn't find site node");
    } else {
      if (!getSiteService().isSite(site)) {
        htmlResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST, "Location does not represent site ");
      } else {
        try {
          // Save a couple of properties which become inaccessible after the node
          // is removed.
          Session session = site.getSession();
          String sitePath = site.getPath();
          SiteAuthz authzHelper = new SiteAuthz(site, postProcessService);
          site.remove();
          authzHelper.deletionPostProcess(session, slingRepository);
          if (session.hasPendingChanges()) {
            session.save();
          }
          LOGGER.info("Delete site {}", sitePath);
          htmlResponse.onDeleted(sitePath);
        } catch (RepositoryException e) {
          LOGGER.warn(e.getMessage(), e);
          htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
              "Failed to service request: " + e.getMessage());
        }
      }
    }
    htmlResponse.send(response, true);
  }

}
