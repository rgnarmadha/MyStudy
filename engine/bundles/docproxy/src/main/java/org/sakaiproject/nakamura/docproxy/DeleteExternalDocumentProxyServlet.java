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
package org.sakaiproject.nakamura.docproxy;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.docproxy.DocProxyConstants;
import org.sakaiproject.nakamura.api.docproxy.DocProxyException;
import org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The servlet for requesting to delete an external repository document
 */

@ServiceDocumentation(
  name = "Delete External Document Proxy Servlet",
  description = "The servlet for requesting to delete an external repository document",
  bindings = {
    @ServiceBinding(
      type = BindingType.TYPE,
      bindings = { "sakai/external-repository" },
      selectors = { @ServiceSelector(name = "delete", description = "Binds to the delete selector.") }
    )
  },
  methods = {
    @ServiceMethod(
      name = "POST",
      description = "Delete an external repository document.",
      response = {
        @ServiceResponse(code = 200, description = "All processing finished successfully."),
        @ServiceResponse(code = 400, description = "Unknown external repository requested."),
        @ServiceResponse(code = 500, description = "Exception occurred during processing.")
      }
    )
  }
)
@SlingServlet(resourceTypes = { "sakai/external-repository" }, selectors = { "delete" }, methods = { "POST" }, generateComponent = true, generateService = true)
public class DeleteExternalDocumentProxyServlet extends SlingAllMethodsServlet {

  private static final long serialVersionUID = -6965132523477790182L;
  protected ExternalRepositoryProcessorTracker tracker;
  public static final Logger LOGGER = LoggerFactory
      .getLogger(ExternalDocumentProxyServlet.class);

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      Node node = request.getResource().adaptTo(Node.class);

      // for deletes, we look for the resources property
      String[] resources = request.getParameterValues("resources");
      String processorType = node.getProperty(DocProxyConstants.REPOSITORY_PROCESSOR)
          .getString();
      ExternalRepositoryProcessor processor = tracker.getProcessorByType(processorType);
      if (processor == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown repository.");
        return;
      }

      for (String resource : resources) {
        try {
          String path = resource.substring(node.getPath().length());
          processor.removeDocument(node, path);
        } catch (DocProxyException e) {
          response.sendError(e.getCode(), e.getMessage());
        }
      }

    } catch (RepositoryException e) {
      LOGGER.error("Failed to retrieve document's content", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Failed to retrieve document's content");
      return;
    }
  }

  protected void activate(ComponentContext context) {
    BundleContext bundleContext = context.getBundleContext();
    tracker = new ExternalRepositoryProcessorTracker(bundleContext,
        ExternalRepositoryProcessor.class.getName(), null);
    tracker.open();
  }

  protected void deactivate(ComponentContext context) {
    if (tracker != null) {
      tracker.close();
      tracker = null;
    }
  }

}
