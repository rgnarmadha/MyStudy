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
package org.sakaiproject.nakamura.files.servlets;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.files.FilesConstants;
import org.sakaiproject.nakamura.api.files.LinkHandler;
import org.sakaiproject.nakamura.files.JcrInternalFileHandler;
import org.sakaiproject.nakamura.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;

/**
 * Points the request to the actual file.
 * 
 */
@SlingServlet(resourceTypes={"sakai/link"}, methods={"GET"})
@Properties(value = {
    @Property(name = "service.description", value = "Links nodes to files."),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
@Reference(name="LinkHandler", referenceInterface=LinkHandler.class, cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE, policy=ReferencePolicy.DYNAMIC)
@ServiceDocumentation(
    name = "LinkServlet", 
    shortDescription = "Download file that this link points to.", 
    description = "When a user hits a sakai/link the file will be downloaded or, if necessary, the request will be redirected to the appropriate url.", 
    bindings = @ServiceBinding(
        type = BindingType.TYPE, 
        bindings = "sakai/link"
    ), 
    methods = @ServiceMethod(
        name = "GET", 
        description = "Downloads the file.",
        response = {
            @ServiceResponse(code = 200, description = "User was successfully linked to the real download (can be file/url)."),
            @ServiceResponse(code = 500, description = "Failed to redirect, explanation in HTML.")
        }
    )
)
public class LinkServlet extends SlingAllMethodsServlet {

  public static final Logger LOGGER = LoggerFactory.getLogger(LinkServlet.class);
  private static final long serialVersionUID = -1536743371265952323L;

  //
  // Needed to bind all the file handlers out there to this servlet.
  //
  private transient LinkHandlerTracker fileHandlerTracker = new LinkHandlerTracker();

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    Resource resource = request.getResource();
    Node node = (Node) resource.adaptTo(Node.class);

    try {
      if (node.hasProperty(FilesConstants.SAKAI_LINK)) {
        String link = node.getProperty(FilesConstants.SAKAI_LINK).getString();

        String[] linkProps = StringUtils.split(link, ':');
        LinkHandler handler = null;
        String path = null;
        if (linkProps.length == 2) {
          handler = fileHandlerTracker.getProcessorByName(linkProps[0]);
          path = linkProps[1];
        } else {
          // We default to JCR.
          handler = new JcrInternalFileHandler();
          path = link;
        }
        if (handler != null) {
          handler.handleFile(request, response, path);
        }
      }
    } catch (RepositoryException e) {
      LOGGER.warn("Unable to handle linked file.");
      response.sendError(500, "Unable to handle linked file.");
    }
  }


  protected void bindLinkHandler(ServiceReference serviceReference) {
    fileHandlerTracker.bindLinkHandler(serviceReference);
  }

  protected void unbindLinkHandler(ServiceReference serviceReference) {
    fileHandlerTracker.unbindLinkHandler(serviceReference);
  }

  protected void activate(ComponentContext componentContext) {
    fileHandlerTracker.setComponentContext(componentContext);
  }

}
