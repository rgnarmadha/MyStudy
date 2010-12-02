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
package org.sakaiproject.nakamura.search;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>JsonQueryServletBlocker</code> blocks the default sling JsonQueryServlet
 */
@ServiceDocumentation(
  name = "Json Query Servlet Blocker",
  description = "Blocks the default sling JsonQueryServlet",
  bindings = {
    @ServiceBinding(
      type = BindingType.TYPE,
      bindings = { "sling/servlet/default" },
      selectors = { @ServiceSelector(name = "query", description = "Binds to the query selector.") },
      extensions = @ServiceExtension(name = "json", description = "javascript object notation")
    )
  },
  methods = {
    @ServiceMethod(
      name = "GET",
      description = "Create an external repository document.",
      response = {
        @ServiceResponse(code = 501, description = "Unimplemented.")
      }
    )
  }
)
@SlingServlet(extensions={"json"}, methods={"GET"}, resourceTypes={"sling/servlet/default"}, selectors={"query"} )
@Properties(value={
 @Property(name="sling.servlet.prefix", value={"-1"}),
 @Property(name="service.description", value={"Blocks the default sling JsonQueryServlet."}),
 @Property(name="service.vendor", value={"The Sakai Foundation"})
})
public class JsonQueryServletBlocker extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = 9135814126478411413L;

  @Override
  protected void doGet(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
  }
}
