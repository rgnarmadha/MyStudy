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

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.util.RequestInfo;
import org.sakaiproject.nakamura.util.RequestWrapper;
import org.sakaiproject.nakamura.util.ResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@SlingServlet(methods = { "POST" }, generateService = true, paths = { "/system/batch" })
@ServiceDocumentation(
    name = "BatchServlet",
    shortDescription = "Bundles multiple requests into a single response.",
    description = "Allows multiple requests to be executed in a single request.",
    bindings = @ServiceBinding(
        type = BindingType.PATH,
        bindings = "/system/batch"
    ),
    methods = @ServiceMethod(
        name = "POST",
        description = "Get multiple request responses into a single response. It can do GET, POST and DELETE everything is defined in the json block.",
        parameters = @ServiceParameter(
            name = "requests",
            description = "A JSON string representing a request. <br />Example:" +
                "<pre>[{  \"url\" : \"/foo/bar\",  \"method\" : \"POST\",  \"parameters\" : {    \"val\" : 123,    \"val@TypeHint\" : \"Long\"  }},{  \"url\" : \"/_user/a/ad/admin/public/authprofile.json\",  \"method\" : \"GET\"}]</pre>"
        ),
        response = {@ServiceResponse(
            code = 200,
            description = "All requests are successful. <br />" +
                "A JSON array is returned containing an object for each resource. Example:" +
                "<pre>[\n" +
                "{\"url\": \"/_user/a/ad/admin/public/authprofile.json\",\n \"body\": \"{\"user\"...\",\n \"success\":true, \"status\": 200,\n \"headers\":{\"Content-Type\":\"application/json\"}\n} \n]</pre>"
          ),
          @ServiceResponse(
            code = 400,
            description = "The JSON object for the 'requests' parameter was malformed."
          ),
          @ServiceResponse(
            code = 500,
            description = "Unable to get and parse all requests."
          )
        }
    )
)
public class BatchServlet extends SlingAllMethodsServlet {

  private static final long serialVersionUID = 419598445499567027L;
  private static final Logger LOGGER = LoggerFactory
      .getLogger(BatchServlet.class);

  protected static final String REQUESTS_PARAMETER = "requests";

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    batchRequest(request, response, false);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    batchRequest(request, response, true);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doDelete(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doDelete(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPut(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPut(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
  }

  /**
   * Takes the original request and starts the batching.
   *
   * @param request
   * @param response
   * @throws IOException
   */
  protected void batchRequest(SlingHttpServletRequest request,
      SlingHttpServletResponse response, boolean allowModify) throws IOException {
    // Grab the JSON block out of it and convert it to RequestData objects we can use.
    String json = request.getParameter(REQUESTS_PARAMETER);

    // KERN-1061 NPE when request parameter is missing or empty
    if (json == null) {
      String msg = "The " + REQUESTS_PARAMETER + " parameter was not provided or was null.";
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
      LOGGER.warn(msg);
      return;
    }

    List<RequestInfo> batchedRequests = new ArrayList<RequestInfo>();
    try {
      JSONArray arr = new JSONArray(json);
      for (int i = 0; i < arr.length(); i++) {
        JSONObject obj = arr.getJSONObject(i);
        RequestInfo r = new RequestInfo(obj);
        if ( allowModify || r.isSafe() ) {
          batchedRequests.add(r);
        }
      }
    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Failed to parse the " + REQUESTS_PARAMETER + " parameter");
      LOGGER.warn("Failed to parse the " + REQUESTS_PARAMETER + " parameter");
      return;
    }

    // Loop over the requests and handle each one.
    try {
      StringWriter sw = new StringWriter();
      JSONWriter write = new JSONWriter(sw);
      write.object();
      write.key("results");
      write.array();

      for (RequestInfo r : batchedRequests) {
        doRequest(request, response, r, write);
      }
      write.endArray();
      write.endObject();
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      response.getWriter().write(sw.getBuffer().toString());
    } catch (JSONException e) {
      LOGGER.warn("Failed to create a JSON response");
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Failed to write JSON response");
    }
  }

  private void doRequest(SlingHttpServletRequest request,
      SlingHttpServletResponse response, RequestInfo requestInfo,
      JSONWriter write) throws JSONException {
    // Look for a matching resource in the usual way. If one is found,
    // the resource will also be embedded with any necessary RequestPathInfo.
    String requestPath = requestInfo.getUrl();
    ResourceResolver resourceResolver = request.getResourceResolver();
    Resource resource = resourceResolver.resolve(request, requestPath);

    // Wrap the request and response.
    RequestWrapper requestWrapper = new RequestWrapper(request, requestInfo);
    ResponseWrapper responseWrapper = new ResponseWrapper(response);
    RequestDispatcher requestDispatcher;
    try {
      // Get the response
      try {
        if (resource != null) {
          LOGGER.debug("Dispatching to request path='{}', resource path='{}'", requestPath, resource.getPath());
          requestDispatcher = request.getRequestDispatcher(resource);
        } else {
          LOGGER.debug("Dispatching to request path='{}', no resource", requestPath);
          requestDispatcher = request.getRequestDispatcher(requestPath);
        }
        requestDispatcher.forward(requestWrapper, responseWrapper);
      } catch (ResourceNotFoundException e) {
        responseWrapper.setStatus(HttpServletResponse.SC_NOT_FOUND);
      } catch (SlingException e) {
        responseWrapper.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
      // Write the response (status, headers, body) back to the client.
      writeResponse(write, responseWrapper, requestInfo);
    } catch (ServletException e) {
      writeFailedRequest(write, requestInfo);
    } catch (IOException e) {
      writeFailedRequest(write, requestInfo);
    }

  }

  private void writeResponse(JSONWriter write, ResponseWrapper responseWrapper,
      RequestInfo requestData) throws JSONException {
    try {
      String body = responseWrapper.getDataAsString();
      write.object();
      write.key("url");
      write.value(requestData.getUrl());
      write.key("success");
      write.value(true);
      write.key("body");
      write.value(body);
      write.key("status");
      write.value(responseWrapper.getResponseStatus());
      write.key("headers");
      write.object();
      Dictionary<String, String> headers = responseWrapper.getResponseHeaders();
      Enumeration<String> keys = headers.keys();
      while (keys.hasMoreElements()) {
        String k = keys.nextElement();
        write.key(k);
        write.value(headers.get(k));
      }
      write.endObject();
      write.endObject();
    } catch (UnsupportedEncodingException e) {
      writeFailedRequest(write, requestData);
    }
  }

  private void writeFailedRequest(JSONWriter write, RequestInfo requestData)
      throws JSONException {
    write.object();
    write.key("url");
    write.value(requestData.getUrl());
    write.key("success");
    write.value(false);
    write.endObject();
  }

}
