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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.api.SlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * This filter enforces privacy on the /_p subtree unless admin is the user.
 * </p>
 */
@Service(value = Filter.class)
@Component(immediate = true, description = "%contentfilter.description", label = "%contentfilter.name")
@Properties(value = { @Property(name = "service.description", value = "Content Filter"),
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "filter.scope", value = "request", propertyPrivate = true),
    @Property(name = "filter.order", intValue = { 10 }, propertyPrivate = true) })
public class ContentPoolFilter implements Filter {

  private static final String ADMIN_USER = "admin";
  private static final Logger LOGGER = LoggerFactory.getLogger(ContentPoolFilter.class);

  @Reference
  public SlingRepository repository;

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    SlingHttpServletRequest srequest = (SlingHttpServletRequest) request;
    SlingHttpServletResponse sresponse = (SlingHttpServletResponse) response;
    Resource resource = srequest.getResource();
    if (resource != null) {
      if (isProtected(srequest, resource)) {
        sresponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Resource is protected");
        return;
      }
    }
    chain.doFilter(request, response);

  }

  /**
   *
   * If the path is /, /_p then access is protected.
   *
   * @param srequest
   * @param resourceNode
   * @return
   * @throws RepositoryException
   */
  private boolean isProtected(SlingHttpServletRequest srequest, Resource resource) {
    if (resource == null) {
      return false;
    }
    String path = resource.getPath();
    if (path == null || path.length() < 2) {
      return false;
    }
    char c = path.charAt(1);
    if (c != '_') {
      return false;
    }

    if (path.startsWith("/_p") ) {
      if (resource.getResourceMetadata().get(ContentPoolProvider.CONTENT_RESOURCE_PROVIDER) instanceof ContentPoolProvider) {
        return false;
      }
      Session session = resource.getResourceResolver().adaptTo(Session.class);
      String currentUser = session.getUserID();
      if (ADMIN_USER.equals(currentUser)) {
        return false;
      }
      Node node = resource.adaptTo(Node.class);
      if ( node == null ) {
        return false; // webdav
      }
      LOGGER.debug("/_p protected ");
      return true;
    }

    return false;
  }

  public void init(FilterConfig filterConfig) throws ServletException {
  }

  public void destroy() {
  }

}
