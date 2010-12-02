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
package org.sakaiproject.nakamura.version.impl;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.version.VersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Gets a version
 */

@ServiceDocumentation(name = "List Versions Servlet", description = "Lists versions of a resource in json format", shortDescription = "List versions of a resource", bindings = @ServiceBinding(type = BindingType.TYPE, bindings = { "sling/servlet/default" }, selectors = @ServiceSelector(name = "versions", description = "Retrieves a paged list of versions for the resource"), extensions = @ServiceExtension(name = "json", description = "A list over versions in json format")), methods = @ServiceMethod(name = "GET", description = {
    "Lists previous versions of a resource. The url is of the form "
        + "http://host/resource.versions.json ",
    "Example<br>"
        + "<pre>curl http://localhost:8080/sresource/resource.versions.json</pre>" }, parameters = {
    @ServiceParameter(name = "items", description = "The number of items per page"),
    @ServiceParameter(name = "page", description = "The page to of items to return") }, response = {
    @ServiceResponse(code = 200, description = "Success a body is returned containing a json tree"),
    @ServiceResponse(code = 404, description = "Resource was not found."),
    @ServiceResponse(code = 500, description = "Failure with HTML explanation.") }))
@SlingServlet(resourceTypes = "sling/servlet/default", methods = "GET", selectors = "versions", extensions = "json")
public class ListVersionsServlet extends SlingSafeMethodsServlet {

  @Reference
  protected transient ProfileService profileService;

  /**
   *
   */
  private static final String JSON_PATH = "path";
  /**
   *
   */
  private static final String JSON_ITEMS = "items";
  /**
   *
   */
  private static final String JSON_TOTAL = "total";
  /**
   *
   */
  private static final String JSON_VERSIONS = "versions";
  public static final String PARAMS_ITEMS_PER_PAGE = JSON_ITEMS;
  /**
  *
  */
  public static final String PARAMS_PAGE = "page";

  /**
   *
   */
  private static final long serialVersionUID = 764192946800357626L;
  private static final Logger LOGGER = LoggerFactory.getLogger(ListVersionsServlet.class);

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    Resource resource = request.getResource();
    String path = null;
    try {
      Node node = resource.adaptTo(Node.class);
      if (node == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
      path = node.getPath();
      int nitems = intRequestParameter(request, PARAMS_ITEMS_PER_PAGE, 25);
      int offset = intRequestParameter(request, PARAMS_PAGE, 0) * nitems;

      VersionManager versionManager = node.getSession().getWorkspace()
          .getVersionManager();
      VersionHistory versionHistory = versionManager.getVersionHistory(node.getPath());
      VersionIterator versionIterator = versionHistory.getAllVersions();

      long total = versionIterator.getSize();
      long[] range = getInvertedRange(total, offset, nitems);
      nitems = (int) (range[1] - range[0]);
      Version[] versions = new Version[nitems];
      versionIterator.skip(range[0]);

      int i = 0;
      while (i < nitems && versionIterator.hasNext()) {
        versions[i++] = versionIterator.nextVersion();
      }

      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");

      boolean tidy = false;
      String[] selectors = request.getRequestPathInfo().getSelectors();
      for (String selector : selectors) {
        if ("tidy".equals(selector)) {
          tidy = true;
          break;
        }
      }

      Writer writer = response.getWriter();
      ExtendedJSONWriter write = new ExtendedJSONWriter(writer);
      write.setTidy(tidy);
      write.object();
      write.key(JSON_PATH);
      write.value(node.getPath());
      write.key(JSON_ITEMS);
      write.value(nitems);
      write.key(JSON_TOTAL);
      write.value(total);
      write.key(JSON_VERSIONS);
      write.object();
      versionIterator.skip(offset);
      for (int j = versions.length - 1; j >= 0; j--) {
        write.key(versions[j].getName());
        write.object();
        Node vnode = versions[j].getNode(JcrConstants.JCR_FROZENNODE);
        writeEditorDetails(vnode, write);
        ExtendedJSONWriter.writeNodeContentsToWriter(write, versions[j]);
        write.endObject();
      }
      write.endObject();
      write.endObject();
    } catch (UnsupportedRepositoryOperationException e) {
      Writer writer = response.getWriter();
      try {
        JSONWriter write = new JSONWriter(writer);
        write.object();
        write.key(JSON_PATH);
        write.value(path);
        write.key(JSON_ITEMS);
        write.value(0);
        write.key(JSON_TOTAL);
        write.value(0);
        write.key("warning");
        write.value("The resource requested is not versionable, "
            + "either becuase the repository does not support it "
            + "or becuase the Resource is not versionable");
        write.key(JSON_VERSIONS);
        write.object();
        write.endObject();
        write.endObject();
      } catch (JSONException e1) {
        response.reset();
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e1.getMessage());
        return;
      }

    } catch (RepositoryException e) {
      response.reset();
      LOGGER.info("Failed to get version History ", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    } catch (JSONException e) {
      LOGGER.info("Failed to get version History ", e);
      response.reset();
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    }
  }

  private void writeEditorDetails(Node node, ExtendedJSONWriter write)
      throws RepositoryException, JSONException {
    String user = null;
    if (node.hasProperty(VersionService.SAVED_BY)) {
      user = node.getProperty(VersionService.SAVED_BY).getString();
    }

    if (user != null) {
      UserManager m = AccessControlUtil.getUserManager(node.getSession());
      Authorizable authorizable = m.getAuthorizable(user);
      ValueMap map = profileService.getProfileMap(authorizable, node.getSession());
      write.key(VersionService.SAVED_BY);
      write.valueMap(map);
    }
  }

  /**
   * @param total
   * @param offset
   * @param nitems
   * @return
   */
  protected long[] getInvertedRange(long total, int offset, int nitems) {
    long[] range = new long[2];
    if (total < 0 || nitems < 0) {
      range[0] = 0;
      range[1] = 0;
      return range;
    }
    if (offset < 0) {
      offset = 0;
    }
    range[1] = total - offset;
    range[0] = 0;
    if (range[1] < 0) {
      range[1] = 0;
      range[0] = 0;
    } else {
      range[0] = range[1] - nitems;
      if (range[0] < 0) {
        range[0] = 0;
      }
    }
    return range;
  }

  private int intRequestParameter(SlingHttpServletRequest request, String paramName,
      int defaultVal) throws ServletException {
    RequestParameter param = request.getRequestParameter(paramName);
    if (param != null) {
      try {
        return Integer.parseInt(param.getString());
      } catch (NumberFormatException e) {
        throw new ServletException("Invalid request, the value of param " + paramName
            + " is not a number " + e.getMessage());
      }
    }
    return defaultVal;
  }

}
