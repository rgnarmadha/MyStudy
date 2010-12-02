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
package org.sakaiproject.nakamura.user;

import static org.sakaiproject.nakamura.api.user.UserConstants.PROP_AUTHORIZABLE_PATH;

import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;

public abstract class AbstractAuthorizableProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAuthorizableProcessor.class);

  /**
   * Initialize the Sakai-3-specific "path" property.
   *
   * TODO It looks like the current code base recreates this logic more often
   * than it uses this property. We should enforce one approach or the other.
   *
   * @param user
   * @param session
   * @throws RepositoryException
   */
  protected void ensurePath(Authorizable authorizable, Session session, String prefix) throws RepositoryException {
    if (!authorizable.hasProperty(PROP_AUTHORIZABLE_PATH)) {
      Principal principal = authorizable.getPrincipal();
      if (principal instanceof ItemBasedPrincipal) {
        String itemPath = ((ItemBasedPrincipal) principal).getPath();
        String path = itemPath.substring(prefix.length());
        ValueFactory valueFactory = session.getValueFactory();
        authorizable.setProperty(PROP_AUTHORIZABLE_PATH, valueFactory.createValue(path));
        LOGGER.debug("Authorizable {} path set to {} ", authorizable.getID(), path);
      } else {
        LOGGER.warn("Authorizable {} has no available path", authorizable.getID());
      }
    }
  }
}
