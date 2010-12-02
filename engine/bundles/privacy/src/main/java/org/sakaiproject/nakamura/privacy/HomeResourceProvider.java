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

package org.sakaiproject.nakamura.privacy;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.Arrays;
import java.util.Iterator;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

@Component(name = "org.sakaiproject.nakamura.privacy.HomeResourceProvider", immediate = true, metatype = true, description = "%homeprovider.description", label = "%homeprovider.name")
@Service(value = ResourceProvider.class)
@Property(name = ResourceProvider.ROOTS, value = { "/", "/group" })
public class HomeResourceProvider implements ResourceProvider {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(HomeResourceProvider.class);
  public static final String HOME_RESOURCE_PROVIDER = HomeResourceProvider.class
      .getName();

  public Resource getResource(ResourceResolver resourceResolver,
      HttpServletRequest request, String path) {
    LOGGER.info("Got Resource URI [{}]  Path [{}] ", request.getRequestURI(), path);
    return getResource(resourceResolver, path);
  }

  public Resource getResource(ResourceResolver resourceResolver, String path) {
    if (path == null || path.length() < 2) {
      return null;
    }
    char c = path.charAt(1);
    if ( !(c == '~'  || c == 'u' || c =='g')  ) {
      return null;
    }
    if ("/~".equals(path) || "/user".equals(path)  || "/group".equals(path) ) {
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
    String subPath = null;
    if (path.startsWith("/~")) {
      subPath = path.substring("/~".length());
    } else if ( path.startsWith("/user/") ) {
      subPath = path.substring("/user/".length());
    } else if ( path.startsWith("/group/")) {
      subPath = path.substring("/group/".length());
    } 
    if ( subPath != null ) {
      String[] elements = StringUtils.split(subPath, "/", 2);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Got Elements Path [{}] ", Arrays.toString(elements));
      }
      if (elements.length >= 1) {
        Session session = resourceResolver.adaptTo(Session.class);
        UserManager um = AccessControlUtil.getUserManager(session);
        Authorizable a = um.getAuthorizable(elements[0]);
        if (a != null) {
          Principal p = a.getPrincipal();
          if (p instanceof ItemBasedPrincipal) {
            ItemBasedPrincipal ibp = (ItemBasedPrincipal) p;
            String principalPathStart = "/rep:security/rep:authorizables/rep:users";
            String targetStart = "/_user";
            if (a.isGroup()) {
              principalPathStart = "/rep:security/rep:authorizables/rep:groups";
              targetStart = "/_group";
            }
            String userPath = targetStart
                + ibp.getPath().substring(principalPathStart.length());
            if (elements.length == 2) {
              userPath = userPath + "/" + elements[1];
            }
            Resource r = resourceResolver.resolve(userPath);
            LOGGER.debug("Resolving [{}] to [{}] ", userPath, r);
            if (r != null) {
              // are the last elements the same ?
              if (getLastElement(r.getPath()).equals(getLastElement(subPath))) {
                r.getResourceMetadata().put(HomeResourceProvider.HOME_RESOURCE_PROVIDER,
                    this);
                return r;
              } else {
                if (LOGGER.isDebugEnabled()) {
                  LOGGER.debug("Rejected [{}] != [{}] ", getLastElement(r.getPath()),
                      getLastElement(subPath));
                }
              }
            }
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
