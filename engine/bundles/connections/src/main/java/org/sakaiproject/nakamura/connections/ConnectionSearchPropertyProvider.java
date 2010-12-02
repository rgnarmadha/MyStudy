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
package org.sakaiproject.nakamura.connections;

import static org.sakaiproject.nakamura.api.connections.ConnectionConstants.SEARCH_PROP_CONNECTIONSTORE;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.search.SearchPropertyProvider;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

@Component(immediate = true, label = "ConnectionSearchPropertyProvider", description= "Provides properties to handle connection searches.")
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"), 
    @Property(name = "sakai.search.provider", value="Connection")
})
@Service(value = SearchPropertyProvider.class)
public class ConnectionSearchPropertyProvider implements SearchPropertyProvider {

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.SearchPropertyProvider#loadUserProperties(org.apache.sling.api.SlingHttpServletRequest,
   *      java.util.Map)
   */
  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
    try {
      String user = request.getRemoteUser();
      Session session = request.getResourceResolver().adaptTo(Session.class);
      UserManager um = AccessControlUtil.getUserManager(session);
      Authorizable auMe = um.getAuthorizable(user);
      String connectionPath = ISO9075.encodePath(ConnectionUtils
          .getConnectionPathBase(auMe));
      if ( connectionPath.startsWith("/"))  {
        connectionPath = connectionPath.substring(1);
      }
      propertiesMap.put(SEARCH_PROP_CONNECTIONSTORE, connectionPath);
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    }
  }
}
