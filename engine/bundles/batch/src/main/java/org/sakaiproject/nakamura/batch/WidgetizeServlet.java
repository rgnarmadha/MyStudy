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
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.StringUtils;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@ServiceDocumentation(name = "WidgetizeServlet", shortDescription = "Fetch all the resources for a widget.", description = { "Fetch all the resources of a widget in one request." }, bindings = { @ServiceBinding(type = BindingType.TYPE, selectors = { @ServiceSelector(name = "widgetize") }, extensions = { @ServiceExtension(name = "json") }) }, methods = { @ServiceMethod(name = "GET", description = { "Fetches all the resources and specified language bundles for a widget in one request." }, parameters = { @ServiceParameter(name = "locale", description = "What locale should be used for the language bundle. This should be in the ISO3 format. ie: en_US or zh_CN.") }, response = {
    @ServiceResponse(code = 200, description = {
        "A JSON response will be streamed back. This exists out of 2 parts",
        "<ul><li>Language bundles</li><li>Widget files</li></ul>",
        "Language bundles",
        "There will be a key in the json object called 'bundles'. This key will contain an object that will contain 2 child-objects.<br />The first one will always be 'default' which is the output for the default language bundle of a widget.<br />The other one will be the one specified in the request parameter (or the server default if none has been specified.)<br /> If the language bundle could not be found an empty object will be returned.",
        "Widget files",
        "The servlet will walk down the tree and try to get the content of each resource. It will then try to get the mimetype of this file. If the mimetype is in the list of allowed mimetypes it will be outputted. This list can be modified in the felix admin console." }),
    @ServiceResponse(code = 403, description = { "The resource where this action is performed on is not a valid widget." }) }

) })
@SlingServlet(resourceTypes = { "sling/servlet/default" }, methods = { "GET" }, selectors = { "widgetize" }, extensions = { "json" }, generateService = true, generateComponent = false)
@Component(metatype = true, immediate = true)
public class WidgetizeServlet extends SlingSafeMethodsServlet {
  private static final long serialVersionUID = -8498483459709451448L;

  @Reference
  protected transient WidgetService widgetService;

  private static final String LOCALE_PARAM = "locale";

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    // Get the necessary values.
    String path = request.getResource().getPath();
    ResourceResolver resolver = request.getResourceResolver();
    RequestParameter localeParam = request.getRequestParameter(LOCALE_PARAM);
    Locale locale = null;
    if (localeParam != null) {
      String[] l = StringUtils.split(localeParam.getString(), '_');
      locale = new Locale(l[0], l[1]);
    }

    // Get the values.
    ValueMap map = null;
    try {
      map = widgetService.getWidget(path, locale, resolver);
    } catch (IllegalArgumentException e) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN,
          "The current resource is not a widget.");
      return;
    }

    // Make sure that we're outputting proper json.
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    // Output all the widget info.
    try {
      ExtendedJSONWriter writer = new ExtendedJSONWriter(response.getWriter());
      writer.valueMap(map);
    } catch (JSONException e) {
      throw new ServletException("Could not output proper JSON.", e);
    }
  }
}
