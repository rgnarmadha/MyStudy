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
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.docproxy.DocProxyConstants;
import org.sakaiproject.nakamura.api.docproxy.DocProxyException;
import org.sakaiproject.nakamura.api.docproxy.DocProxyUtils;
import org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResult;
import org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor;
import org.sakaiproject.nakamura.util.IOUtils;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The generic access servlet for Node types of type sling/external-repository. This
 * servlet identify the node in question, determine the implementation of the External
 * Repository Processor from the node or the request, and then forward the request to that
 * processor appropriately, serializing any output onto http.
 */
@ServiceDocumentation(
  name = "External Document Proxy Servlet",
  description = "Generic access to external document resources",
  bindings = {
    @ServiceBinding(
      type = BindingType.TYPE,
      bindings = { "sakai/external-repository-document" }
    )
  },
  methods = {
    @ServiceMethod(
      name = "GET",
      description = "Gets a document from an external repository.",
      response = {
        @ServiceResponse(code = 200, description = "All processing finished successfully."),
        @ServiceResponse(code = 400, description = "Unknown repository."),
        @ServiceResponse(code = 404, description = "Unable to find the requested resource."),
        @ServiceResponse(code = 500, description = "Exception occurred during processing.")
      }
    )
  }
)
@SlingServlet(resourceTypes = { "sling/nonexisting", "sakai/external-repository-document" }, methods = { "GET" }, generateComponent = true, generateService = true)
public class ExternalDocumentProxyServlet extends SlingSafeMethodsServlet {

  protected ExternalRepositoryProcessorTracker tracker;
  private static final long serialVersionUID = 1521106164249874441L;
  public static final Logger LOGGER = LoggerFactory
      .getLogger(ExternalDocumentProxyServlet.class);

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      String url = request.getRequestURI();
      Session session = request.getResourceResolver().adaptTo(Session.class);
      Node node = JcrUtils.getFirstExistingNode(session, url);

/*
      --- this block removed because it yields an unusable path - zathomas

      if (DocProxyUtils.isExternalRepositoryDocument(node)) {
        // This document should reference the config node.
        String uuid = node.getProperty(REPOSITORY_REF).getString();
        node = session.getNodeByIdentifier(uuid);
      }

*/
      if (!DocProxyUtils.isExternalRepositoryConfig(node)) {
        // This must be something else, ignore it..
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Requested resource does not exist here: " + url);
        return;
      }

      // This is a repository node.
      // Get the processor and output meta data.
      String processorType = node.getProperty(DocProxyConstants.REPOSITORY_PROCESSOR)
          .getString();
      ExternalRepositoryProcessor processor = tracker.getProcessorByType(processorType);
      if (processor == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown repository.");
        return;
      }

      String path = url.substring(node.getPath().length());
      try {
        // Get actual content.
        ExternalDocumentResult result = processor.getDocument(node, path);
        InputStream in = result.getDocumentInputStream(0, session.getUserID());

        // FIXME: what about content type and encoding ?
        
        // Stream it to the user.
        OutputStream out = response.getOutputStream();
        IOUtils.stream(in, out);
      } catch (DocProxyException e) {
        response.sendError(e.getCode(), e.getMessage());
        return;
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
