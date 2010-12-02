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
package org.sakaiproject.nakamura.proxy;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.framework.Constants;
import org.sakaiproject.nakamura.api.proxy.ProxyPostProcessor;
import org.sakaiproject.nakamura.api.proxy.ProxyResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Retrieves the content type of a requested URL.
 */
@Component
@Service
@Properties(value = {
    @Property(name = Constants.SERVICE_VENDOR, value = "Sakai Foundation"),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Post processor to return content type of requested URL.") })
public class HeaderProxyPostProcessor implements ProxyPostProcessor {
  public static final String NAME = "header";

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.proxy.ProxyPostProcessor#process(org.apache.sling.api.SlingHttpServletResponse,
   *      org.sakaiproject.nakamura.api.proxy.ProxyResponse)
   */
  public void process(Map<String, Object> templateParams,
      SlingHttpServletResponse response, ProxyResponse proxyResponse) throws IOException {
    HashMap<String, String> headers = new HashMap<String, String>();

    Map<String, String[]> responseHeaders = proxyResponse.getResponseHeaders();
    Object requestedHeaders = templateParams.get("header");

    if (requestedHeaders instanceof String[]) {
      headers.putAll(retrieveHeaders(responseHeaders, (String[]) requestedHeaders));
    } else {
      headers.putAll(retrieveHeaders(responseHeaders, (String) requestedHeaders));
    }

    try {
      JSONObject jsonObject = new JSONObject();
      for (Entry<String, String> header : headers.entrySet()) {
        jsonObject.put(header.getKey(), header.getValue());
      }
      String output = jsonObject.toString();
      response.getWriter().print(output);

      response.setStatus(proxyResponse.getResultCode());
    } catch (JSONException je) {
      response.sendError(SC_INTERNAL_SERVER_ERROR, je.getMessage());
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.proxy.ProxyPostProcessor#getName()
   */
  public String getName() {
    return NAME;
  }

  private Map<String, String> retrieveHeaders(Map<String, String[]> responseHeaders,
      String... requestedHeaders) {
    HashMap<String, String> output = new HashMap<String, String>();
    if (requestedHeaders != null
        && !(requestedHeaders.length == 1 && requestedHeaders[0] == null)) {
      for (String requestedHeader : requestedHeaders) {
        if (responseHeaders.containsKey(requestedHeader)) {
          String headerValue = "";
          String[] header = responseHeaders.get(requestedHeader);
          if (header != null) {
            headerValue = responseHeaders.get(requestedHeader)[0];

            if (headerValue == null) {
              headerValue = "";
            }
          }
          output.put(requestedHeader, headerValue);
        }
      }
    } else {
      for (Entry<String, String[]> responseHeader : responseHeaders.entrySet()) {
        String headerKey = responseHeader.getKey();
        String[] headerValue = responseHeader.getValue();
        output.put(headerKey, headerValue[0]);
      }
    }
    return output;
  }
}
