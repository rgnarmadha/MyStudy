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
package org.sakaiproject.nakamura.antixss.servlet;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.antixss.AntiXssService;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;

/**
 *
 */
@ServiceDocumentation(
    name = "AntiXssServlet",
    shortDescription = "Filter all properties",
    description = "Filter all properties on output to remove any user generated content that might be bad or cause XSS.",
    bindings = @ServiceBinding(type = BindingType.TYPE,
        bindings = "sling/servlet/default",
        selectors = @ServiceSelector(name = "noxss")),
    methods = @ServiceMethod(name = "GET",
        description = "Gets the node in json format" +
        "Example:<br />" +
        "curl http://admin:admin@localhost:8080/some/user/generated/content.noxss.json",
        response = {
          @ServiceResponse(code = 200, description = "The servlet will send a JSON response tree of the node."),
          @ServiceResponse(code = 404, description = "The URL did not point to a node."),
          @ServiceResponse(code = 401, description = "The user is not logged in and needs to be to access the resource."),
          @ServiceResponse(code = 500, description = "The server had a problem processing the request.")}
))
@SlingServlet(methods={"GET"},selectors={"noxss"},extensions={"json"},resourceTypes={"sling/servlet/default"})
public class AntiXssServlet extends SlingSafeMethodsServlet {
  /**
   * 
   */
  private static final long serialVersionUID = -4327617774491837075L;

  @Reference
  protected transient AntiXssService antiXssService;
  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    
    Resource resource = request.getResource();
    if (resource == null || ResourceUtil.isNonExistingResource(resource)) {
      throw new ResourceNotFoundException("No data to render.");
    }
    Node node = resource.adaptTo(Node.class);
    
    response.setContentType(request.getResponseContentType());
    response.setCharacterEncoding("UTF-8");

    
    try {
      SafeJsonWriter safeWriter = new SafeJsonWriter(antiXssService, response.getWriter());
      SafeJsonWriter.writeNodeToWriter(safeWriter, node);
    } catch (JSONException e) {
      throw new ServletException(e);
    } catch (RepositoryException e) {
      throw new ServletException(e);
    }
  }
}
