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
package org.sakaiproject.nakamura.batch;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.batch.WidgetService;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@ServiceDocumentation(name = "WidgetsServlet", description = "Gives a list of all the known widgets in the system.", shortDescription = "List all the widgets", methods = { @ServiceMethod(parameters = { @ServiceParameter(name = "callback", description = { "Optional parameter that determines the name of the callback function for the json-p output. If this parameter is not found, than normal json will be outputted." }) }, response = {
    @ServiceResponse(code = 200, description = {
        "Will output a JSON object with all the widgets in the system.",
        "This servlet will only check the preconfigured locations. These can be modified in the felix admin console panel. The folder should be the toplevel folder that contains the widgets. Each subfolder should represent a widget and should contain a 'config.json' file.",
        "In the JSON response, each key represents a widgetname and will have the content of the 'config.json' file outputted in it." }),
    @ServiceResponse(code = 500, description = { "The servlet is unable to produce a proper JSON output." }) }) }, bindings = { @ServiceBinding(type = BindingType.PATH, bindings = { "/var/widgets" }) })
@SlingServlet(methods = { "GET" }, paths = { "/var/widgets" }, generateComponent = false, generateService = true)
@Component(metatype = true, immediate = true)
public class WidgetsServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = -4113451154211163118L;
  private static final Logger LOGGER = LoggerFactory.getLogger(WidgetsServlet.class);

  @Reference
  protected transient WidgetService widgetService;

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    // The resolver that can be used to resolve widget resources (JcrNodeResource or
    // FsResource)
    ResourceResolver resolver = request.getResourceResolver();

    // We will store all the found widgets in this map.
    // The key will be the name of widget.
    Map<String, ValueMap> validWidgets = widgetService.getWidgetConfigs(resolver);

    // Depending on the parameter 'callback' we send out json or json-p.
    RequestParameter callbackParam = request.getRequestParameter("callback");

    response.setCharacterEncoding("UTF-8");
    PrintWriter printWriter = response.getWriter();

    if (callbackParam != null) {
      response.setContentType("application/javascript");
      printWriter.append(callbackParam.getString("UTF-8"));
      printWriter.append("(");
    } else {
      // Ensure that we're sending out proper json.
      response.setContentType("application/json");
    }

    // Write the whole map
    ExtendedJSONWriter writer = new ExtendedJSONWriter(printWriter);
    try {
      writer.object();
      for (Entry<String, ValueMap> entry : validWidgets.entrySet()) {
        writer.key(entry.getKey());
        writer.valueMap(entry.getValue());
      }
      writer.endObject();
      if (callbackParam != null) {
        printWriter.append(");");
      }
    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Failed to construct proper JSON.");
      LOGGER.error("Failed to construct proper JSON.", e);
    }

  }

}