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
package org.sakaiproject.nakamura.personal;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>SiteServiceGetServlet</code>
 */
@Component(immediate = true, label = "%home.showServlet.label", description = "%home.showServlet.desc")
@SlingServlet(resourceTypes = { "sakai/user-home", "sakai/group-home" }, methods = { "GET" }, generateComponent = false)
@ServiceDocumentation(name = "Show Site", description = " Shows the content of /dev/show.html when requested ", shortDescription = "Shows html page for users", bindings = @ServiceBinding(type = BindingType.TYPE, bindings = {
    "sakai/user-home", "sakai/group-home" }),

methods = @ServiceMethod(name = "GET", description = { "Shows  a HTML page when the user or groups home is accessed" }, response = { @ServiceResponse(code = 200, description = "A HTML template for the site, or json tree of the site depending on the request.") }))
public class ShowHomeServlet extends SlingSafeMethodsServlet implements OptingServlet {

  private static final long serialVersionUID = 613629169503411716L;

  @Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  @Property(value = "Renders user and groups")
  static final String SERVICE_DESCRIPTION = "service.description";

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    Resource resource = request.getResourceResolver().getResource("/dev/show.html");
    response.setContentType("text/html");
    response.setCharacterEncoding("UTF-8");
    response.setStatus(HttpServletResponse.SC_OK);
    IOUtils.stream(resource.adaptTo(InputStream.class), response.getOutputStream());
  }

  /**
   * Let other servlets take care of requests for JSON.
   *
   * @see org.apache.sling.api.servlets.OptingServlet#accepts(org.apache.sling.api.SlingHttpServletRequest)
   */
  public boolean accepts(SlingHttpServletRequest request) {
    String extension = request.getRequestPathInfo().getExtension();
    if ( extension == null || extension.length() == 0 ) {
      return true;
    } else {
      return false;
    }
  }

}
