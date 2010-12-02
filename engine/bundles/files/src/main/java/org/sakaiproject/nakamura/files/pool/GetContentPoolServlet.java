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
package org.sakaiproject.nakamura.files.pool;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Work around performance issue with Sling's DefaultGetServlet. The JsonResourceWriter
 * ends up unnecessarily reading the complete stream for a binary property's value
 * just to report its length. Nakamura's ExtendedJSONWriter instead fetches the
 * property length directly, a much more efficient operation.
 *
 * TODO Enter patch for Sling.
 */
@SlingServlet(methods = { "GET" }, extensions = { "json" }, resourceTypes = { "sakai/pooled-content" })
public class GetContentPoolServlet extends SlingAllMethodsServlet implements OptingServlet {
  private static final long serialVersionUID = -382733858518678148L;
  private static final Logger LOGGER = LoggerFactory.getLogger(GetContentPoolServlet.class);

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    Node node = request.getResource().adaptTo(Node.class);

    // Check selectors.
    boolean isTidy = false;
    int recursion = 0;
    String[] selectors = request.getRequestPathInfo().getSelectors();
    if (selectors != null) {
      for (int i = 0; i < selectors.length; i++) {
        String selector = selectors[i];
        if ("tidy".equals(selector)) {
          isTidy = true;
        } else if (i == (selectors.length - 1)) {
          if ("infinity".equals(selector)) {
            recursion = -1;
          } else {
            try {
              recursion = Integer.parseInt(selector);
            } catch (NumberFormatException e) {
              LOGGER.warn("Invalid selector value '" + selector
                  + "'; defaulting recursion to 0");
            }
          }
        }
      }
    }

    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    ExtendedJSONWriter writer = new ExtendedJSONWriter(response.getWriter());
    writer.setTidy(isTidy);
    try {
      ExtendedJSONWriter.writeNodeTreeToWriter(writer, node, recursion);
    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      LOGGER.info("Caught JSONException {}", e.getMessage());
    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      LOGGER.info("Caught RepositoryException {}", e.getMessage());
    }
  }

  /**
   * Do not interfere with the default servlet's handling of streaming data,
   * which kicks in if no extension has been specified was specified in the
   * request. (Sling servlet resolution uses a servlet's declared list of
   * "extensions" for score weighing, not for filtering.)
   *
   * @see org.apache.sling.api.servlets.OptingServlet#accepts(org.apache.sling.api.SlingHttpServletRequest)
   */
  public boolean accepts(SlingHttpServletRequest request) {
    return "json".equals(request.getRequestPathInfo().getExtension());
  }
}
