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
package org.sakaiproject.nakamura.captcha;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.captcha.CaptchaService;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@ServiceDocumentation(
  name = "ReCaptcha Servlet",
  description = "All the necessary properties the UI needs to communicate with the reCAPTCHA.net service.",
  bindings = {
    @ServiceBinding(
      type = BindingType.TYPE,
      bindings = { "system/captcha" }
    )
  },
  methods = {
    @ServiceMethod(
      name = "GET",
      description = "Get the captcha properties.",
      response = {
        @ServiceResponse(code = 200, description = "All processing finished successfully."),
        @ServiceResponse(code = 500, description = "Exception occurred during processing.")
       }
    )
  }
)
@SlingServlet(paths = { "/system/captcha" }, methods = { "GET" }, generateComponent = true, generateService = true)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "All the necessary properties the UI needs to communicate with the reCAPTCHA.net service.") })
public class ReCaptchaServlet extends SlingSafeMethodsServlet {

  @Reference
  protected transient CaptchaService captchaService;

  /**
   *
   */
  private static final long serialVersionUID = -384817779459717337L;

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
      // Send out proper application/json
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");

      // Get the properties from the service we're using.
      Map<String, Object> map = captchaService.getProperties();

      // Write them all out.
      // It's up to the implementation to expose the correct properties.
      JSONWriter writer = new JSONWriter(response.getWriter());
      writer.object();
      for (Entry<String, Object> e : map.entrySet()) {
        writer.key(e.getKey());
        writer.value(e.getValue());
      }
      writer.endObject();
    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Failed to create the proper JSON structure.");
    }
  }

}
