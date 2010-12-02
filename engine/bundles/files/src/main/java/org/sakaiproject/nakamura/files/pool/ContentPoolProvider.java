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
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

@Component(name = "org.sakaiproject.nakamura.files.pool.ContentPoolProvider", immediate = true, metatype = true, description = "%contentpool.description", label = "%contentpool.name")
@Service(value = ResourceProvider.class)
@Property(name = ResourceProvider.ROOTS, value = { "/", "/p" })
public class ContentPoolProvider implements ResourceProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(ContentPoolProvider.class);
  public static final String CONTENT_RESOURCE_PROVIDER = ContentPoolProvider.class
      .getName();

  // this 36*36 = 1296, so /a/aa/aa/aa will have 36 at the first level, then 46656 at the
  // second and then 60M, then 7e10 items at the last level.

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.resource.ResourceProvider#getResource(org.apache.sling.api.resource.ResourceResolver,
   *      javax.servlet.http.HttpServletRequest, java.lang.String)
   */
  public Resource getResource(ResourceResolver resourceResolver,
      HttpServletRequest request, String path) {
    LOGGER.debug("Got Resource URI [{}]  Path [{}] ", request.getRequestURI(), path);
    return getResource(resourceResolver, path);
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.resource.ResourceProvider#getResource(org.apache.sling.api.resource.ResourceResolver,
   *      java.lang.String)
   */
  public Resource getResource(ResourceResolver resourceResolver, String path) {

    if (path == null || path.length() < 2) {
      return null;
    }
    char c = path.charAt(1);
    if (!(c == 'p')) {
      return null;
    }
    try {
      return resolveMappedResource(resourceResolver, path);
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
    }
    return null;
  }

  private Resource resolveMappedResource(ResourceResolver resourceResolver, String path)
      throws RepositoryException {
    String poolId = null;

    if (path.startsWith("/p/")) {
      poolId = path.substring("/p/".length());
    }
    if (poolId != null && poolId.length() > 0) {
      int i = poolId.indexOf('/');
      if (i > 0) {
        poolId = poolId.substring(0, i);
      }
      i = poolId.indexOf('.');
      String selectors = "";
      if (i > 0) {
        // This ResourceProvider should really only resolver things like
        // /p/717AugiABkcKGOOYxGyzoEsa
        // If extensions and selectors are added, this resolver should NOT resolve it.
        // Sling wil keep slicing the extensions/selectors off untill it hits this url
        // ie: /p/717AugiABkcKGOOYxGyzoEsa.modifyAce.html
        // - /p/717AugiABkcKGOOYxGyzoEsa.modifyAce.html -> null
        // - /p/717AugiABkcKGOOYxGyzoEsa.modifyAce -> null
        // - /p/717AugiABkcKGOOYxGyzoEsa -> return JcrNodeResource
        return null;
      }
      LOGGER.debug("Pool ID is [{}]", poolId);
      String poolPath = null;
      try {
        poolPath = CreateContentPoolServlet.hash(poolId) + selectors;
      } catch (Exception e) {
        throw new RepositoryException("Unable to hash pool ID " + e.getMessage(), e);
      }
      Resource r = resourceResolver.resolve(poolPath);
      if (r instanceof NonExistingResource) {
        LOGGER.debug("Pool ID does not exist, reject and dont allow creation on POST {} ",
            poolPath);
        throw new SlingException("Resources may not be created at /p by the user",
            new AccessDeniedException("Cant create user specified pool resoruce"));
      }
      LOGGER.debug("Resolving [{}] to [{}] ", poolPath, r);
      if (r != null) {
        // are the last elements the same ?
        if (getLastElement(r.getPath()).equals("/" + poolId)) {
          r.getResourceMetadata().put(CONTENT_RESOURCE_PROVIDER, this);
          r.getResourceMetadata().setResolutionPathInfo(selectors);
          return r;
        } else {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Rejected [{}] != [{}] ", getLastElement(r.getPath()), "/"
                + poolId);
          }
        }
      }

    }
    return null;
  }

  private String getLastElement(String path) {
    for (int i = path.length() - 1; i >= 0; i--) {
      if (path.charAt(i) == '/') {
        return path.substring(i);
      }
    }
    return "/" + path;
  }

  public Iterator<Resource> listChildren(Resource parent) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("List Children [{}] ", parent.getPath());
    }
    return null;
  }

}
