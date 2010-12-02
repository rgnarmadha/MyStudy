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
package org.sakaiproject.nakamura.doc.search;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.DocumentationConstants;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.doc.DocumentationWriter;

import java.io.IOException;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@SlingServlet(paths = { "/system/doc/search" }, methods = { "GET" })
@ServiceDocumentation(name = "Search documentation", description = "Provides auto documentation of search nodes currently in the repository. Documentation will use the "
    + "node properties."
    + " Requests to this servlet take the form /system/doc/search?p=&lt;searchnodepath&gt where <em>searchnodepath</em>"
    + " is the absolute path of the searchnode deployed into the JCR repository. If the node is "
    + "not present a 404 will be retruned, if the node is present, it will be interogated to extract "
    + "documentation from the node. All documentation is assumed to be HTML encoded. If the browser is "
    + "directed to <a href=\"/system/doc/search\" >/system/doc/search</a> a list of all the search nodes in the system will be displayed ", shortDescription = "Documentation for all the searchnodes in the repository. ", url = "/system/doc/search", bindings = @ServiceBinding(type = BindingType.PATH, bindings = "/system/doc/search"), methods = { @ServiceMethod(name = "GET", description = "GETs to this servlet will produce documentation for the searchnode, "
    + "or an index of all searchnodes.", parameters = @ServiceParameter(name = "p", description = "The absolute path to a searchnode to display the documentation for"), response = {
    @ServiceResponse(code = 200, description = "html page for the requested resource"),
    @ServiceResponse(code = 404, description = "Search node not found") }) })
public class SearchDocumentationServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = -5820041368602931242L;
  private static final String PATH_PARAM = "p";

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    RequestParameter path = request.getRequestParameter(PATH_PARAM);
    Session session = request.getResourceResolver().adaptTo(Session.class);
    DocumentationWriter docWriter = new DocumentationWriter("Search nodes", response
        .getWriter());
    try {
      if (path != null) {
        docWriter.writeSearchInfo(path.getString(), session);
      } else {
        String query = "//*[@sling:resourceType='sakai/search']";
        docWriter.writeNodes(session, query, DocumentationConstants.PREFIX + "/proxy");
      }
    } catch (ItemNotFoundException e) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }
}
