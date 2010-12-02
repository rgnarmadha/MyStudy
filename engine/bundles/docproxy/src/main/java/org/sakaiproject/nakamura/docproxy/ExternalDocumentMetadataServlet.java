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
import org.apache.sling.commons.json.JSONException;
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
import org.sakaiproject.nakamura.api.docproxy.DocProxyUtils;
import org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResultMetadata;
import org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.JcrUtils;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet provides access to the node metadata of an existing node.
 */
@ServiceDocumentation(
  name = "External Document Metadata Servlet",
  description = "Provides access to the node metadata of an existing node.",
  bindings = {
    @ServiceBinding(
      type = BindingType.TYPE,
      bindings = { "sakai/external-repository" },
      selectors = {
        @ServiceSelector(name = "metadata", description = "Binds to the metadata selector.")
      }
    )
  },
  methods = {
    @ServiceMethod(
      name = "POST",
      description = "Gets the metadata for an external repository document.",
      response = {
        @ServiceResponse(code = 200, description = "All processing finished successfully."),
        @ServiceResponse(code = 400, description = "Syntactically incorrect request or unknown repository."),
        @ServiceResponse(code = 401, description = "POST by anonymous user."),
        @ServiceResponse(code = 500, description = "Exception occurred during processing.")
      }
    ),
    @ServiceMethod(
      name = "GET",
      description = "Gets the metadata for an external repository document.",
      response = {
        @ServiceResponse(code = 200, description = "All processing finished successfully."),
        @ServiceResponse(code = 400, description = "Syntactically incorrect request or unknown repository."),
        @ServiceResponse(code = 404, description = "The requested resource couldn't be found."),
        @ServiceResponse(code = 500, description = "Exception occurred during processing.")
      }
    )
  }
)
@SlingServlet(selectors = "metadata", extensions = "json", resourceTypes = {
    "sling/nonexisting","sakai/external-repository" }, generateComponent = true, generateService = true, methods = {
    "GET", "POST" })
public class ExternalDocumentMetadataServlet extends SlingAllMethodsServlet {

  private static final long serialVersionUID = 1619334869539586945L;
  protected ExternalRepositoryProcessorTracker tracker;

  /**
   * Fetch the properties for this file.
   * 
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    // Because we are bound to a sling/nonexisting we iterate over the
    // URL and try to find the first real node.

    try {
      String url = request.getRequestURI();
      url = url.replace(".metadata.json", "");
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

      // Get the meta data.
      String path = url.substring(node.getPath().length());
      ExternalDocumentResultMetadata meta = processor.getDocumentMetadata(node, path);

      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");


      // Give a JSON representation.
      ExtendedJSONWriter write = new ExtendedJSONWriter(response.getWriter());
      DocProxyUtils.writeMetaData(write, meta);

    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Could not lookup file information.");
    } catch (DocProxyException e) {
      response.sendError(e.getCode(), e.getMessage());
    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Could not write JSON information.");
    }
  }

  /**
   * Write a couple of new properties on the file.
   * 
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @SuppressWarnings("unchecked")
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      String url = request.getRequestURI();
      url = url.replace(".metadata.json", "");
      Session session = request.getResourceResolver().adaptTo(Session.class);
      Node node = JcrUtils.getFirstExistingNode(session, url);

      if (!DocProxyUtils.isExternalRepositoryConfig(node)) {
        // This must be something else, ignore it..
        return;
      }

      // Anonymous users can't do anything.
      if (request.getRemoteUser().equals("anonymous")) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
            "Anonymous users can't post anything.");
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

      // Write the meta data.
      String path = url.substring(node.getPath().length());
      processor.updateDocument(node, path, request.getParameterMap(), null, -1);

      // FIXME: what is the response ?

    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Could not lookup file information.");
    } catch (DocProxyException e) {
      response.sendError(e.getCode(), e.getMessage());
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
